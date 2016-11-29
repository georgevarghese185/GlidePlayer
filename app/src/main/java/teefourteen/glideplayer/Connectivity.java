package teefourteen.glideplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.text.Layout;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by george on 15/11/16.
 */

public class Connectivity {
    private boolean initialized = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private WifiP2pBroadcastReceiver receiver;
    private ArrayList<WifiP2pDevice> peerList;
    private ArrayAdapter<WifiP2pDevice> peerListAdapter;
    public final static String LOG_TAG = "wut";
    private Context context;

    public Connectivity(Context context, WifiP2pManager manager) {
        peerList = new ArrayList<>();
        this.context = context;
        this.p2pManager = manager;
        peerListAdapter = new ArrayAdapter<WifiP2pDevice>(context,
                android.R.layout.simple_list_item_1);
    }

    public ArrayAdapter<WifiP2pDevice> getPeerListAdapter() {
        return peerListAdapter;
    }

    public void initialize() {
        if(!initialized) {

            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            channel = p2pManager.initialize(context, Looper.getMainLooper(), null);
            registerBroadcastReceiver();
            initialized = true;
        }
    }

    public void registerBroadcastReceiver() {
         WifiP2pManager.PeerListListener peerListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                peerList.clear();
                peerList.addAll(peers.getDeviceList());
                if(peerList.size() == 0) Log.d(LOG_TAG, "No devices found");
                peerListAdapter.clear();
                peerListAdapter.addAll(peerList);
                peerListAdapter.notifyDataSetChanged();
            }
        };

        receiver = new WifiP2pBroadcastReceiver(peerListener);
        context.registerReceiver(receiver, intentFilter);
    }

    public void unregisterBroadcastReceiver() {
        context.unregisterReceiver(receiver);
    }

    public void startDiscovery() {
        p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Discovery started");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LOG_TAG, "Discovery failed");
            }
        });
    }

    public class WifiP2pBroadcastReceiver extends BroadcastReceiver {
        private WifiP2pManager.PeerListListener peerListener;

        public WifiP2pBroadcastReceiver(WifiP2pManager.PeerListListener listener) {
            peerListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
                if(intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d(LOG_TAG, "WIFI P2P enabled");
                }

            } else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                if(p2pManager!=null)
                    p2pManager.requestPeers(channel, peerListener);

            } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            } else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            }
        }
    }
}
