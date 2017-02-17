package teefourteen.glideplayer.fragments.connectivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.Connection;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.listeners.GroupCreationListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;

public class CreateGroupFragment extends Fragment implements GroupCreationListener,
        GroupMemberListener{
    private View rootView;
    enum ConnectionStatus {
        GROUP_NOT_CREATED,
        GROUP_CREATED,
        GROUP_WAITING_FOR_CREATION
    }
    private ConnectionStatus groupConnectionStatus = ConnectionStatus.GROUP_NOT_CREATED;
    private ShareGroup group;
    private ConnectionCloseListener closeListener;

    public CreateGroupFragment() {
        // Required empty public constructor
    }

    public static CreateGroupFragment newInstance(ShareGroup group,
                                                  ConnectionCloseListener closeListener) {
        CreateGroupFragment fragment = new CreateGroupFragment();
        fragment.group = group;
        fragment.closeListener = closeListener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_create_group, container, false);

        switch (groupConnectionStatus) {
            case GROUP_NOT_CREATED:
                rootView.findViewById(R.id.create_group).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createGroup(v);
                    }
                });
                break;
            case GROUP_WAITING_FOR_CREATION:
                onWaitingForGroupCreation();
                break;
            case GROUP_CREATED:
                onGroupCreated();
                break;
        }

        SharedPreferences preferences =
                getContext().getSharedPreferences(Global.SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        String lastGroupName = preferences.getString(ConnectivityFragment.LAST_USED_GROUP_NAME_KEY, null);

        if(lastGroupName != null) {
            ((EditText) rootView.findViewById(R.id.group_name)).setText(lastGroupName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1,
                ShareGroup.shareGroupWeakReference.get().getMemberList());
        ((ListView)rootView.findViewById(R.id.peer_list)).setAdapter(adapter);

        rootView.findViewById(R.id.close_connection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConnection();
            }
        });

        return rootView;
    }

    private void createGroup(View view){
        String groupName = ((EditText) rootView.findViewById(R.id.group_name)).getText().toString();
        if(groupName.equals("")) {
            EditText textView = (EditText)rootView.findViewById(R.id.group_name);
            textView.setHint("Cannot be empty!");
            textView.setHintTextColor(Color.RED);
        } else {
            SharedPreferences.Editor editor = getContext()
                    .getSharedPreferences(Global.SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(ConnectivityFragment.LAST_USED_GROUP_NAME_KEY, groupName);
            editor.apply();

            group.createGroup(groupName, this, this);
            groupConnectionStatus = ConnectionStatus.GROUP_WAITING_FOR_CREATION;
            onWaitingForGroupCreation();
        }
    }

    private void onWaitingForGroupCreation() {
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.create_group).setEnabled(false);
    }

    private void closeConnection() {
        group.deleteGroup();
        group.close();
        group = null;
        closeListener.onConnectionClose();
    }

    @Override
    public void onGroupCreated() {
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.create_group).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.group_created_message).setVisibility(View.VISIBLE);
        groupConnectionStatus = ConnectionStatus.GROUP_CREATED;
    }

    @Override
    public void onGroupCreationFailed(String failureMessage) {
        Toast.makeText(getContext(), failureMessage, Toast.LENGTH_LONG).show();
        groupConnectionStatus = ConnectionStatus.GROUP_NOT_CREATED;
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.create_group).setEnabled(true);
    }

    @Override
    public void onMemberLeft(String member) {
        ((ArrayAdapter) ((ListView) rootView.findViewById(R.id.peer_list))
                .getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onNewMemberJoined(String memberId, String memberName) {
        Toast.makeText(getContext(), memberName + " joined", Toast.LENGTH_LONG).show();
        ((ArrayAdapter) ((ListView) rootView.findViewById(R.id.peer_list))
                .getAdapter()).notifyDataSetChanged();
    }
}
