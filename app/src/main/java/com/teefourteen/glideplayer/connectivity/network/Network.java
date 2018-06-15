package com.teefourteen.glideplayer.connectivity.network;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;
import com.teefourteen.glideplayer.connectivity.network.server.GPServer;
import com.teefourteen.glideplayer.connectivity.network.server.Request;
import com.teefourteen.glideplayer.connectivity.network.server.RequestTask;
import com.teefourteen.glideplayer.connectivity.network.server.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.DISCONNECTED;

public abstract class Network extends StateListener<NetworkListener, Network.NetworkState> {
    private static final String LOG_TAG = "Network";
    private static final String REQUEST_HOWDY = "/network/howdy";
    private static final String REQUEST_CLIENTS = "/network/clients";
    private static final String REQUEST_PEACE_OUT = "/network/peace_out";

    public final boolean isOwner;
    public final String networkName;
    public final String ownerName;
    protected Client owner;
    protected Client me;
    protected ArrayList<Client> clients;

    protected Map<Integer, RequestTask> activeRequests;
    protected int requestIndex = 0;

    public enum NetworkState {
        CREATING,
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    public Network(boolean isOwner, String networkName, String ownerName, Client[] clients) {
        super(null, DISCONNECTED);
        this.isOwner = isOwner;
        this.networkName = networkName;
        this.ownerName = ownerName;
        this.activeRequests = new ConcurrentHashMap<>();

        this.clients = new ArrayList<>(clients.length);
        this.clients.addAll(Arrays.asList(clients));
    }


    abstract public void create();
    abstract public void connect();
    abstract public void disconnect();

    public void addClient(Client client) {
        clients.add(client);
        if(client != me) {
            for(NetworkListener listener : listeners) {
                EasyHandler.executeOnMainThread(() -> listener.onClientConnect(client.clientId));
            }
        }
    }

    public void removeClient(String clientId) {
        Client client = getClient(clientId);
        if(client != null) {
            clients.remove(client);

            for(NetworkListener listener : listeners) {
                EasyHandler.executeOnMainThread(() -> listener.onClientDisconnect(clientId));
            }
        }
    }

    public Client getClient(String clientId) {
        for(Client client : clients) {
            if(client.clientId.equals(clientId));
                return client;
        }

        return null;
    }

    public Client[] getClients() {
        return clients.toArray(new Client[clients.size()]);
    }

    public Client getOwner() {
        return this.owner;
    }

    public Client getMe() {
        return this.me;
    }

    protected void groupCreateFailure(String e) {
        for(NetworkListener listener : listeners) {
            EasyHandler.executeOnMainThread(() -> listener.onCreateFailed(e));
        }
    }

    protected void groupConnectFailure(String reason) {
        for(NetworkListener listener : listeners) {
            EasyHandler.executeOnMainThread(() -> listener.onConnectFailed(reason));
        }
    }


    //NETWORK REQUESTS


    //Handle network level requests
    protected @Nullable
    Response handleRequest(Request request) {
        if(request.requestType.equals(REQUEST_HOWDY)) {
            return respondToHowdy(request);
        } else if (request.requestType.equals(REQUEST_CLIENTS)) {
            return returnClients(request);
        } else if(request.requestType.equals(REQUEST_PEACE_OUT)){
            return handlePeaceOut(request);
        } else {
            return null;
        }
    }


    //HOWDY REQUEST
    protected void sendHowdy(Client client, ResponseListener<JSONObject> responseListener) {
        HashMap<String, String> howdyRequest = new HashMap<>(2);
        howdyRequest.put("deviceAddress", me.clientId);
        howdyRequest.put("port", String.valueOf(me.serverPort));

        requestJSON(client.clientId, REQUEST_HOWDY, howdyRequest, responseListener);
    }

