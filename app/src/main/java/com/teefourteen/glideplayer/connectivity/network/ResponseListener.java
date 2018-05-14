package com.teefourteen.glideplayer.connectivity.network;

import org.json.JSONObject;

public interface ResponseListener<T> {
    void onResponse(T response);
    void onError(JSONObject error);
}
