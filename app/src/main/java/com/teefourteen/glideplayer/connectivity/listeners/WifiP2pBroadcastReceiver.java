package com.teefourteen.glideplayer.connectivity.listeners;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import java.net.InetAddress;

public class WifiP2pBroadcastReceiver extends BroadcastReceiver {
    WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    WifiP2pManager.GroupInfoListener groupInfoListener;
    WifiP2pManager p2pManager;
    WifiP2pManager.Channel channel;
    public WifiP2pDevice myDevice;

    public WifiP2pBroadcastReceiver(Context context, WifiP2pManager p2pManager,
                                    WifiP2pManager.Channel channel) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        context.registerReceiver(this, intentFilter);
        this.p2pManager = p2pManager;
        this.channel = channel;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)
                && p2pManager != null) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(connectionInfoListener != null) {
                p2pManager.requestConnectionInfo(channel, connectionInfoListener);
            }

            if(groupInfoListener != null) {
                p2pManager.requestGroupInfo(channel, groupInfoListener);
            }
        }

        if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            myDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        }
    }

    public void registerConnectionInfoListener(
            WifiP2pManager.ConnectionInfoListener connectionInfoListener) {
        this.connectionInfoListener = connectionInfoListener;
    }

    public void clearConnectionInfoListener() {
        this.connectionInfoListener = null;
    }

    public void registerGroupInfoListener(
            WifiP2pManager.GroupInfoListener groupInfoListener) {
        this.groupInfoListener = groupInfoListener;
    }

    public void clearGroupInfoListener() {
        this.groupInfoListener = null;
    }
}
