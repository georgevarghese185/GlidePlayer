package com.teefourteen.glideplayer.fragments.connectivity;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.ShareGroup;
import com.teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import com.teefourteen.glideplayer.fragments.connectivity.adapters.GroupAdapter;
import com.teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;


public class JoinGroupFragment extends Fragment implements GroupConnectionListener,
        GroupMemberListener{
    private View rootView;
    private ShareGroup group;
    private GroupAdapter groupAdapter;
    private ConnectionCloseListener closeListener;
    ShareGroup.JoinStatus joinStatus = ShareGroup.JoinStatus.NOT_CONNECTED;
    String connectedGroupName;

    public JoinGroupFragment() {
        // Required empty public constructor
    }

    public static JoinGroupFragment newInstance(ShareGroup group,
                                                ConnectionCloseListener closeListener) {
        JoinGroupFragment fragment = new JoinGroupFragment();
        fragment.group = group;

        if(group.getJoinStatus() != ShareGroup.JoinStatus.NOT_CONNECTED) {
            fragment.connectedGroupName = group.getGroupName();
            fragment.joinStatus = group.getJoinStatus();
        }

        fragment.closeListener = closeListener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_join_group, container, false);
        initializeViews();

        rootView.findViewById(R.id.close_connection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConnection();
            }
        });

        return rootView;
    }

    private void initializeViews() {
        switch (joinStatus) {
            case CONNECTED:
                onConnectionSuccess(connectedGroupName);
                break;
            case CONNECTING:
                setStatusConnecting();
                break;
            case RECENTLY_DISCONNECTED:
                ((TextView)rootView.findViewById(R.id.connection_status))
                        .setText("Disconnected from " + connectedGroupName + ". Owner closed the group");
            case NOT_CONNECTED:
                groupAdapter = new GroupAdapter(getContext(), group.getGroupList());
                setupGroupList();
                group.findGroups(groupAdapter);
                break;
            case EXCHANGING_INFO:
                onExchangingInfo();
                break;
        }
    }

    private void setupGroupList() {
        ListView groupList = (ListView) rootView.findViewById(R.id.group_list);
        groupList.setAdapter(groupAdapter);
        final JoinGroupFragment fragment = this;
        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                group.connectToGroup(position, fragment, fragment);
                setStatusConnecting();
                joinStatus = ShareGroup.JoinStatus.CONNECTING;
            }
        });
    }

    private void closeConnection() {
        group.stopFindingGroups();
        group.deleteGroup();
        group.close();
        group = null;
        closeListener.onConnectionClose();
    }

    @Override
    public void onConnectionFailed(String failureMessage) {
        Toast.makeText(getContext(), failureMessage, Toast.LENGTH_LONG).show();
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.INVISIBLE);
        TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
        connectionStatus.setText("Not Connected");
        connectionStatus.setTextColor(Color.parseColor("#0a5900"));
        this.joinStatus = ShareGroup.JoinStatus.NOT_CONNECTED;
        initializeViews();
    }

    @Override
    public void onConnectionSuccess(String connectedGroup) {
        this.connectedGroupName = connectedGroup;
        ((ListView)rootView.findViewById(R.id.group_list)).setOnItemClickListener(null);
        setStatusConnected();
        joinStatus = ShareGroup.JoinStatus.CONNECTED;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1,
                ShareGroup.shareGroupWeakReference.get().getMemberList());
        ((ListView)rootView.findViewById(R.id.group_list)).setAdapter(adapter);
        ((TextView) rootView.findViewById(R.id.group_members_caption)).setText("Group Members");
    }

    private void setStatusConnected() {
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(), "Connected to " + connectedGroupName,
                Toast.LENGTH_LONG).show();
        TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
        connectionStatus.setText("Connected to " + connectedGroupName);
        connectionStatus.setTextColor(Color.parseColor("#FF0A5900"));
    }

    private void setStatusConnecting() {
        TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
        connectionStatus.setText("Connecting");
        connectionStatus.setTextColor(Color.parseColor("#FF00506B"));
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.VISIBLE);
    }

    @Override
    public void onMemberLeft(String member) {
        ((ArrayAdapter) ((ListView) rootView.findViewById(R.id.group_list))
                .getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onNewMemberJoined(String memberId, String memberName) {
        Toast.makeText(getContext(), memberName + " connected", Toast.LENGTH_LONG).show();
        ((ArrayAdapter) ((ListView) rootView.findViewById(R.id.group_list))
                .getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onExchangingInfo() {
        TextView connectionStatus = (TextView)rootView.findViewById(R.id.connection_status);
        connectionStatus.setText("exchanging libraries");
    }

    @Override
    public void onOwnerDisconnected() {
        Toast.makeText(getContext(), "Owner disconnected. Group closed", Toast.LENGTH_LONG).show();
        joinStatus = ShareGroup.JoinStatus.RECENTLY_DISCONNECTED;
        ((TextView) rootView.findViewById(R.id.group_members_caption)).setText("Nearby Groups");
        initializeViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (group != null) {
            group.unregisterGroupConnectionListener(this);
            group.unregisterGroupMemberListener(this);

            if(joinStatus == ShareGroup.JoinStatus.NOT_CONNECTED
                    && getContext() != null) {
                group.stopFindingGroups();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (group != null) {
            group.registerGroupConnectionListener(this);
            group.registerGroupMemberListener(this);
            joinStatus = group.getJoinStatus();
        }
    }
}
