package teefourteen.glideplayer.connectivity;


import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;

import teefourteen.glideplayer.connectivity.listeners.ErrorListener;
import teefourteen.glideplayer.connectivity.listeners.GroupCreatedListener;
import teefourteen.glideplayer.connectivity.listeners.NewGroupListener;
import teefourteen.glideplayer.fragments.connectivity.CreateGroupFragment;

public class ShareGroup implements NewGroupListener, ErrorListener {
    private NetworkManager netManager;
    private String userName;
    private ArrayList<AvailableGroup> availableGroups;
    private Activity activity;
    private ArrayAdapter groupListAdapter;

    public class AvailableGroup {
        public final String Id;
        public final String groupName;
        public final String ownerName;
        public final String deviceName;
        public int memberCount;

        AvailableGroup(String Id, String groupName, String ownerName, String deviceName, int memberCount) {
            this.Id = Id;
            this.groupName = groupName;
            this.ownerName = ownerName;
            this.deviceName = deviceName;
            this.memberCount = memberCount;
        }
    }

    public ShareGroup(Activity activity, String userName){
        this.activity = activity;
        netManager = new NetworkManager(activity);
        this.userName = userName;
        availableGroups = new ArrayList<>();
    }

    public void createGroup(String groupName, final Handler groupCreatedHandler) {
        netManager.createGroup(userName, groupName, this, new GroupCreatedListener() {
            @Override
            public void onGroupCreated() {
                Message m = new Message();
                m.arg1 = CreateGroupFragment.GROUP_CREATED_MSG;
                groupCreatedHandler.sendMessage(m);
            }
        });
    }

    public ArrayList<AvailableGroup> getGroupList() {
        return availableGroups;
    }

    public void findGroups(ArrayAdapter groupListAdapter) {
        this.groupListAdapter = groupListAdapter;
        netManager.discoverGroups(this, this);
    }

    public void deleteGroup() {
        netManager.closeGroup();
    }

    public void stopFindingGroups() {
        netManager.stopDiscovery();
    }

    @Override
    public void newGroupFound(String Id, String groupName, String ownerName, String deviceName, int memberCount) {
        availableGroups.add(new AvailableGroup(Id,groupName,ownerName, deviceName, memberCount));
        groupListAdapter.notifyDataSetChanged();
    }

    @Override
    public void error(String errorMsg) {
        Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
    }
}

