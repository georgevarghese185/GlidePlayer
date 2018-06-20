/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
