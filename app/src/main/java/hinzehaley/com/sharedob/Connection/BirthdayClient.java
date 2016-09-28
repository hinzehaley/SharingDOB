package hinzehaley.com.sharedob.Connection;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import hinzehaley.com.sharedob.NsdListener;

/**
 * Created by haleyhinze on 9/28/16.
 * Creates a client that connects to available server
 */
public class BirthdayClient {

    private InetAddress mAddress;
    private int PORT;
    private final String CLIENT_TAG = "BirthdayClient";
    private BirthdayConnection connection;
    private NsdListener nsdListener;

    public BirthdayClient(InetAddress address, int port, BirthdayConnection connection, NsdListener nsdListener) {

        this.mAddress = address;
        this.PORT = port;
        this.connection = connection;
        this.nsdListener = nsdListener;

        Thread mSendThread = new Thread(new SendingThread());
        mSendThread.start();
    }

    /**
     * Thread that sends data to server
     */
    class SendingThread implements Runnable {

        BlockingQueue<String> mMessageQueue;
        private int QUEUE_CAPACITY = 10;

        public SendingThread() {
            mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
        }

        @Override
        public void run() {
            try {
                if (connection.getSocket() == null) {
                    connection.setSocket(new Socket(mAddress, PORT));
                    Log.d(CLIENT_TAG, "Client-side socket initialized.");
                }
                //Creates thread to receive message
                Thread mRecThread = new Thread(new ReceivingThread());
                mRecThread.start();
            } catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Initializing socket failed, UnknownHostException", e);
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "Initializing socket failed, IOException.", e);
            }

            while (true) {
                try {
                    //gets next message and sends it
                    String msg = mMessageQueue.take();
                    sendBirthday(msg);
                } catch (InterruptedException ie) {
                    Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                }
            }
        }
    }

    /**
     * Thread to receive message from server socket
     */
    class ReceivingThread implements Runnable {

        @Override
        public void run() {
            BufferedReader input;
            try {
                InputStream inputStream = connection.getSocket().getInputStream();
                input = new BufferedReader(new InputStreamReader(
                        inputStream));
                while (!Thread.currentThread().isInterrupted()) {
                    if(nsdListener != null){
                        nsdListener.isConnected();
                    }

                    String messageStr = null;
                    messageStr = input.readLine();
                    if (messageStr != null) {
                        connection.updateBirthday(messageStr, false);
                    } else {
                        break;
                    }
                }
                input.close();
            } catch (IOException e) {
                Log.e(CLIENT_TAG, "Server error: ", e);
            }
        }
    }

    /**
     * Closes server socket
     */
    public void tearDown() {
        try {
            connection.getSocket().close();
        } catch (IOException ioe) {
            Log.e(CLIENT_TAG, "Error when closing server socket.");
        } catch(NullPointerException e){
            Log.e(CLIENT_TAG, "server socket is null");

        }
    }

    /**
     * Updates the sent birthday
     * @param birthday
     */
    public void sendBirthday(String birthday) {
        try {
            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(connection.getSocket().getOutputStream())), true);
            out.println(birthday);
            out.flush();
            connection.updateBirthday(birthday, true);
        } catch (UnknownHostException e) {
            Log.d(CLIENT_TAG, "Unknown Host", e);
        } catch (IOException e) {
            Log.d(CLIENT_TAG, "I/O Exception", e);
        } catch (Exception e) {
            Log.d(CLIENT_TAG, "Error3", e);
        }
        Log.d(CLIENT_TAG, "Client sent birthday: " + birthday);
    }
}
