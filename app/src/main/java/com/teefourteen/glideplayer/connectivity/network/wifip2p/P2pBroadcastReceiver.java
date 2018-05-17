package com.teefourteen.glideplayer.connectivity.network.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;

public class P2pBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private ConnectionInfoListener connectionInfoListener;
    private GroupInfoListener groupInfoListener;
    private DeviceListener deviceListener;

    interface DeviceListener {
        void onDeviceChange(WifiP2pDevice device);
    }

    P2pBroadcastReceiver(WifiP2pManager p2pManager, Channel channel,
                         ConnectionInfoListener connectionInfoListener,
                         GroupInfoListener groupInfoListener,
                         DeviceListener deviceListener) {
        this.p2pManager = p2pManager;
        this.channel = channel;
        this.connectionInfoListener = connectionInfoListener;
        this.groupInfoListener = groupInfoListener;
        this.deviceListener = deviceListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            p2pManager.requestConnectionInfo(channel, connectionInfoListener);
            p2pManager.requestGroupInfo(channel, groupInfoListener);
        }

        if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice myDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            deviceListener.onDeviceChange(myDevice);
        }
    }
}
