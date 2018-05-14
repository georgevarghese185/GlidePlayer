package com.teefourteen.glideplayer.connectivity.network.server;

import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Request {
    public final String requestType;
    public final Map<String, String> requestParams;

    public Request(NanoHTTPD.IHTTPSession session) {
        this.requestType = Uri.parse(session.getUri()).getPath();
        this.requestParams = session.getParms();
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
