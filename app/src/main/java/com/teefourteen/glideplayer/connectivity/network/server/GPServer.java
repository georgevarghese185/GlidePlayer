package com.teefourteen.glideplayer.connectivity.network.server;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.teefourteen.glideplayer.connectivity.network.Client;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;


public class GPServer extends fi.iki.elonen.NanoHTTPD {
    private static final String UNKNOWN_ERROR = "{\"error\": true, \"errorMessage\":\"Unknown error\"}";

    private GPServerListener serverListener;

    public interface GPServerListener {
        com.teefourteen.glideplayer.connectivity.network.server.Response onRequest(Request request);
    }

    public GPServer(int port, @NonNull GPServerListener serverListener) {
        super(port);
        this.serverListener = serverListener;
    }

    //Responds to incoming requests by calling GPServerListener that was provided in the constructor
    @Override
    public Response serve(IHTTPSession session) {
        com.teefourteen.glideplayer.connectivity.network.server.Response response =
                serverListener.onRequest(new Request(session));

        if(response.error && response.jsonResponse != null) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    response.jsonResponse.toString());
        } else if(response.fileResponse != null) {
            FileInputStream fIn = null;
            try {
                fIn = new FileInputStream(response.fileResponse);
                return newFixedLengthResponse(Response.Status.OK, "application/octet-stream",
                        fIn, fIn.getChannel().size());
            } catch (Exception e) {
                close(fIn);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "application/json", UNKNOWN_ERROR);
            }

        } else if(response.jsonResponse != null) {

            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    response.jsonResponse.toString());

        } else {

            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    UNKNOWN_ERROR);

        }
    }

    //Make plain requests with no expected response data
    public com.teefourteen.glideplayer.connectivity.network.server.Response
            request(Client to, String requestType, Map<String, String> params) {
        try {

            URL url = getURL(to.ipAddress, to.serverPort, requestType, params);
            HttpURLConnection connection = sendRequestParameters(url);

            int responseCode = connection.getResponseCode();
            if(responseCode < 200 || responseCode > 299) {

                JSONObject errorJSON = getJSON(connection.getErrorStream());
                return new com.teefourteen.glideplayer.connectivity.network.server.Response(errorJSON, true);

            } else {
                return new com.teefourteen.glideplayer.connectivity.network.server.Response(new JSONObject());
            }

        } catch (Exception e) {
            return new com.teefourteen.glideplayer.connectivity.network.server.Response(e.toString());
        }
    }

    //Requests where a JSON is the expected response
    public com.teefourteen.glideplayer.connectivity.network.server.Response
            requestJson(Client to, String requestType, Map<String, String> params) {
        try {

            URL url = getURL(to.ipAddress, to.serverPort, requestType, params);
            HttpURLConnection connection = sendRequestParameters(url);

            int responseCode = connection.getResponseCode();
            if(responseCode < 200 || responseCode > 299) {

                JSONObject errorJSON = getJSON(connection.getErrorStream());
                return new com.teefourteen.glideplayer.connectivity.network.server.Response(errorJSON, true);

            } else {

                JSONObject responseJson = getJSON(connection.getInputStream());
                return new com.teefourteen.glideplayer.connectivity.network.server.Response(responseJson);

            }

        } catch (Exception e) {
            return new com.teefourteen.glideplayer.connectivity.network.server.Response(e.toString());
        }
    }

    //Requests where a File is the expected response
    public com.teefourteen.glideplayer.connectivity.network.server.Response
            requestFile(Client to, String requestType, Map<String, String> params, String fileLocation) {
        try {

            URL url = getURL(to.ipAddress, to.serverPort, requestType, params);
            HttpURLConnection connection = sendRequestParameters(url);

            int responseCode = connection.getResponseCode();
            if(responseCode < 200 || responseCode > 299) {

                JSONObject errorJSON = getJSON(connection.getErrorStream());
                return new com.teefourteen.glideplayer.connectivity.network.server.Response(errorJSON, true);

            } else {

                File fileResponse = getFile(connection.getInputStream(), fileLocation);
                return new com.teefourteen.glideplayer.connectivity.network.server.Response(fileResponse);

            }

        } catch (Exception e) {
            return new com.teefourteen.glideplayer.connectivity.network.server.Response(e.toString());
        }
    }

    //Makes a URI using IP address and port and puts query paramters into the URL as well
    private URL getURL(String ip, int port, String requestType,
                       Map<String, String> requestParams) throws MalformedURLException {
        Uri.Builder builder = new Uri.Builder();

        builder.scheme("http");
        builder.encodedAuthority(ip + ":" + port);
        builder.path(requestType);

        for(Map.Entry<String, String> entry : requestParams.entrySet()) {
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        String url = builder.build().toString();

        return new URL(url);
    }

    //Only GET requests supported
    private HttpURLConnection sendRequestParameters(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        return connection;
    }

    //Get a JSON response from the input stream. (Used for error JSONs as well)
    private JSONObject getJSON(InputStream is) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> response = new ArrayList<>();

            String line;

            while ((line = reader.readLine()) != null) {
                response.add(line);
            }

            reader.close();

            return new JSONObject(response.get(0));
        } catch (Exception e) {
            close(reader);
            close(is);
            throw e;
        }
    }

    //Get a file response from the input stream
    private File getFile(InputStream is, String fileLocation) throws Exception {
        DataInputStream dataIn = null;
        FileOutputStream fileOut = null;

        try {
            dataIn = new DataInputStream(is);

            File file = new File(fileLocation);
            if (file.exists()) {
                file.delete();
            }
            boolean created = file.createNewFile();

            if (!created) {
                throw new IOException("Failed to create new file " + fileLocation);
            }

            int bytesRead;
            byte[] buffer = new byte[8096];

            fileOut = new FileOutputStream(file);

            while ((bytesRead = dataIn.read(buffer, 0, buffer.length)) > 0) {
                fileOut.write(buffer, 0, bytesRead);
                fileOut.flush();
            }

            fileOut.close();
            dataIn.close();

            return file;
        } catch (Exception e) {
            close(is);
            close(dataIn);
            close(fileOut);
            throw e;
        }
    }

    //safely close any Object that implements Closeable
    private void close(@Nullable Closeable obj) {
        if(obj != null) {
            try {
                obj.close();
            } catch (Exception e) {
                //Ignore
            }
        }
    }
}
