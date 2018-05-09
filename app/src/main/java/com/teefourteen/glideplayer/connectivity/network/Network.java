package com.teefourteen.glideplayer.connectivity.network;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;

import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.DISCONNECTED;

public abstract class Network extends StateListener<NetworkListener, Network.NetworkState> {
    private boolean isOwner;
    private String mGroupName;

    public enum NetworkState {
        CREATING,
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    public Network(String groupName) {
        super(null, DISCONNECTED);

        this.mGroupName = groupName;
    }


    abstract public void create(String networkName);
    abstract public void connect();
    abstract public void disconnect();

//    abstract public int sendRequest(String clientId, int requestType, Object requestData,
//                                    ResponseListener listener);
//    abstract public int rawConnection(String clientId, int requestType, Object requestData,
//                               ResponseListener listener);
//    abstract public void cancelRequest(int requestId);

    @Override
    protected void notifyListener(NetworkListener networkListener, NetworkState networkState) {
        EasyHandler.executeOnMainThread(() -> {
            switch (networkState) {

                case CREATING:
                    networkListener.onCreating();
                    break;
                case CONNECTING:
                    networkListener.onConnecting();
                    break;
                case CONNECTED:
                    networkListener.onConnect();
                    break;
                case DISCONNECTED:
                    networkListener.onDisconnect();
                    break;
            }
        });
    }
}
