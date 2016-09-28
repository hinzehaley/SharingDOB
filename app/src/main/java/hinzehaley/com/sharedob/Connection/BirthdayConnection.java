/*
Used sample code from
https://android.googlesource.com/platform/development/+/master/samples/training/NsdChat/src/com/example/android/nsdchat
 */

package hinzehaley.com.sharedob.Connection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import hinzehaley.com.sharedob.Constants;
import hinzehaley.com.sharedob.NsdListener;

/**
 * Establishes a connection between two devices using sockets
 */
public class BirthdayConnection {

    private Handler mUpdateHandler;
    private BirthdayServer mBirthdayServer;
    protected BirthdayClient mBirthdayClient;
    private NsdListener nsdListener;
    private Socket mSocket;
    private int mPort = -1;

    public BirthdayConnection(Handler handler, NsdListener nsdListener) {
        mUpdateHandler = handler;
        this.nsdListener = nsdListener;
        mBirthdayServer = new BirthdayServer(this);
    }

    /**
     * Stops threads
     */
    public void tearDown() {
        mBirthdayServer.tearDown();
        if (mBirthdayClient != null) {
          mBirthdayClient.tearDown();
        }
    }

    /**
     * Connects socket to given address and port to create client
     * @param address
     * @param port
     */
    public void connectToServer(InetAddress address, int port) {
        mBirthdayClient = new BirthdayClient(address, port, this, nsdListener);

    }

    /**
     * Sends birthday from client
     * @param birthday
     */
    public void sendBirthday(String birthday) {
        if (mBirthdayClient != null) {
            mBirthdayClient.sendBirthday(birthday);
        }
    }

    public int getLocalPort() {
        return mPort;
    }

    public void setLocalPort(int port) {
        mPort = port;
    }

    /**
     * updates the birthday by sending the new birthday to the handler,
     * which updates the UI to display the birthday to the user
     *
     * @param birthday
     * @param local
     */
    public synchronized void updateBirthday(String birthday, boolean local) {

        Bundle messageBundle = new Bundle();
        messageBundle.putString(Constants.BIRTHDAY_KEY, birthday);
        messageBundle.putBoolean(Constants.IS_SENDER_KEY, local);

        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);

    }

    /**
     * If a socket is already connected, closes it. Sets a new socket
     * @param socket
     */
    protected synchronized void setSocket(Socket socket) {
        if (socket == null) {
        }
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mSocket = socket;
    }

    public Socket getSocket() {
        return mSocket;
    }

    public BirthdayServer getmBirthdayServer() {
        return mBirthdayServer;
    }

    public BirthdayClient getmBirthdayClient() {
        return mBirthdayClient;
    }
}
