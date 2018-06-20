/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.connectivity.network.wifip2p;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.Network;
import com.teefourteen.glideplayer.connectivity.network.NetworkFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.teefourteen.glideplayer.connectivity.network.wifip2p.WifiDirectNetwork.INSTANCE_NAME;
import static com.teefourteen.glideplayer.connectivity.network.wifip2p.WifiDirectNetwork.RECORD_GP_VERSION;
import static com.teefourteen.glideplayer.connectivity.network.wifip2p.WifiDirectNetwork.RECORD_GROUP_NAME;
import static com.teefourteen.glideplayer.connectivity.network.wifip2p.WifiDirectNetwork.RECORD_LISTEN_PORT;
import static com.teefourteen.glideplayer.connectivity.network.wifip2p.WifiDirectNetwork.RECORD_NUMBER_OF_MEMBERS;
import static com.teefourteen.glideplayer.connectivity.network.wifip2p.WifiDirectNetwork.RECORD_USER_NAME;

public class WifiDirectNetworkFinder implements NetworkFinder, WifiP2pManager.PeerListListener {
    private static final String LOG_TAG = "WifiDirectNetworkFinder";

    private Context context;
    private NetworkFinderListener finderListener;
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private WifiP2pServiceRequest discoveryRequest;
    private P2pBroadcastReceiver p2pBroadcastReceiver;

    private HashMap<String, Map<String, String>> discoveredServices = new HashMap<>();
    private ArrayList<WifiDirectNetwork> discoveredNetworks = new ArrayList<>();


    public WifiDirectNetworkFinder(Context context, @NonNull NetworkFinderListener finderListener) {
        this.finderListener = finderListener;
        this.context = context;

        p2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);
    }


    @Override
    public void findNetworks(NetworkFinder.NetworkFinderListener finderListener) {
        this.finderListener = finderListener;
        WifiP2pManager.DnsSdTxtRecordListener txtListener =
                (fullDomainName, txtRecordMap, srcDevice) -> {

                    if(fullDomainName.equals(INSTANCE_NAME)) {
                        discoveredServices.put(srcDevice.deviceAddress, txtRecordMap);
                    }
                };

        WifiP2pManager.DnsSdServiceResponseListener serviceListener =
                (instanceName, registrationType, srcDevice) -> {
                    if(discoveredServices.containsKey(srcDevice.deviceAddress)) {
                        Map<String, String> txtRecord = discoveredServices.get(srcDevice.deviceAddress);

                        String gpVersion = txtRecord.get(RECORD_GP_VERSION);
                        int port = Integer.parseInt(txtRecord.get(RECORD_LISTEN_PORT));
                        String ownerName = txtRecord.get(RECORD_USER_NAME);
                        String groupName = txtRecord.get(RECORD_GROUP_NAME);
                        int memberCount = Integer.parseInt(txtRecord.get(RECORD_NUMBER_OF_MEMBERS));

                        Client owner = new Client(srcDevice.deviceAddress, "0.0.0.0", port); //we don't know ip address yet. Will be updated by Network on successful connection

                        WifiDirectNetwork network = new WifiDirectNetwork(groupName, ownerName, owner,
                                p2pManager, channel, context);

                        discoveredNetworks.add(network);

                        notifyListener();
                    }
                };

        p2pManager.setDnsSdResponseListeners(channel, serviceListener, txtListener);

        discoveryRequest = WifiP2pDnsSdServiceRequest.newInstance();
        p2pManager.addServiceRequest(channel, discoveryRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Discovery request added");
            }

            @Override
            public void onFailure(int reason) {
                EasyHandler.executeOnMainThread(() -> finderListener.finderError("Search failed. Reason: " + reason));
            }
        });

        p2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Discovery started");
            }

            @Override
            public void onFailure(int reason) {
                EasyHandler.executeOnMainThread(() -> finderListener.finderError("Search failed. Reason: " + reason));
            }
        });

        p2pBroadcastReceiver = new P2pBroadcastReceiver(p2pManager, channel, null, null, this, null);
        P2pBroadcastReceiver.registerReceiver(context, p2pBroadcastReceiver);
    }

    @Override
    public void stopFindingNetworks() {
        try {
            p2pManager.removeServiceRequest(channel, discoveryRequest, null);
            p2pManager.clearLocalServices(channel, null);
            P2pBroadcastReceiver.unregisterReceiver(context, p2pBroadcastReceiver);
        } catch (Exception e) {
            //ignored
        }
    }


    @Override
    synchronized public void onPeersAvailable(WifiP2pDeviceList peers) {
        //Check if any previously discovered networks have been lost
        Collection<WifiP2pDevice> deviceList = peers.getDeviceList();
        ArrayList<String> deviceAddresses = new ArrayList<>(deviceList.size());
        ArrayList<WifiDirectNetwork> lostNetworks = new ArrayList<>();

        for(WifiP2pDevice device : deviceList) {
            deviceAddresses.add(device.deviceAddress);
        }

        for(WifiDirectNetwork network : discoveredNetworks) {
            if(!deviceAddresses.contains(network.getOwner().clientId)) {
                lostNetworks.add(network);
            }
        }

        discoveredNetworks.removeAll(lostNetworks);

        notifyListener();
    }


    private void notifyListener() {
        EasyHandler.executeOnMainThread(() ->
                finderListener.onFoundNetworks(discoveredNetworks.toArray(new Network[discoveredNetworks.size()]))
        );
    }
}
