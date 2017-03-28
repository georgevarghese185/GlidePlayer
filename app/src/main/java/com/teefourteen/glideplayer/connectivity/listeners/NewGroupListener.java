package com.teefourteen.glideplayer.connectivity.listeners;

/**
 * Created by George on 1/16/2017.
 */
public interface NewGroupListener {
    void newGroupFound(String Id, String groupName, String ownerName, String deviceName, int memberCount);
}
