package com.teefourteen.glideplayer.connectivity.network;

import com.teefourteen.glideplayer.connectivity.network.server.Request;
import com.teefourteen.glideplayer.connectivity.network.server.Response;

public interface NetworkListener {
    void onCreating();
    void onCreate();
    void onCreateFailed(String reason);
    void onConnecting();
    void onConnect();
    void onConnectFailed(String reason);
    void onClientConnect(String clientId);
    void onClientDisconnect(String clientId);
    void onDisconnect();

    Response onRequestReceived(Request request);
}
