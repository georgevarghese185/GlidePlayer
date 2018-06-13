package com.teefourteen.glideplayer.connectivity.network;

public interface NetworkFinder {
    interface NetworkFinderListener {
        void onFoundNetworks(Network[] networks);
        void finderError(String error);
    }

    void findNetworks(NetworkFinderListener finderListener);
    void stopFindingNetworks();
}
