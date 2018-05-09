package com.teefourteen.glideplayer.connectivity.group;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class GroupService extends Service {
    @Nullable private Group mConnectedGroup = null;

    public class GroupServiceBinder extends Binder {
        public void findGroups(GroupFinderListener finderListener) {
            GroupService.this.findGroups(finderListener);
        }

        public void stopFindingGroups() {
            GroupService.this.stopFindingGroups();
        }

        public void connectToGroup(Group group) {
            GroupService.this.connectToGroup(group);
        }

        public void disconnectFromGroup() {
            GroupService.this.disconnectFromGroup();
        }

        @Nullable
        public Group getConnectedGroup() {
            return mConnectedGroup;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new GroupServiceBinder();
    }

    public void findGroups(GroupFinderListener finderListener) {

    }

    public void stopFindingGroups() {

    }

    public void connectToGroup(Group group) {

    }

    public void disconnectFromGroup() {

    }

    public static interface GroupFinderListener {
        void foundGroups(Group[] groups);
    }
}
