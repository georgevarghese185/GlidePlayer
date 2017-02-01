package teefourteen.glideplayer.connectivity;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Connection implements Closeable {
    private Socket clientSocket;
    private TransmissionType lastTransmission = null;

    private enum TransmissionType {
        READ,
        WRITE
    }


    static class ListenServer implements Closeable{
        private ServerSocket serverSocket;
        private int listenPort;

        ListenServer(int listenPort) throws IOException {
            serverSocket = new ServerSocket(listenPort);
            this.listenPort = serverSocket.getLocalPort();
        }

        int getPort() {
            return listenPort;
        }

        Connection listen() throws IOException {
            try {
                Socket socket = serverSocket.accept();
                return new Connection(socket);
            } catch (SocketException e) {
                //Socket closed by close() or stopListening()
            }
            return null;
        }

        @Override
        public void close() {
            if(serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    //TODO: handle if needed
                }
            }
        }
    }

    private Connection(Socket socket) {
        clientSocket = socket;
    }

    public Connection(InetAddress inetAddress, int port)throws IOException {
        clientSocket = new Socket(inetAddress, port);
    }

    public Socket getSocket() {
        return clientSocket;
    }


    int getNextInt(int numberOfInts)throws IOException {
        readyWait(TransmissionType.READ);
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return Integer.parseInt(reader.readLine());
    }

    String getNextString()throws IOException {
        readyWait(TransmissionType.READ);
        BufferedReader input = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));

        return input.readLine();
    }

    Object getNextObject()throws IOException {
        readyWait(TransmissionType.READ);
        ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
        try {
            return input.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    void sendInt(int n)throws IOException {
        readyWait(TransmissionType.WRITE);
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
        pw.println(n);
    }

    void sendString(String string)throws IOException {
        readyWait(TransmissionType.WRITE);
        PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
        output.println(string);
    }

    void sendObject(Object obj)throws IOException {
        readyWait(TransmissionType.WRITE);
        ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
        output.writeObject(obj);
    }

    InetAddress getRemoteInetAddress()throws IOException {
        return clientSocket.getInetAddress();
    }

    int getRemotePort()throws IOException {
        return clientSocket.getPort();
    }

    void readyWait(TransmissionType nextTransmission) throws IOException {
        if(nextTransmission == TransmissionType.READ
                && lastTransmission == TransmissionType.READ) {
            new PrintWriter(clientSocket.getOutputStream(), true).println("ready");
        } else if(nextTransmission == TransmissionType.WRITE
                && lastTransmission == TransmissionType.WRITE) {
            new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
        }

        lastTransmission = nextTransmission;
    }

    @Override
    public void close() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            //TODO: handle if needed
        }
    }
}
