package com.teefourteen.glideplayer.connectivity.network;

import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;
import com.teefourteen.glideplayer.connectivity.network.server.GPServer;
import com.teefourteen.glideplayer.connectivity.network.server.RequestTask;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.DISCONNECTED;

public abstract class Network extends StateListener<NetworkListener, Network.NetworkState> {
    protected final boolean isOwner;
    protected final String networkName;
    protected Map<String, Client> clients;

    protected Map<Integer, RequestTask> activeRequests;
    protected int requestIndex = 0;

    public enum NetworkState {
        CREATING,
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    public Network(boolean isOwner, String networkName, Client[] clients) {
        super(null, DISCONNECTED);
        this.isOwner = isOwner;
        this.networkName = networkName;
        this.activeRequests = new ConcurrentHashMap<>();

        this.clients = new HashMap<>(clients.length);
        for(Client client : clients) {
            this.clients.put(client.clientId, client);
        }
    }


    abstract public void create(String ownerName);
    abstract public void connect();
    abstract public void disconnect();


    public int requestJSON(String clientId, String requestType, Map<String, String> parameters,
                           @Nullable ResponseListener<JSONObject> responseListener) {
        int requestId = createRequestTask(clientId, requestType, parameters, responseListener);
        activeRequests.get(requestId).executeJSONRequest();
        return requestId;
    }

    public int requestFile(String clientId, String requestType, Map<String, String> parameters,
                           String fileLocation, @Nullable ResponseListener<File> responseListener) {
        int requestId = createRequestTask(clientId, requestType, parameters, responseListener);
        activeRequests.get(requestId).executeFileRequest(fileLocation);
        return requestId;
    }

    public void cancelRequest(int requestId, String reason) {
        RequestTask request = activeRequests.remove(requestId);
        request.cancelRequest(reason);
    }


    protected <T> int createRequestTask(String clientId, String requestType, Map<String, String> parameters,
                       @Nullable ResponseListener<T> responseListener) {

        int index = requestIndex++;

        ResponseListener<T> listener = new ResponseListener<T>() {
            @Override
            public void onResponse(T response) {
                activeRequests.remove(index);
                if (responseListener != null) {
                    responseListener.onResponse(response);
                }
            }

            @Override
            public void onError(JSONObject error) {
                activeRequests.remove(index);
                if (responseListener != null) {
                    responseListener.onError(error);
                }
            }
        };

        RequestTask request = new RequestTask(clients.get(clientId), requestType, parameters, listener);
        activeRequests.put(requestIndex++, request);
        return index;
    }


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
