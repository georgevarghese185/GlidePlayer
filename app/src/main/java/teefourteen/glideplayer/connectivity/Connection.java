package teefourteen.glideplayer.connectivity;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

    class Connection {
    private Socket clientSocket;
    private ServerSocket serverSocket;

    Connection(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    void closeConnection() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            } else if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ex) {
            //TODO: handle if required
        }
    }
}
