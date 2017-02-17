package teefourteen.glideplayer.connectivity;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.connectivity.listeners.ErrorListener;
import teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import teefourteen.glideplayer.connectivity.listeners.GroupCreationListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.connectivity.listeners.RequestListener;
import teefourteen.glideplayer.connectivity.listeners.NewGroupListener;
import teefourteen.glideplayer.connectivity.listeners.ResponseListener;
import teefourteen.glideplayer.connectivity.listeners.WifiP2pBroadcastReceiver;


//TODO: persistent notification
//TODO: allow bigger files (right now, can't send Long for file size. fix that)
//TODO: set some sort of time out on sent and received requests to back off.
public class NetworkService extends Service {
    private static final String RECORD_LISTEN_PORT = "listen_port";
    private static final String RECORD_USER_NAME = "user_name";
    private static final String RECORD_GROUP_NAME = "group_name";
    private final static String LOG_TAG = "p2p_debug";
    private final static String INSTANCE_NAME = "glideplayer";
    private final static String THREAD_NETWORK_MANAGER = "network_manager_thread";
    private final static String THREAD_CONNECTION_LISTENER = "connection_listener_thread";
    private final static String THREAD_REQUEST = "request_thread";
    private final static String THREAD_REQUEST_HANDLER = "request_handler_thread";
    private final static String THREAD_CLIENT_HANDLER = "new_client_handler_thread"; //new clients or leaving clients are handled in order on this one thread to avoid asynchronous issues
    private static final int ACTION_ESTABLISH =2226;
    private static final int ACTION_NEW_CLIENT = 2622;
    public static final int ACTION_RAW_SOCKET = 2125;
    private static final String DATA_TYPE_INTEGER = "integer";
    private static final String DATA_TYPE_LONG = "long";
    private static final String DATA_TYPE_STRING = "string";
    private static final String DATA_TYPE_OBJECT = "object";
    private static final String DATA_TYPE_FILE = "file";
    private static final String DATA_TYPE_NULL = "null";
    public static String FILE_SAVE_LOCATION;

    private final IBinder binder = new LocalBinder();
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private Connection.ListenServer server;
    private  WifiP2pDnsSdServiceRequest discoveryRequest;
    private WifiP2pBroadcastReceiver p2pBroadcastReceiver;
    private HashMap<String, AvailableGroup> discoveredP2pGroups;
    private HashMap<String, Client> clientMap;
    private GroupMemberListener groupMemberListener;
    private RequestListener requestListener;
    private EasyHandler handler;
    private int session = 0;



    private class AvailableGroup {
        WifiP2pDevice owner;
        HashMap<String, String> record;
        AvailableGroup(WifiP2pDevice owner, HashMap<String, String> record) {
            this.owner = owner;
            this.record = record;
        }
    }


    private class Client {
        public final InetAddress inetAddress;
        public final int port;

        Client(InetAddress inetAddress, int port){
            this.inetAddress = inetAddress;
            this.port = port;
        }

        Client(JSONObject jsonObject)throws JSONException, UnknownHostException {
            this.inetAddress = InetAddress.getByName(jsonObject.getString("inetAddress"));
            this.port = jsonObject.getInt("port");
        }

