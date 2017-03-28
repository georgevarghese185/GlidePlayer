package com.teefourteen.glideplayer.connectivity.listeners;


public interface ResponseListener {
    void onResponseReceived(Object responseData);
    void onRequestFailed();
}