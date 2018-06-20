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

package com.teefourteen.glideplayer.connectivity.network;

public class Client {
    public final String clientId;
    public final String ipAddress;
    public final int serverPort;

    public Client(String clientId, String ipAddress, int serverPort) {

        this.clientId = clientId;
        this.ipAddress = ipAddress;
        this.serverPort = serverPort;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Client)
                && (((Client) obj).clientId.equals(this.clientId));
    }
}
