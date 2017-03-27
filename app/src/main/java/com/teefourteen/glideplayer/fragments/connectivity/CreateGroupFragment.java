package com.teefourteen.glideplayer.fragments.connectivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.ShareGroup;
import com.teefourteen.glideplayer.connectivity.listeners.GroupCreationListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import com.teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;

public class CreateGroupFragment extends Fragment implements GroupCreationListener,
        GroupMemberListener{
    private View rootView;
    private ShareGroup.CreationStatus groupCreationStatus
            = ShareGroup.CreationStatus.GROUP_NOT_CREATED;
    private ShareGroup group;
    private String groupName;
    private ConnectionCloseListener closeListener;

    public CreateGroupFragment() {
        // Required empty public constructor
    }

    public static CreateGroupFragment newInstance(ShareGroup group,
                                                  ConnectionCloseListener closeListener) {
        CreateGroupFragment fragment = new CreateGroupFragment();
        fragment.group = group;
        fragment.closeListener = closeListener;

        if(group.getCreationStatus() != ShareGroup.CreationStatus.GROUP_NOT_CREATED) {
            fragment.groupName = group.getGroupName();
            fragment.groupCreationStatus = group.getCreationStatus();
        }

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_create_group, container, false);

        groupCreationStatus = group.getCreationStatus();

        switch (groupCreationStatus) {
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
            EditText editText = (EditText)rootView.findViewById(R.id.group_name);
            editText.setHint("Cannot be empty!");
            editText.setHintTextColor(Color.RED);
        } else {
            this.groupName = groupName;
            SharedPreferences.Editor editor = getContext()
                    .getSharedPreferences(Global.SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(ConnectivityFragment.LAST_USED_GROUP_NAME_KEY, groupName);
            editor.apply();

            group.createGroup(groupName, this, this);
            groupCreationStatus = ShareGroup.CreationStatus.GROUP_WAITING_FOR_CREATION;
            onWaitingForGroupCreation();
        }
    }

    private void onWaitingForGroupCreation() {
        EditText editText = (EditText)rootView.findViewById(R.id.group_name);
        editText.setText(groupName); //in case this is called on first run of fragment.
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
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
        EditText editText = (EditText)rootView.findViewById(R.id.group_name);
        editText.setText(groupName); //in case this is called on first run of fragment.
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
        rootView.findViewById(R.id.creating_group_progress_bar).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.create_group).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.group_created_message).setVisibility(View.VISIBLE);
        groupCreationStatus = ShareGroup.CreationStatus.GROUP_CREATED;
    }

    @Override
    public void onGroupCreationFailed(String failureMessage) {
        Toast.makeText(getContext(), failureMessage, Toast.LENGTH_LONG).show();
        EditText editText = (EditText)rootView.findViewById(R.id.group_name);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setClickable(true);
        groupCreationStatus = ShareGroup.CreationStatus.GROUP_NOT_CREATED;
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

    @Override
    public void onPause() {
        super.onPause();
        if(group != null) {
            group.unregisterGroupCreationListener(this);
            group.unregisterGroupMemberListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(group != null) {
            group.registerGroupCreationListener(this);
            group.registerGroupMemberListener(this);
            ((ArrayAdapter) ((ListView) rootView.findViewById(R.id.peer_list))
                    .getAdapter()).notifyDataSetChanged();
        }
    }
}
