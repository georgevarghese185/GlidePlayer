package com.teefourteen.glideplayer.connectivity.listeners;

public interface GroupMemberListener {
    void onNewMemberJoined(String memberId, String memberName);
    void onMemberLeft(String member);
}
