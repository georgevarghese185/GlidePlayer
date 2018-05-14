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

    private JSONObject makeErrorJSON(String errorMessage) {
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
