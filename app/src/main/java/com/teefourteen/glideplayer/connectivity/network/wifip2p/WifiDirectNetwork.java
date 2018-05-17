package com.teefourteen.glideplayer.connectivity.network.wifip2p;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Looper;

import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.Network;
import com.teefourteen.glideplayer.connectivity.network.NetworkListener;
import com.teefourteen.glideplayer.connectivity.network.server.GPServer;
import com.teefourteen.glideplayer.connectivity.network.server.Response;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.CONNECTED;
import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.CONNECTING;
import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.DISCONNECTED;

public class WifiDirectNetwork extends Network implements WifiP2pManager.GroupInfoListener,
        WifiP2pManager.ConnectionInfoListener, P2pBroadcastReceiver.DeviceListener {
    private static final String RECORD_LISTEN_PORT = "listen_port";
    private static final String RECORD_USER_NAME = "user_name";
    private static final String RECORD_GROUP_NAME = "group_name";

    private Context context;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager p2pManager;
    private WifiP2pDevice myDevice;

    private GPServer server;
    private GPServer.GPServerListener requestListener = request -> {
        if(listeners.size() == 0) {
            return new Response("No listeners to handle request");
        } else {
            return listeners.get(0).onRequestReceived(request);
        }
    };


    public WifiDirectNetwork(boolean isOwner, String networkName, Client[] clients,
                             Context context) {
        super(isOwner, networkName, clients);
        this.context = context;
    }

    @Override
    public void create(String ownerName) {
        if(state != DISCONNECTED) {
            for(NetworkListener listener : listeners) {
                listener.onConnectFailed("Not in disconnected state");
            }
        } else if(!isOwner) {
            for(NetworkListener listener : listeners) {
                listener.onConnectFailed("Not the owner of this group");
            }
        } else {
            updateState(CONNECTING);
            createImpl(ownerName);
        }
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    private void createImpl(String ownerName) {
        server = new GPServer(0, requestListener);
        try {
            server.start();

            initializeP2p();

            Map<String,String> txtMap = new HashMap<>();
            txtMap.put(RECORD_LISTEN_PORT, String.valueOf(server.getListeningPort()));
            txtMap.put(RECORD_USER_NAME, ownerName);
            txtMap.put(RECORD_GROUP_NAME, networkName);

            WifiP2pDnsSdServiceInfo serviceInfo =
                    WifiP2pDnsSdServiceInfo.newInstance("glideplayer", "_presence._tcp", txtMap);


            ActionListener createGroupActionListener = new ActionListener() {
                @Override
                public void onSuccess() {
                    //Group creation success handled in onConnectionInfoAvailable
                }

                @Override
                public void onFailure(int reason) {
                    updateState(DISCONNECTED);
                    groupCreateFailure("Failed to create group: " + reason);
                }
            };

            ActionListener addServiceActionListener = new ActionListener() {
                @Override
                public void onSuccess() {
                    p2pManager.createGroup(channel, createGroupActionListener);
                }

                @Override
                public void onFailure(final int reason) {
                    groupCreateFailure("DnsSd service failed: " + reason);
                    updateState(DISCONNECTED);
                }
            };

            //Begin group creation
            p2pManager.addLocalService(channel, serviceInfo, addServiceActionListener);

        } catch (Exception e) {
            groupCreateFailure(e.toString());
            updateState(DISCONNECTED);
        }
    }

    private void initializeP2p() {
        p2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);

        P2pBroadcastReceiver p2pBroadcastReceiver = new P2pBroadcastReceiver(p2pManager, channel, this, this, this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(p2pBroadcastReceiver, intentFilter);
    }

    private void groupCreateFailure(String e) {
        for(NetworkListener listener : listeners) {
            listener.onCreateFailed(e);
        }
    }



    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if(info.groupFormed && state == DISCONNECTED) {
            updateState(CONNECTED);
        } else if(!info.groupFormed && state == CONNECTING) {
            groupCreateFailure("Group creation failed: UNKNOWN REASON");
            updateState(DISCONNECTED);
        } else if(!info.groupFormed && state == CONNECTED) {
            updateState(DISCONNECTED);
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        Collection<WifiP2pDevice> devices = group.getClientList();

    }

    private Client toClient(WifiP2pDevice device) {
        return new Client(device.deviceAddress, device.)
    }

    @Override
    public void onDeviceChange(WifiP2pDevice device) {
        myDevice = device;
    }
}
