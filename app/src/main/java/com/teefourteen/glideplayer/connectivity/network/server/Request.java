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

import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Request {
    public final String requestType;
    public final Map<String, String> requestParams;
    public final String ip;

    public Request(NanoHTTPD.IHTTPSession session) {
        this.requestType = Uri.parse(session.getUri()).getPath();
        this.requestParams = session.getParms();
        this.ip = session.getHeaders().get("http-client-ip");
    }

    Response respond(JSONObject responseObject) {
        return new Response(responseObject);
    }
    Response respond(File file) {
        return new Response(file);
    }
    Response respond() {
        return new Response(new JSONObject());
    }
    Response reject(String reason) {
        return new Response(reason);
    }
}
