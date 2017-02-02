package teefourteen.glideplayer.fragments.connectivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.listeners.GroupCreationListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.fragments.connectivity.listeners.ConnectionCloseListener;

public class CreateGroupFragment extends Fragment implements GroupCreationListener,
        GroupMemberListener{
    private View rootView;
    private boolean groupCreated = false;
    private boolean waitingForGroupCreation = false;
    private ShareGroup group;
    private ConnectionCloseListener closeListener;
    public static final int GROUP_CREATED_MSG = 3;
    public static final int GROUP_CREATION_FAILED_MSG = 4;

    private final Handler groupCreatedHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.arg1 == GROUP_CREATED_MSG) {
                waitingForGroupCreation = false;
                groupCreated = true;
                onGroupCreated();
            } else if(msg.arg1 == GROUP_CREATION_FAILED_MSG){
                //TODO: handle failure
            }
            return false;
        }
    });

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

        if(groupCreated) {
            onGroupCreated();
        } else if(waitingForGroupCreation) {
            onWaitingForGroupCreation();
        } else {
            rootView.findViewById(R.id.create_group).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createGroup(v);
                }
            });
        }

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
        group.createGroup(groupName, this, this);
        waitingForGroupCreation = true;
        onWaitingForGroupCreation();
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
    }

    @Override
    public void onGroupCreationFailed(String failureMessage) {
        Toast.makeText(getContext(), failureMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMemberLeft(String member) {

    }

    @Override
    public void onNewMemberJoined(String memberId, String memberName) {
        Toast.makeText(getContext(), memberName + " joined", Toast.LENGTH_LONG).show();
    }
}
