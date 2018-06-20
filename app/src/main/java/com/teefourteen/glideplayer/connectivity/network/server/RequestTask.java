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

package com.teefourteen.glideplayer.connectivity.network.server;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.ResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import static com.teefourteen.glideplayer.connectivity.network.Helpers.safeClose;

public class RequestTask {

    public final Client to;
    public final String requestType;
    public final Map<String, String> params;
    private ResponseType responseType;
    private String fileLocation;
    private AsyncRequestTask task;
    private ResponseListener responseListener = null;

    private HttpURLConnection connection = null;

    private boolean requestCanceled = false;
    private String cancelReason = "Request canceled";

    private enum ResponseType {
        FILE,
        JSON
    }

    public RequestTask(Client to, String requestType, Map<String, String> params,
                       @Nullable ResponseListener responseListener) {
        this.to = to;
        this.requestType = requestType;
        this.params = params;
        this.task = new AsyncRequestTask();
        this.responseListener = responseListener;
    }

    public void executeJSONRequest() {
        this.responseType = ResponseType.JSON;
        task.execute();
    }

    public Response syncJSONRequest() throws Exception {
        this.responseType = ResponseType.JSON;
        return syncRequest();
    }

    public void executeFileRequest(String fileLocation) {
        this.responseType = ResponseType.FILE;
        this.fileLocation = fileLocation;
        task.execute();
    }

    public Response syncFileRequest(String fileLocation) throws Exception {
        this.responseType = ResponseType.FILE;
        this.fileLocation = fileLocation;
        return syncRequest();
    }

    public void cancelRequest(String reason) {
        cancelReason = reason;
        task.cancel(false);
    }

    private Response doInBackground(Object[] objects) {
        try {
            return syncRequest();
        } catch (Exception e) {
            String errorMessage = requestCanceled ? cancelReason : e.toString();
            return new Response(errorMessage);
        }
    }

    public Response syncRequest() throws Exception {
        URL url = getURL(to.ipAddress, to.serverPort, requestType, params);
        connection = openAndSend(url);

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode > 299) {

            JSONObject errorJSON = getJSON(connection.getErrorStream());
            return new Response(errorJSON, true);

        } else if (responseType == ResponseType.JSON) {
            JSONObject responseJson = getJSON(connection.getInputStream());
            return new Response(responseJson);
        } else {
            File fileResponse = getFile(connection.getInputStream(), fileLocation);
            return new Response(fileResponse);
        }
    }

    @SuppressWarnings("unchecked")
    private void onPostExecute(Response response) {
        if(responseListener != null) {
            if(response.error) {
                responseListener.onError(response.jsonResponse);
            } else if(response.jsonResponse != null) {
                responseListener.onResponse(response.jsonResponse);
            } else if(response.fileResponse != null) {
                responseListener.onResponse(response.fileResponse);
            } else {
                JSONObject errorJSON = new JSONObject();
                try {
                    errorJSON.put("error", true);
                    errorJSON.put("errorMessage", "Exhausted Response cases");
                } catch (JSONException e) {
                    //ignored
                }
                responseListener.onError(errorJSON);
            }
        }
    }


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
    private HttpURLConnection openAndSend(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        return connection;
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
                if(task.isCancelled()) {
                    throw new InterruptedException("Request Cancelled: " + cancelReason);
                }
                fileOut.write(buffer, 0, bytesRead);
                fileOut.flush();
            }

            fileOut.close();
            dataIn.close();

            return file;
        } catch (Exception e) {
            safeClose(is);
            safeClose(dataIn);
            safeClose(fileOut);
            throw e;
        }
    }

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
            safeClose(reader);
            safeClose(is);
            throw e;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncRequestTask extends AsyncTask<Object, Float, Response> {

        @Override
        protected Response doInBackground(Object[] objects) {
            return RequestTask.this.doInBackground(objects);
        }

        @Override
        protected void onPostExecute(Response response) {
            RequestTask.this.onPostExecute(response);
        }
    }
}
