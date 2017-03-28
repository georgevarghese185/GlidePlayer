package com.teefourteen.glideplayer.connectivity.listeners;

/**
 * Created by George on 1/31/2017.
 */

public interface RequestListener {
    Object onNewRequest(String deviceId, int action, Object requestData);
}