        public JSONObject toJson()throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("inetAddress", inetAddress.getHostAddress());
            jsonObject.put("port", port);
            return jsonObject;
        }
    }

    private void sendClientMap(Connection connection) throws IOException, JSONException {
        connection.sendInt(clientMap.entrySet().size());

        for(String deviceId : clientMap.keySet()) {
            connection.sendString(deviceId);
            JSONObject jsonObject = clientMap.get(deviceId).toJson();
            connection.sendString(jsonObject.toString());
        }
    }

    private void getClientMap(Connection connection)throws IOException, JSONException {
        int size = connection.getNextInt();

        for(; size>0; size--) {
            String deviceId = connection.getNextString();
            JSONObject jsonObject = new JSONObject(connection.getNextString());
            clientMap.put(deviceId, new Client(jsonObject));
        }
    }



    private class RequestHandler implements Runnable{
        private Connection con;
        private String receiverDeviceAddress;

        RequestHandler(Connection requestCon, String receiverDeviceAddress) {
            con = requestCon;
            this.receiverDeviceAddress = receiverDeviceAddress;
        }

        @Override
        public void run() {
            try {
                int action = con.getNextInt();

                if (action == ACTION_ESTABLISH) {
                    handler.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            incomingEstablishRequested(con, receiverDeviceAddress);
                        }
                    }, THREAD_CLIENT_HANDLER, true);
                } else if (action == ACTION_NEW_CLIENT) {
                    handler.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            newClient(con);
                        }
                    }, THREAD_CLIENT_HANDLER, true);
                } else if(action == ACTION_RAW_SOCKET) {
                    requestListener.onNewRequest(receiverDeviceAddress, ACTION_RAW_SOCKET,
                            con.getSocket());
                } else {
                    handleRequest(action);
                }
            } catch (IOException e) {
                //TODO: handle
            }
        }

        void handleRequest(int action)throws IOException {
            String requestType = con.getNextString();
            Object responseData;

            switch (requestType) {
                case DATA_TYPE_NULL:
                    responseData =
                            requestListener.onNewRequest(receiverDeviceAddress, action, null);
                    break;
                case DATA_TYPE_STRING:
                    responseData = requestListener.onNewRequest(receiverDeviceAddress, action,
                            con.getNextString());
                    break;
                case DATA_TYPE_INTEGER:
                    responseData = requestListener.onNewRequest(receiverDeviceAddress, action,
                            con.getNextInt());
                    break;
                case DATA_TYPE_LONG:
                    responseData = requestListener.onNewRequest(receiverDeviceAddress, action,
                            con.getNextLong());
                    break;
                case DATA_TYPE_FILE:
                    int size = con.getNextInt();
                    responseData = requestListener.onNewRequest(receiverDeviceAddress, action,
                            con.getNextFile(getCacheDir()+"/files",size));
                    break;
                default:
                    responseData = requestListener.onNewRequest(receiverDeviceAddress, action,
                            con.getNextObject());
                    break;
            }

            if(responseData == null) {
                con.sendString(DATA_TYPE_NULL);
            } else if(responseData instanceof String) {
                con.sendString(DATA_TYPE_STRING);
                con.sendString((String) responseData);
            } else if(responseData instanceof Integer) {
                con.sendString(DATA_TYPE_INTEGER);
                con.sendInt((Integer) responseData);
            } else if(responseData instanceof Long) {
                con.sendString(DATA_TYPE_INTEGER);
                con.sendLong((Long) responseData);
            } else if(responseData instanceof File) {
                con.sendString(DATA_TYPE_FILE);
                int size = (int) (((File) responseData).length());
                con.sendInt(size);
                con.sendFile((File) responseData);
            } else {
                con.sendString(DATA_TYPE_OBJECT);
                con.sendObject(responseData);
            }

            con.close();
        }
    }



    public class LocalBinder extends Binder {
        private NetworkService service = NetworkService.this;

        void createGroup(final String username, final String groupName,
                         final GroupCreationListener groupCreationListener,
                         final GroupMemberListener groupMemberListener,
                         final RequestListener requestListener) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.createGroup(username, groupName, groupCreationListener,
                            groupMemberListener, requestListener);
                }
            };
            handler.executeAsync(r, THREAD_NETWORK_MANAGER);
        }

        public void deleteGroup(){
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.deleteGroup();
                }
            };
            handler.executeAsync(r, THREAD_NETWORK_MANAGER);
        }

        void discoverGroups(final NewGroupListener groupListener,
                            final ErrorListener errorListener) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.discoverGroups(groupListener, errorListener);
                }
            };
            handler.executeAsync(r, THREAD_NETWORK_MANAGER);
        }

        void stopDiscovery() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.stopDiscovery();
                }
            };
            handler.executeAsync(r, THREAD_NETWORK_MANAGER);
        }

        public void connectToGroup(final String groupId,
                                   final GroupConnectionListener groupConnectionListener,
                                   final GroupMemberListener groupMemberListener,
                                   final RequestListener requestListener) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.connectToGroup(groupId, groupConnectionListener, groupMemberListener,
                            requestListener);
                }
            };
            handler.executeAsync(r, THREAD_NETWORK_MANAGER);
        }

        public void sendRequest(final String deviceId, final int action,
                                final Object requestData,
                                final ResponseListener responseListener) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.sendRequest(deviceId, action, requestData, responseListener);
                }
            };
            handler.executeAsync(r, THREAD_REQUEST+session++);
        }

        public void getRawSocket(final String deviceId, final ResponseListener responseListener) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.getRawSocket(deviceId, responseListener);
                }
            };
            handler.executeAsync(r, THREAD_NETWORK_MANAGER);
        }
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        try {
            server = new Connection.ListenServer(0);
            p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            channel = p2pManager.initialize(this, Looper.getMainLooper(), null);
            p2pBroadcastReceiver = new WifiP2pBroadcastReceiver(this, p2pManager, channel);
            handler = new EasyHandler();
            handler.createHandler(THREAD_NETWORK_MANAGER);
            clientMap = new HashMap<>();

            //start listening for requests
            handler.executeAsync(new Runnable() {
                @Override
                public void run() {
                    listenForRequests();
                }
            }, THREAD_CONNECTION_LISTENER);

            FILE_SAVE_LOCATION = getExternalFilesDir(null) + "/files";
            File fileDir = new File(FILE_SAVE_LOCATION);
            if( !(fileDir.exists()) ) {
                fileDir.mkdir();
            }

            return binder;
        } catch (IOException e) {
            return null;
        }
    }

    void createGroup(final String username, final String groupName,
                     final GroupCreationListener groupCreationListener,
                     final GroupMemberListener groupMemberListener,
                     final RequestListener requestListener) {
        Map<String,String> txtMap = new HashMap<>();
        txtMap.put(RECORD_LISTEN_PORT, String.valueOf(server.getPort()));
        txtMap.put(RECORD_USER_NAME, username);
        txtMap.put(RECORD_GROUP_NAME, groupName);

        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(INSTANCE_NAME, "_presence._tcp", txtMap);

        p2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG,"DnsSd service added");
            }

            @Override
            public void onFailure(final int reason) {
                Log.d(LOG_TAG, "Create service failed");
                String message = "Failed to create group. Reason: ";
                groupCreationListener.onGroupCreationFailed(message + standardErrors(reason));
            }
        });

        this.groupMemberListener = groupMemberListener; //will be used later for when new clients join and establish
        this.requestListener = requestListener; //for any incoming requests

        //to check if a member left the group
        p2pBroadcastReceiver.registerGroupInfoListener(new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                if(group != null) {
                    updateLostClients(group.getClientList(), groupMemberListener);
                }
            }
        });

        //TODO: register connection info listener instead for group creation success


        p2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG,"create group initiated");
                groupCreationListener.onGroupCreated();
            }

            @Override
            public void onFailure(final int reason) {
                Log.d(LOG_TAG, "Failed to create P2P Group");
                final String message = "Failed to create group. Reason: ";
                groupCreationListener.onGroupCreationFailed(message + standardErrors(reason));
            }
        });
    }

    public void deleteGroup(){
        p2pManager.removeGroup(channel, null);
        p2pManager.clearLocalServices(channel, null);
        server.close();
        p2pBroadcastReceiver.clearGroupInfoListener();
    }

    //put in service and separate thread
    void discoverGroups(final NewGroupListener groupListener, final ErrorListener errorListener) {
        discoveredP2pGroups = new HashMap<>();
        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                //TODO: improve checking if glideplayer
                //TODO: check GlidePlayer app version in case of mismatched formats in exchanging info
                if(fullDomain.substring(0, INSTANCE_NAME.length()).equals(INSTANCE_NAME)
                        && (record instanceof HashMap)) {
                    discoveredP2pGroups.put(device.deviceAddress,
                            new AvailableGroup(device, (HashMap) record));
                }
            }
        };

        DnsSdServiceResponseListener serviceListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                final WifiP2pDevice srcDevice) {
                if(instanceName.equals("glideplayer")) {
                    final AvailableGroup group = discoveredP2pGroups.get(srcDevice.deviceAddress);
                    groupListener.newGroupFound(group.owner.deviceAddress,
                            group.record.get(RECORD_GROUP_NAME),
                            group.record.get(RECORD_USER_NAME),
                            srcDevice.deviceName,
                            /*TODO: get members*/1);
                }
            }
        };

        p2pManager.setDnsSdResponseListeners(channel, serviceListener, txtListener);

        discoveryRequest = WifiP2pDnsSdServiceRequest.newInstance();
        p2pManager.addServiceRequest(channel, discoveryRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { Log.d(LOG_TAG,"discovery request added"); }

            @Override
            public void onFailure(final int reason) {
                final String message = "Search failed. Reason: ";
                errorListener.error(message + standardErrors(reason));
            }
        });

        p2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {Log.d(LOG_TAG,"discover services started");}

            @Override
            public void onFailure(final int reason) {
                final String message = "Search failed. Reason: ";
                errorListener.error(message + standardErrors(reason));
                p2pManager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure(int reason) {
                        Log.d(LOG_TAG, "Failed to clear service requests. Reason:" + reason);
                    }
                });
            }
        });
    }

    void stopDiscovery() {
        if(discoveryRequest!=null) {
            p2pManager.removeServiceRequest(channel, discoveryRequest, null); //TODO: add action listener
            p2pManager.clearLocalServices(channel, null); //TODO: add action listener
            discoveryRequest = null;
            discoveredP2pGroups.clear();
        }
    }

    //put in service, separate thread
    public void connectToGroup(final String groupId,
                               final GroupConnectionListener groupConnectionListener,
                               final GroupMemberListener groupMemberListener,
                               RequestListener requestListener) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = groupId;
        config.wps.setup = WpsInfo.PBC;

        this.groupMemberListener = groupMemberListener;
        this.requestListener = requestListener;

        p2pBroadcastReceiver.registerConnectionInfoListener(
                new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                        if(!info.groupFormed) {
                            groupConnectionListener.onConnectionFailed("Connection failed");
                            return;
                        }
                        p2pBroadcastReceiver.clearConnectionInfoListener(); //TODO: register a new one for disconnection

                        final int ownerPort = Integer.parseInt(
                                discoveredP2pGroups.get(groupId).record.get(RECORD_LISTEN_PORT));
                        final InetAddress ownerAddress = info.groupOwnerAddress;
                        Runnable establish = new Runnable() {
                            @Override
                            public void run() {
                                clientMap.put(groupId, new Client(ownerAddress, ownerPort));
                                try {
                                    EasyHandler.executeOnMainThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            groupConnectionListener.onConnectionSuccess(groupId);
                                        }
                                    });

                                    establish(ownerAddress, ownerPort);
                                    stopDiscovery();
                                } catch (IOException  | JSONException e) {
                                    groupConnectionListener.onConnectionFailed("IOException at establish");
                                }
                            }
                        };
                        handler.executeAsync(establish, THREAD_NETWORK_MANAGER);

                        p2pBroadcastReceiver.registerGroupInfoListener(new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        if(group != null) {
                                            updateLostClients(group.getClientList(), groupMemberListener);
                                        }
                                    }
                                };
                                handler.executeAsync(r, THREAD_CLIENT_HANDLER);
                            }
                        });

                        p2pBroadcastReceiver.registerConnectionInfoListener(new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                if(!info.groupFormed) {
                                    groupConnectionListener.onOwnerDisconnected();
                                }
                            }
                        });
                    }
                });


        p2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Initiating connection to: " + groupId);
            }

            @Override
            public void onFailure(final int reason) {
                final String message = "Failed to connect to group. Reason: ";
                groupConnectionListener.onConnectionFailed(message + standardErrors(reason));
            }
        });
    }

    private void listenForRequests() {
        try {
            while (true) {
                Connection requestCon = server.listen();
                if (requestCon == null) {
                    break; //null would mean close() was called
                }

                try {
                    String receiverAddress = requestCon.getNextString();

                    RequestHandler requestHandler = new RequestHandler(requestCon, receiverAddress);
                    new Thread(requestHandler, THREAD_REQUEST_HANDLER + session++).start();
                } catch (IOException e) {/*TODO: handle if required*/}
            }

        } catch (IOException e) {
            //TODO: handle this
        }
    }

    private void incomingEstablishRequested(Connection newClientConnection,
                                            final String newClientDeviceAddress) {
        try {
            final InetAddress newClientInetAddress = newClientConnection.getRemoteInetAddress();
            final int newClientPort = newClientConnection.getNextInt();
            sendClientMap(newClientConnection);
            newClientConnection.close();
            clientMap.put(newClientDeviceAddress, new Client(newClientInetAddress, newClientPort));

            for (final String deviceAddress : clientMap.keySet()) {
                if(deviceAddress.equals(newClientDeviceAddress)) {
                    continue;
                }
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Client client = clientMap.get(deviceAddress);
                            Connection connection = new Connection(client.inetAddress, client.port);
                            connection.sendString(p2pBroadcastReceiver.myDevice.deviceAddress);
                            connection.sendInt(ACTION_NEW_CLIENT);
                            connection.sendString(newClientDeviceAddress);
                            connection.sendString(
                                    clientMap.get(newClientDeviceAddress).toJson().toString());
                            connection.close();
                        } catch (IOException | JSONException e) {
                            Log.d("notifying new client", e.toString());
                        }
                    }
                };
                new Thread(r).start();
            }

            groupMemberListener.onNewMemberJoined(newClientDeviceAddress, null);
        } catch (IOException | JSONException e) {
            Log.d("Incoming establish", e.toString());
        }
    }

    private void establish(InetAddress inetAddress, int port)throws IOException, JSONException {
        Connection ownerCon = new Connection(inetAddress, port);
        ownerCon.sendString(p2pBroadcastReceiver.myDevice.deviceAddress);

        ownerCon.sendInt(ACTION_ESTABLISH);
        ownerCon.sendInt(server.getPort());
        getClientMap(ownerCon);

        ownerCon.close();
    }

    private void newClient(Connection connection){
        try {
            String newClientDeviceAddress = connection.getNextString();
            JSONObject jsonObject = new JSONObject(connection.getNextString());
            connection.close();
            clientMap.put(newClientDeviceAddress, new Client(jsonObject));
            groupMemberListener.onNewMemberJoined(newClientDeviceAddress, null);
        } catch (IOException | JSONException e) {
            Log.d("new client", e.toString());
        }
    }

    private void updateLostClients(Collection<WifiP2pDevice> newClientList,
                                   final GroupMemberListener groupMemberListener){
        if(groupMemberListener == null) {
            return;
        }
        //check only if any established clients left

        ArrayList<String> devicesToRemove = new ArrayList<>();
        outerLoop:
        for(String deviceAddress : clientMap.keySet()){
            for(WifiP2pDevice device : newClientList) {
                if(deviceAddress.equals(device.deviceAddress)) continue outerLoop;
            }
            groupMemberListener.onMemberLeft(deviceAddress);
            devicesToRemove.add(deviceAddress);
        }

        for(String deviceAddress : devicesToRemove) {
            clientMap.remove(deviceAddress);
        }
    }

    public void getRawSocket(String deviceId, ResponseListener responseListener) {
        try {
            Connection connection = new Connection(clientMap.get(deviceId).inetAddress,
                    clientMap.get(deviceId).port);

            connection.sendString(p2pBroadcastReceiver.myDevice.deviceAddress);
            connection.sendInt(ACTION_RAW_SOCKET);

            responseListener.onResponseReceived(connection.getSocket());
        } catch (IOException e) {
            responseListener.onRequestFailed();
        }
    }

    public void sendRequest(String deviceId, int action, Object requestData, ResponseListener responseListener) {
        Connection connection = null;

        try {
            connection = new Connection(clientMap.get(deviceId).inetAddress,
                    clientMap.get(deviceId).port);

            connection.sendString(p2pBroadcastReceiver.myDevice.deviceAddress);

            connection.sendInt(action);

            if(requestData == null) {
                connection.sendString(DATA_TYPE_NULL);
            } else if(requestData instanceof String) {
                connection.sendString(DATA_TYPE_STRING);
                connection.sendString((String) requestData);
            } else if(requestData instanceof Integer) {
                connection.sendString(DATA_TYPE_INTEGER);
                connection.sendInt((Integer) requestData);
            }else if(requestData instanceof Long) {
                connection.sendString(DATA_TYPE_LONG);
                connection.sendLong((Long) requestData);
            } else if (requestData instanceof File){
                connection.sendString(DATA_TYPE_FILE);
                long size = ((File) requestData).length();
                if(size > Integer.MAX_VALUE) throw new Exception("File too big");

                connection.sendInt((int) size);
                connection.sendFile((File) requestData);
            }else {
                connection.sendString(DATA_TYPE_OBJECT);
                connection.sendObject(requestData);
            }

            String responseType = connection.getNextString();

            switch (responseType) {
                case DATA_TYPE_NULL:
                    responseListener.onResponseReceived(null);
                    break;
                case DATA_TYPE_STRING:
                    responseListener.onResponseReceived(connection.getNextString());
                    break;
                case DATA_TYPE_INTEGER:
                    responseListener.onResponseReceived(connection.getNextInt());
                    break;
                case DATA_TYPE_LONG:
                    responseListener.onResponseReceived(connection.getNextLong());
                    break;
                case DATA_TYPE_FILE:
                    int size = connection.getNextInt();
                    responseListener.onResponseReceived(
                            connection.getNextFile(FILE_SAVE_LOCATION, size));
                    break;
                default:
                    responseListener.onResponseReceived(connection.getNextObject());
                    break;
            }

        } catch (IOException e) {
            responseListener.onRequestFailed();
        } catch (Exception e) {
            if(e.toString().equals("File too big"))
                responseListener.onRequestFailed();
        }finally {
            if (connection != null) connection.close();
        }
    }

    private String standardErrors(int reason) {
        if(reason == WifiP2pManager.ERROR) {
            return "Internal error occured";
        } else if (reason == WifiP2pManager.BUSY) {
            return "System busy. Try again";
        } else if (reason == WifiP2pManager.P2P_UNSUPPORTED){
            return "Device does not support Wifi Direct";
        }
        else return "Unknown error";
    }

    @Override
    public void onDestroy() {
        server.close();
        handler.closeAllHandlers();
        unregisterReceiver(p2pBroadcastReceiver);
    }
}
