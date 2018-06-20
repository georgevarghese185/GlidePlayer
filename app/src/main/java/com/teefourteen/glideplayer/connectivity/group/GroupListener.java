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

public interface GroupListener {

    void onCreating();
    void onCreate();
    void onCreateFailure(String reason);

    void onConnecting();
    void onConnect();
    void onConnectFailure(String reason);

    void onJoining();
    void onJoin();
    void onJoinFailure();

    void onMemberConnect(Member member);
    void onMemberLeave(Member member);

    void onDisconnect();
}