    protected synchronized Response respondToHowdy(Request request) {
        String ipAddress = request.ip;
        String deviceAddress = request.requestParams.get("deviceAddress");
        int port;

        try {
            port = Integer.valueOf(request.requestParams.get("port"));
        } catch (Exception e) {
            return new Response("Invalid port");
        }

        if(ipAddress == null || deviceAddress == null) {
            return new Response("Missing parameters");
        }

        addClient(new Client(deviceAddress, ipAddress, port));

        return new Response();
    }


    //CLIENTS REQUEST
    protected void requestClients(ResponseListener<Client[]> responseListener) {
        ResponseListener<JSONObject> responseListener1 = new ResponseListener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray clientsJSONArray = response.getJSONArray("clients");
                    ArrayList<Client> clients = new ArrayList<>(clientsJSONArray.length());

                    for(int i = 0; i < clientsJSONArray.length(); i++) {
                        JSONObject client = clientsJSONArray.getJSONObject(i);
                        String clientId = client.getString("clientId");
                        String ip = client.getString("ipAddress");
                        int port = client.getInt("port");
                        clients.add(new Client(clientId, ip, port));
                    }

                    responseListener.onResponse(clients.toArray(new Client[clients.size()]));
                } catch (Exception e) {
                    JSONObject err = Response.makeErrorJSON("Exception while creating clients: " + e.toString());
                    responseListener.onError(err);
                }
            }

            @Override
            public void onError(JSONObject error) {
                responseListener.onError(error);
            }
        };

        requestJSON(owner.clientId, REQUEST_CLIENTS, new HashMap<>(), responseListener1);
    }

    protected synchronized Response returnClients(Request request) {
        JSONArray clients = new JSONArray();

        try {
            for (Client client : getClients()) {
                JSONObject clientObject = new JSONObject();
                clientObject.put("ipAddress", client.ipAddress);
                clientObject.put("clientId", client.clientId);
                clientObject.put("port", client.serverPort);

                clients.put(clientObject);
            }

            JSONObject response = new JSONObject();
            response.put("clients", clients);

            return new Response(response);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while returning clients", e);
            return new Response("Exception while returning clients: " + e.toString());
        }
    }


    //PEACE OUT REQUEST
    protected void sendPeaceOut(Client client) {
        requestJSON(client.clientId, REQUEST_PEACE_OUT, new HashMap<>(), null);
    }

    protected Response handlePeaceOut(Request request) {
        for(Client client : getClients()) {
            if(client.ipAddress.equals(request.ip)) {
                removeClient(client.clientId);
            }
        }

        return new Response();
    }


    public int requestJSON(String clientId, String requestType, Map<String, String> parameters,
                           @Nullable ResponseListener<JSONObject> responseListener) {
        int requestId = createRequestTask(clientId, requestType, parameters, responseListener);
        if(activeRequests.containsKey(requestId)) {
            activeRequests.get(requestId).executeJSONRequest();
        }
        return requestId;
    }

    public int requestFile(String clientId, String requestType, Map<String, String> parameters,
                           String fileLocation, @Nullable ResponseListener<File> responseListener) {
        int requestId = createRequestTask(clientId, requestType, parameters, responseListener);
        if(activeRequests.containsKey(requestId)) {
            activeRequests.get(requestId).executeFileRequest(fileLocation);
        }
        return requestId;
    }

    public void cancelRequest(int requestId, String reason) {
        if(activeRequests.containsKey(requestId)) {
            RequestTask request = activeRequests.remove(requestId);
            request.cancelRequest(reason);
        }
    }

    public void cancelPendingRequests(String reason) {
        for(int requestId : activeRequests.keySet()) {
            cancelRequest(requestId, reason);
        }
    }


    protected <T> int createRequestTask(String clientId, String requestType, Map<String, String> parameters,
                       @Nullable ResponseListener<T> responseListener) {

        Client to = getClient(clientId);

        if(to == null) {
            if(responseListener != null) {
                responseListener.onError(Response.makeErrorJSON("Can't find client"));
            }
            return -1;
        }

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

        RequestTask request = new RequestTask(getClient(clientId), requestType, parameters, listener);
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
