package com.teefourteen.glideplayer.connectivity.network;

public interface NetworkListener {
    void onCreating();
    void onCreate();
    void onConnecting();
    void onConnect();
    void onClientConnect(String clientId);
    void onClientDisconnect(String clientId);
    void onDisconnect();

//    void onRequestReceived(String clientId, int requestType, Response response);
}
