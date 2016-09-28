package hinzehaley.com.sharedob.Connection;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * Created by haleyhinze on 9/28/16.
 * Creates Server
 */
public class BirthdayServer {
    ServerSocket mServerSocket = null;
    Thread mThread = null;
    String TAG = "birthdayServer";
    private BirthdayConnection connection;

    public BirthdayServer(BirthdayConnection connection) {
        mThread = new Thread(new ServerThread());
        this.connection = connection;
        mThread.start();
    }

    /**
     * Closes server socket
     */
    public void tearDown() {
        mThread.interrupt();
        try {
            mServerSocket.close();
        } catch (IOException ioe) {
            Log.e(TAG, "IOException when closing socket");
        }
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {

            try {
                //gets available port
                mServerSocket = new ServerSocket(0);
                connection.setLocalPort(mServerSocket.getLocalPort());

                while (!Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "server socket created");
                    try {
                        //gets connection with client
                        connection.setSocket(mServerSocket.accept());
                    }catch (SocketException e){
                        break;
                    }

                    //If a connection was obtained, gets the port and address then connects to the server
                    if (connection.mBirthdayClient == null) {
                        int port = connection.getSocket().getPort();
                        InetAddress address = connection.getSocket().getInetAddress();
                        connection.connectToServer(address, port);
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
            }
        }
    }
}
