package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CommunicationThread extends Thread {
    private final ServerThread serverThread;
    private final Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }

        try {
            BufferedReader bufferedReader = Utils.getReader(socket);
            PrintWriter printWriter = Utils.getWriter(socket);

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for data parameters from client (op., key, value)");

            String request = bufferedReader.readLine();
            if (request == null || request.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving request from client!");
                return;
            }

            HttpClient httpClient = new DefaultHttpClient();
            String pageSourceCode = null;

            HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS);
            HttpResponse httpGetResponse = httpClient.execute(httpGet);
            HttpEntity httpGetEntity = httpGetResponse.getEntity();
            if (httpGetEntity != null)
                pageSourceCode = EntityUtils.toString(httpGetEntity);
            if (pageSourceCode == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the time from the webservice!");
                return;
            } else {
                Log.i(Constants.TAG, pageSourceCode);
            }

            String time = new JSONObject(pageSourceCode).getString("unixtime");
            String key, actualValue, valueTime;

            String[] requestData = request.split(",");
            switch (requestData[0]) {
                case Constants.PUT:
                    if (requestData.length != 3) {
                        Log.e(Constants.TAG, "[COMMUNICATION THREAD] Invalid request from client!");
                        return;
                    }
                    serverThread.put(requestData[1], requestData[2] + "," + time);
                    printWriter.println("OK");
                    printWriter.flush();
                    return;
                case Constants.GET:
                    if (requestData.length != 2) {
                        Log.e(Constants.TAG, "[COMMUNICATION THREAD] Invalid request from client!");
                        return;
                    }
                    key = requestData[1];
                    actualValue = serverThread.get(key);

                    if (actualValue == null) {
                        printWriter.println("NULL");
                        printWriter.flush();
                        return;
                    }

                    requestData = actualValue.split(",");
                    actualValue = requestData[0];
                    valueTime = requestData[1];

                    if (Integer.parseInt(time) > Integer.parseInt(valueTime) + 60) {
                        serverThread.put(key, null);
                        Log.e(Constants.TAG, "[COMMUNICATION THREAD] Value expired.");
                        printWriter.println("NULL");
                        printWriter.flush();
                        return;
                    }
                    printWriter.println(actualValue);
                    printWriter.flush();
                    return;
                default:
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Invalid operation.");
            }
        } catch (Exception ex) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }
}
