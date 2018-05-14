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
