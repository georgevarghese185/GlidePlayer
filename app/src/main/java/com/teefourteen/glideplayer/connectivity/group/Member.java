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

import com.teefourteen.glideplayer.connectivity.network.Network;

public class Member {
    public final String userName;
    public final String memberId;
    private Network network;

    private static final int TYPE_SONG = 1;
    private static final int TYPE_VIDEO = 2;
    private static final int TYPE_ALBUM_ART = 3;
    private static final int TYPE_COMMAND = 4;

    public Member(String userName, String memberId, Network network) {
        this.userName = userName;
        this.memberId = memberId;
        this.network = network;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Member)
                && ((Member) obj).memberId.equals(this.memberId);
    }

    //    public void getSong(String memberId, String songId, ResponseListener responseListener) {
//        getFile(memberId, songId, TYPE_SONG, responseListener);
//    }
//
//    public void getVideo(String memberId, String videoId, ResponseListener responseListener) {
//        getFile(memberId, videoId, TYPE_VIDEO, responseListener);
//    }
//
//    public void getAlbumArt(String memberId, String albumId, ResponseListener responseListener) {
//        getFile(memberId, albumId, TYPE_ALBUM_ART, responseListener);
//    }
//
//    public void getFile(String memberId, String itemId, int itemType, ResponseListener responseListener) {
//        network.sendRequest(memberId, itemType, itemId, responseListener);
//    }
//
//    public void sendCommand(String memberId, String command, ResponseListener responseListener) {
//        network.sendRequest(memberId, TYPE_COMMAND, command, responseListener);
//    }
}
