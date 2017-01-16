package teefourteen.glideplayer.connectivity;


import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import teefourteen.glideplayer.connectivity.listeners.ErrorListener;
import teefourteen.glideplayer.connectivity.listeners.GroupCreatedListener;
import teefourteen.glideplayer.connectivity.listeners.NewGroupListener;

class NetworkManager {

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private Context context;
    private Connection commandReceiver;
    private  WifiP2pDnsSdServiceRequest discoveryRequest;
    private static final String RECORD_LISTEN_PORT = "listen_port";
    private static final String RECORD_USER_NAME = "user_name";
    private static final String RECORD_GROUP_NAME = "group_name";
    private final static String LOG_TAG = "p2p_debug";
    private final static String INSTANCE_NAME = "glideplayer";
    private final HashMap<String, WifiP2pGroup> p2pGroups = new HashMap<>();

    private class WifiP2pGroup {
        WifiP2pDevice owner;
        HashMap<String, String> record;
        WifiP2pGroup(WifiP2pDevice owner, HashMap<String, String> record) {
            this.owner = owner;
            this.record = record;
        }
    }

    private class AsyncGroupCreator implements Runnable {
        String username;
        String groupName;
        ErrorListener errorListener;
        GroupCreatedListener createdListener;

        AsyncGroupCreator(String username, String groupName, final ErrorListener errorListener,
                          GroupCreatedListener groupCreatedListener) {
            this.username = username;
            this.groupName = groupName;
            this.errorListener = errorListener;
            this.createdListener = groupCreatedListener;
        }

        @Override
        public void run() {
            try {
                commandReceiver = new Connection(0);
            } catch (IOException ex) {
                //TODO: handle this
            }

            Map<String,String> record = new HashMap();
            record.put(RECORD_LISTEN_PORT, String.valueOf(commandReceiver.getPort()));
            record.put(RECORD_USER_NAME, username);
            record.put(RECORD_GROUP_NAME, groupName);

            WifiP2pDnsSdServiceInfo serviceInfo =
                    WifiP2pDnsSdServiceInfo.newInstance(INSTANCE_NAME, "_presence._tcp", record);

            p2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(LOG_TAG,"DnsSd service added");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(LOG_TAG, "Create service failed");
                    commandReceiver.closeConnection();
                    String message = "Failed to create group. Reason: ";
                    errorListener.error(message + standardErrors(reason));
                }
            });

            p2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(LOG_TAG,"create group initiated");
                    createdListener.onGroupCreated();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(LOG_TAG, "Failed to create P2P Group");
                    commandReceiver.closeConnection();
                    String message = "Failed to create group. Reason: ";
                    errorListener.error(message + standardErrors(reason));
                }
            });
        }
    }

    NetworkManager(Activity activity) {
        context = activity.getApplicationContext();
        p2pManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(context, Looper.getMainLooper(), null);
    }

    void createGroup(String username, String groupName, final ErrorListener errorListener,
                     GroupCreatedListener groupCreatedListener) {
        new Thread(new AsyncGroupCreator(username,groupName,errorListener,
                groupCreatedListener), "Group_creator_thread").start();
    }

    void closeGroup(){
        if(commandReceiver != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    commandReceiver.closeConnection();
                }
            }).start();
        }
        p2pManager.removeGroup(channel, null);
        if(discoveryRequest != null) stopDiscovery();
        p2pManager.clearLocalServices(channel, null);
    }

    void discoverGroups(final NewGroupListener groupListener, final ErrorListener errorListener) {
        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                //TODO: improve checking if glideplayer
                if(fullDomain.substring(0, INSTANCE_NAME.length()).equals(INSTANCE_NAME)) {
                    p2pGroups.put(device.deviceAddress, new WifiP2pGroup(device, (HashMap) record));
                }
            }
        };

        DnsSdServiceResponseListener serviceListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice srcDevice) {
                if(instanceName.equals("glideplayer")) {
                    WifiP2pGroup group = p2pGroups.get(srcDevice.deviceAddress);
                    groupListener.newGroupFound(group.owner.deviceAddress,
                            group.record.get(RECORD_GROUP_NAME),
                            group.record.get(RECORD_USER_NAME),
                            srcDevice.deviceName,
                            /*TODO: get members*/0);
                }
            }
        };

        p2pManager.setDnsSdResponseListeners(channel, serviceListener, txtListener);

        discoveryRequest = WifiP2pDnsSdServiceRequest.newInstance();
        p2pManager.addServiceRequest(channel, discoveryRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {Log.d(LOG_TAG,"discovery request added");}

            @Override
            public void onFailure(int reason) {
                String message = "Search failed. Reason: ";
                errorListener.error(message + standardErrors(reason));
            }
        });

        p2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {Log.d(LOG_TAG,"discover services started");}

            @Override
            public void onFailure(int reason) {
                String message = "Search failed. Reason: ";
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
        p2pManager.removeServiceRequest(channel, discoveryRequest, null); //TODO: add action listener
        p2pManager.clearLocalServices(channel, null); //TODO: add action listener
        discoveryRequest = null;
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
}
