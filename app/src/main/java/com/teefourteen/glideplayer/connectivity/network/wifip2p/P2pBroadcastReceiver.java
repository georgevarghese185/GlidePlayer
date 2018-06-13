package com.teefourteen.glideplayer.connectivity.network.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.support.annotation.Nullable;

public class P2pBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private ConnectionInfoListener connectionInfoListener;
    private GroupInfoListener groupInfoListener;
    private PeerListListener peerListListener;
    private DeviceListener deviceListener;

    interface DeviceListener {
        void onDeviceChange(WifiP2pDevice device);
    }

    P2pBroadcastReceiver(WifiP2pManager p2pManager, Channel channel,
                         @Nullable ConnectionInfoListener connectionInfoListener,
                         @Nullable GroupInfoListener groupInfoListener,
                         @Nullable PeerListListener peerListListener,
                         @Nullable DeviceListener deviceListener) {
        this.p2pManager = p2pManager;
        this.channel = channel;
        this.connectionInfoListener = connectionInfoListener;
        this.groupInfoListener = groupInfoListener;
        this.deviceListener = deviceListener;
        this.peerListListener = peerListListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if(connectionInfoListener != null) {
                p2pManager.requestConnectionInfo(channel, connectionInfoListener);
            }

            if(groupInfoListener != null) {
                p2pManager.requestGroupInfo(channel, groupInfoListener);
            }
        }

        if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if(peerListListener != null) {
                p2pManager.requestPeers(channel, peerListListener);
            }
        }

        if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice myDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            deviceListener.onDeviceChange(myDevice);
        }
    }

    public static void registerReceiver(Context context, P2pBroadcastReceiver receiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(receiver, intentFilter);
    }

    public static void unregisterReceiver(Context context, P2pBroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception e) {
            //ignored
        }
    }
}
