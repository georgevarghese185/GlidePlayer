package com.teefourteen.glideplayer.connectivity.network.wifip2p;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.Network;
import com.teefourteen.glideplayer.connectivity.network.NetworkListener;
import com.teefourteen.glideplayer.connectivity.network.ResponseListener;
import com.teefourteen.glideplayer.connectivity.network.server.GPServer;
import com.teefourteen.glideplayer.connectivity.network.server.Request;
import com.teefourteen.glideplayer.connectivity.network.server.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.CONNECTED;
import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.CONNECTING;
import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.CREATING;
import static com.teefourteen.glideplayer.connectivity.network.Network.NetworkState.DISCONNECTED;

public class WifiDirectNetwork extends Network implements WifiP2pManager.GroupInfoListener,
        WifiP2pManager.ConnectionInfoListener, P2pBroadcastReceiver.DeviceListener {
    private static final String LOG_TAG = "WifiDirectNetwork";
    private static final String INSTANCE_NAME = "glideplayer";
    private static final String SERVICE_TYPE = "_presence._tcp";
    private static final String RECORD_LISTEN_PORT = "listen_port";
    private static final String RECORD_USER_NAME = "user_name";
    private static final String RECORD_GROUP_NAME = "group_name";
    private static final String RECORD_NUMBER_OF_MEMBERS = "number_of_members";

    private Context context;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager p2pManager;
    private WifiP2pDevice myDevice;
    private WifiP2pDnsSdServiceInfo serviceInfo;
    private Map<String,String> localServiceTxtMap;

    private GPServer server;
    private GPServer.GPServerListener requestListener = request -> {
        Response response = handleRequest(request); //handle network level requests
        if(response == null) {
            if(listeners.size() == 0) {
                return new Response("No listeners to handle request");
            } else {
                return listeners.get(0).onRequestReceived(request);
            }
        } else {
            return response;
        }
    };


    public WifiDirectNetwork(boolean isOwner, String networkName, String ownerName, Client[] clients,
                             Context context) {
        super(isOwner, networkName, ownerName, clients);
        this.context = context;
    }

    //Set up Android WifiP2p related objects
    private void initializeP2p() {
        p2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);

        P2pBroadcastReceiver p2pBroadcastReceiver = new P2pBroadcastReceiver(p2pManager, channel, this, this, this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(p2pBroadcastReceiver, intentFilter);
    }


    //PARENT METHODS IMPLEMENTED

    @Override
    public void create() {
        if(state != DISCONNECTED) {
            groupCreateFailure("Not in disconnected state");
        } else if(!isOwner) {
            groupCreateFailure("Not the owner of this group");
        } else {
            updateState(CREATING);
            createImpl();
        }
    }

    @Override
    public void connect() {
        if(state != DISCONNECTED) {
            groupConnectFailure("Not in disconnected state");
        } else if(isOwner) {
            groupConnectFailure("You cannot connect to your own group");
        } else {
            updateState(CONNECTING);
            connectImpl();
        }
    }

    @Override
    public void disconnect() {
        if(state == DISCONNECTED) {
            Log.w(LOG_TAG, "Trying to disconnect from already disconnected group");
            updateState(DISCONNECTED);
        } else {
            cancelPendingRequests("Disconnecting");

            for(Client client : getClients()) {
                if(!client.equals(me)) sendPeaceOut(client);
            }

            purgeAnyConnection();
            updateState(DISCONNECTED);
        }
    }


    //GROUP CREATION

    //Create a group
    private void createImpl() {
        server = new GPServer(0, requestListener);
        try {
            server.start();

            initializeP2p();

            localServiceTxtMap = new HashMap<>();
            localServiceTxtMap.put(RECORD_LISTEN_PORT, String.valueOf(server.getListeningPort()));
            localServiceTxtMap.put(RECORD_USER_NAME, ownerName);
            localServiceTxtMap.put(RECORD_GROUP_NAME, networkName);
            localServiceTxtMap.put(RECORD_NUMBER_OF_MEMBERS, "1");

            serviceInfo =
                    WifiP2pDnsSdServiceInfo.newInstance(INSTANCE_NAME, SERVICE_TYPE, localServiceTxtMap);


            ActionListener createGroupActionListener = new ActionListener() {
                @Override
                public void onSuccess() {
                    //Group creation success handled in onConnectionInfoAvailable
                }

                @Override
                public void onFailure(int reason) {
                    groupCreateFailure("Failed to create group: " + reason);
                    updateState(DISCONNECTED);
                }
            };

            ActionListener addServiceActionListener = new ActionListener() {
                @Override
                public void onSuccess() {
                    p2pManager.createGroup(channel, createGroupActionListener);
                }

                @Override
                public void onFailure(final int reason) {
                    groupCreateFailure("DnsSd service failed: " + reason);
                    updateState(DISCONNECTED);
                }
            };

            //Begin group creation
            p2pManager.addLocalService(channel, serviceInfo, addServiceActionListener);

        } catch (Exception e) {
            groupCreateFailure(e.toString());
            updateState(DISCONNECTED);
        }
    }

    //GROUP CONNECTION

    //Connect to this group
    private void connectImpl() {
        server = new GPServer(0, requestListener);
        try {
            server.start();
            initializeP2p();

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = owner.clientId;
            config.wps.setup = WpsInfo.PBC;

            ActionListener connectionInitiationListener = new ActionListener() {
                @Override
                public void onSuccess() {
                    //Connection handled in onConnectionInfoAvailable which will call connectSuccessful
                }

                @Override
                public void onFailure(int reason) {
                    groupConnectFailure("Failed to connect to group: " + reason);
                    updateState(DISCONNECTED);
                }
            };

            p2pManager.connect(channel, config, connectionInitiationListener);
        } catch (Exception e) {
            groupConnectFailure(e.toString());
            updateState(DISCONNECTED);
        }
    }

    //After successful WifiP2p connection (called from onConnectionInfoAvailable)
    private void connectSuccessful() {
        //1. Send howdy to owner
        //2. Get list of group clients from owner
        //3. Send howdy to each client

        ResponseListener<Client[]> clientResponseListener = new ResponseListener<Client[]>() {
            @Override
            public void onResponse(final Client[] clients) {
                //send howdy to each client. Keep count. Connection successful when all respond successfully
                ResponseListener<JSONObject> clientHowdyListener = new ResponseListener<JSONObject>() {
                    int successfulHowdys = 0;

                    @Override
                    synchronized public void onResponse(JSONObject response) {
                        successfulHowdys++;

                        if(successfulHowdys == clients.length) {
                            updateState(CONNECTED);
                        }
                    }

                    @Override
                    synchronized public void onError(JSONObject error) {
                        groupConnectFailure(error.toString());
                        purgeAnyConnection();
                        updateState(DISCONNECTED);
                    }
                };


                for(Client client : clients) {
                    addClient(client);
                    sendHowdy(client, clientHowdyListener);
                }
            }

            @Override
            public void onError(JSONObject error) {
                groupConnectFailure(error.toString());
                purgeAnyConnection();
                updateState(DISCONNECTED);
            }
        };

        ResponseListener<JSONObject> ownerHowdyListener = new ResponseListener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                requestClients(clientResponseListener);
            }

            @Override
            public void onError(JSONObject error) {
                groupConnectFailure(error.toString());
                purgeAnyConnection();
                updateState(DISCONNECTED);
            }
        };

        sendHowdy(me, ownerHowdyListener);
    }



    //Safely delete any group (created or connected)
    private void purgeAnyConnection() {
        try {
            p2pManager.removeGroup(channel, null);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }


    //P2P EVENT LISTENERS

    //Called when group creation or group connection is successful. Handle accordingly
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if(info.groupFormed && (state == DISCONNECTED || state == CREATING || state == CONNECTING)) {
            //Add self to clients
            Client me = new Client(myDevice.deviceAddress, "127.0.0.1", server.getListeningPort());
            this.me = me;
            clients.put(myDevice.deviceAddress, me);

            //If owner of this group, we're done. Else, go to next steps
            if(isOwner) {
                this.owner = me;
                updateState(CONNECTED);
            } else {
                connectSuccessful();
            }
        } else if(!info.groupFormed && (state == CONNECTING || state == CREATING)) {
            if(state == CREATING) {
                groupCreateFailure("Group creation failed: UNKNOWN REASON");
            } else {
                groupConnectFailure("Group connection failed: UNKNOWN REASON");
            }
            updateState(DISCONNECTED);
        } else if(!info.groupFormed && state == CONNECTED) {
            updateState(DISCONNECTED);
        }
    }

    //Called when the group changes (we're interested only when group members change because someone left unexcpectedly
    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        Collection<WifiP2pDevice> devices = group.getClientList();
        removeLostClients(devices.toArray(new WifiP2pDevice[devices.size()]));
    }

    private void removeLostClients(WifiP2pDevice[] devices) {
        for(WifiP2pDevice device : devices) {
            if(clients.containsKey(device.deviceAddress)) {
                removeClient(device.deviceAddress);
            }
        }

        //Update Group size in WifiP2p local service
        localServiceTxtMap.put(RECORD_NUMBER_OF_MEMBERS, String.valueOf(clients.size()));
        WifiP2pDnsSdServiceInfo newServiceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(INSTANCE_NAME, SERVICE_TYPE, localServiceTxtMap);
        p2pManager.removeLocalService(channel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                p2pManager.addLocalService(channel, newServiceInfo, null);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(LOG_TAG, "Failed to update group size in P2p local service");
            }
        });
    }

    //Called when first connected to or created a group. We can get our own device address from here
    @Override
    public void onDeviceChange(WifiP2pDevice device) {
        myDevice = device;
    }
}
