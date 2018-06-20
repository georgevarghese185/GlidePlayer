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

package com.teefourteen.glideplayer.connectivity.network.server;

import android.support.annotation.NonNull;

import com.teefourteen.glideplayer.connectivity.network.Helpers;

import org.json.JSONObject;

import java.io.FileInputStream;

import fi.iki.elonen.NanoHTTPD;


public class GPServer extends NanoHTTPD {
    private static final String UNKNOWN_ERROR = "{\"error\": true, \"errorMessage\":\"Unknown error\"}";
    private GPServerListener serverListener;

    public interface GPServerListener {
        com.teefourteen.glideplayer.connectivity.network.server.Response onRequest(Request request);
    }

    public GPServer(int port, @NonNull GPServerListener serverListener) {
        super(port);
        this.serverListener = serverListener;
    }

    //Responds to incoming requests by calling GPServerListener that was provided in the constructor
    @Override
    public Response serve(IHTTPSession session) {
        com.teefourteen.glideplayer.connectivity.network.server.Response response =
                serverListener.onRequest(new Request(session));

        if(response.error) {
            if(response.jsonResponse != null) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                        response.jsonResponse.toString());
            } else {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                        UNKNOWN_ERROR);
            }
        } else if(response.fileResponse != null) {
            FileInputStream fIn = null;
            try {
                fIn = new FileInputStream(response.fileResponse);
                return newFixedLengthResponse(Response.Status.OK, "application/octet-stream",
                        fIn, fIn.getChannel().size());
            } catch (Exception e) {
                Helpers.safeClose(fIn);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "application/json", UNKNOWN_ERROR);
            }

        } else if(response.jsonResponse != null) {

            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    response.jsonResponse.toString());

        } else {
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    new JSONObject().toString());
        }
    }
}
