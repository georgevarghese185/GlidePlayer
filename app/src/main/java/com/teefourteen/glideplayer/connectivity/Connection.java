/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.connectivity;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    public static final int DATA_TYPE_INTEGER = 100;
    public static final int DATA_TYPE_LONG = 101;
    public static final int DATA_TYPE_STRING = 102;
    public static final int DATA_TYPE_OBJECT = 103;
    public static final int DATA_TYPE_FILE = 104;
    public static final int DATA_TYPE_NULL = 105;
    public static final int DATA_TYPE_STREAM = 106;
    
    private Socket clientSocket;
    private TransmissionType lastTransmission = null;

    private enum TransmissionType {
        READ,
        WRITE
    }


    public static class ListenServer implements Closeable{
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


    public int getNextInt()throws IOException {
        return (int) getNextLong();
    }

    public long getNextLong()throws IOException {
        readyWait(TransmissionType.READ);
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return Long.parseLong(reader.readLine());
    }

    public String getNextString()throws IOException {
        readyWait(TransmissionType.READ);
        BufferedReader input = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));

        return input.readLine();
    }

    public  Object getNextObject()throws IOException {
        readyWait(TransmissionType.READ);
        ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
        try {
            return input.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public File getNextFile(String filePath, long size)throws IOException {
        File file = new File(filePath);
        if(file.exists()) {
            file.delete();
        }
        file.createNewFile();

        int bytesRead;
        long bytesSent;
        DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
        FileOutputStream fileOut = new FileOutputStream(file);
        byte[] buffer = new byte[8096];

        readyWait(TransmissionType.READ);

        for(bytesSent = 0; bytesSent < size; bytesSent += bytesRead) {
            bytesRead = dataIn.read(buffer,0,buffer.length);
            fileOut.write(buffer, 0, bytesRead);
            fileOut.flush();
        }

        fileOut.close();

        return file;
    }

    public void sendInt(int n)throws IOException {
        readyWait(TransmissionType.WRITE);
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
        pw.println(n);
    }

    public void sendLong(long n)throws IOException {
        readyWait(TransmissionType.WRITE);
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
        pw.println(n);
    }

    public void sendString(String string)throws IOException {
        readyWait(TransmissionType.WRITE);
        PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
        output.println(string);
    }

    public void sendObject(Object obj)throws IOException {
        readyWait(TransmissionType.WRITE);
        ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
        output.writeObject(obj);
    }

    public void sendFile(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        byte[] buffer = new byte[8096];
        DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        int bytesRead;

        readyWait(TransmissionType.WRITE);
        while ((bytesRead = fin.read(buffer, 0, buffer.length)) > 0) {
            dataOut.write(buffer, 0, bytesRead);
        }

        fin.close();
    }

    InetAddress getRemoteInetAddress()throws IOException {
        return clientSocket.getInetAddress();
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
