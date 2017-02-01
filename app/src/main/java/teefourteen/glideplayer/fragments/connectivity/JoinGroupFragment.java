package teefourteen.glideplayer.fragments.connectivity;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.fragments.connectivity.adapters.GroupAdapter;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;


public class JoinGroupFragment extends Fragment implements GroupConnectionListener,
        GroupMemberListener{
    private View rootView;
    private ShareGroup group;
    private GroupAdapter groupAdapter;
    private ConnectionCloseListener closeListener;

    public JoinGroupFragment() {
        // Required empty public constructor
    }

    public static JoinGroupFragment newInstance(ShareGroup group,
                                                ConnectionCloseListener closeListener) {
        JoinGroupFragment fragment = new JoinGroupFragment();
        fragment.group = group;
        fragment.closeListener = closeListener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_join_group, container, false);
        setupGroupList();

        rootView.findViewById(R.id.close_connection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConnection();
            }
        });

        return rootView;
    }

    private void setupGroupList() {
        groupAdapter = new GroupAdapter(getContext(), group.getGroupList());
        ListView groupList = (ListView) rootView.findViewById(R.id.group_list);
        groupList.setAdapter(groupAdapter);
        final JoinGroupFragment fragment = this;
        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                group.connectToGroup(position, fragment, fragment);
                TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
                connectionStatus.setText("Connecting");
                connectionStatus.setTextColor(Color.parseColor("#FF00506B"));
                rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.VISIBLE);
            }
        });
        group.findGroups(groupAdapter);
    }

    private void closeConnection() {
        group.stopFindingGroups();
        group.close();
        group = null;
        closeListener.onConnectionClose();
    }

    @Override
    public void onConnectionFailed(String failureMessage) {
        Toast.makeText(getContext(), failureMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuccess(String connectedGroup) {
        ((ListView)rootView.findViewById(R.id.group_list)).setOnItemClickListener(null);
        TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
        connectionStatus.setText("Connected to " + connectedGroup);
        connectionStatus.setTextColor(Color.parseColor("#FF0A5900"));
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(),"Connected to " + connectedGroup + " successfully",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMemberLeft(String member) {

    }

    @Override
    public void onNewMemberJoined(String memberId, String memberName) {
        Toast.makeText(getContext(), memberName + " connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onExchangingInfo() {
        TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
        connectionStatus.setText("exchanging libraries");
    }
}
