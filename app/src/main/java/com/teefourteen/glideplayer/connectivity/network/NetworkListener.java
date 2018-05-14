package com.teefourteen.glideplayer.connectivity.network;

import com.teefourteen.glideplayer.connectivity.network.server.Request;

public interface NetworkListener {
    void onCreating();
    void onCreate();
    void onConnecting();
    void onConnect();
    void onClientConnect(String clientId);
    void onClientDisconnect(String clientId);
    void onDisconnect();

    void onRequestReceived(Client client, int requestType, Request request);
}
