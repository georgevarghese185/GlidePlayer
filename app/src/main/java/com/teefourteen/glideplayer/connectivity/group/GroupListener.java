package com.teefourteen.glideplayer.connectivity.group;

public interface GroupListener {

    void onCreating();
    void onCreate();
    void onCreateFailure(String reason);

    void onConnecting();
    void onConnect();
    void onConnectFailure(String reason);

    void onJoining();
    void onJoin();
    void onJoinFailure();

    void onMemberConnect(Member member);
    void onMemberLeave(Member member);

    void onDisconnect();
}
