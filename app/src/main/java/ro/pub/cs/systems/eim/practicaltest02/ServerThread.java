package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerThread extends Thread {
    private final int port;
    private final ServerSocket serverSocket;
    private final Map<String, String> data;

    public ServerThread(int port) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
            ioException.printStackTrace();
        }

        this.port = port;
        this.serverSocket = serverSocket;
        this.data = new HashMap<>();
    }

    public int getPort() {
        return port;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public synchronized void put(String key, String value) {
        if (value == null)
            data.remove(key);
        else
            data.put(key, value);
    }

    public synchronized String get(String key) {
        return data.get(key);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Log.i(Constants.TAG, "[SERVER THREAD] Waiting for a client invocation...");
                Socket socket = serverSocket.accept();
                Log.i(Constants.TAG, "[SERVER THREAD] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());
                CommunicationThread communicationThread = new CommunicationThread(this, socket);
                communicationThread.start();
            }
        } catch (Exception ex) {
            Log.e(Constants.TAG, "[SERVER THREAD] An exception has occurred: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void stopThread() {
        interrupt();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[SERVER THREAD] An exception has occurred: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }
}