package teefourteen.glideplayer.fragments.connectivity;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import teefourteen.glideplayer.Connectivity;
import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.R;

public class ConnectivityFragment extends Fragment {


    public ConnectivityFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connectivity, container, false);
        if(Global.connectivity == null) {
            Global.connectivity = new Connectivity(getContext(),
                    (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE));
        }
        Global.connectivity.initialize();
        ListView peerListView = (ListView) view.findViewById(R.id.peer_list);
        peerListView.setAdapter(Global.connectivity.getPeerListAdapter());

        (view.findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discover(v);
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    public void discover(View view) {
        Global.connectivity.startDiscovery();
    }


}
