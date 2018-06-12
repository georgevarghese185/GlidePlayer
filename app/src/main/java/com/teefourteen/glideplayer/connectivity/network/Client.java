package com.teefourteen.glideplayer.connectivity.network;

public class Client {
    public final String clientId;
    public final String ipAddress;
    public final int serverPort;

    public Client(String clientId, String ipAddress, int serverPort) {

        this.clientId = clientId;
        this.ipAddress = ipAddress;
        this.serverPort = serverPort;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Client)
                && (((Client) obj).clientId.equals(this.clientId));
    }
}
