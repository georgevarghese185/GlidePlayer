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

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class Response {
    public final JSONObject jsonResponse;
    public final File fileResponse;
    public final boolean error;

    public Response(File file) {
        this.error = false;
        this.jsonResponse = null;
        this.fileResponse = file;
    }

    public Response(JSONObject jsonResponse) {
        this.error = false;
        this.jsonResponse = jsonResponse;
        this.fileResponse = null;
    }

    public Response(JSONObject jsonObject, boolean error) {
        this.error = error;
        this.jsonResponse = jsonObject;
        this.fileResponse = null;
    }

    public Response(String errorMessage) {
        this.error = true;
        this.fileResponse = null;
        this.jsonResponse = makeErrorJSON(errorMessage);
    }

    public Response() {
        this.error = false;
        this.fileResponse = null;
        this.jsonResponse = null;
    }

    public static JSONObject makeErrorJSON(String errorMessage) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("errorMessage", errorMessage);
            jsonObject.put("error", true);
        } catch (JSONException e) {
            Log.e("Response", "Exception while making error JSON", e);
        }

        return jsonObject;
    }
}
