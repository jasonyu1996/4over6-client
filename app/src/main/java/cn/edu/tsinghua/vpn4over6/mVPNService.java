package cn.edu.tsinghua.vpn4over6;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;


public class mVPNService extends VpnService {
    private static final String TAG = "TsinghuaIVI";

    private String mServerAddress = "127.0.0.1";
    private int mServerPort = 5555;
    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }
        // Start a new session by creating a new thread.
        mThread = new Thread(this, "VpnThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    DatagramChannel mTunnel = null;
    //	FileOutputStream out;
    private boolean run(InetSocketAddress server) throws Exception {
        boolean connected = false;

        android.os.Debug.waitForDebugger();

        // Create a DatagramChannel as the VPN tunnel.
        mTunnel = DatagramChannel.open();

        // Protect the tunnel before connecting to avoid loopback.
        if (!protect(mTunnel.socket())) {
            throw new IllegalStateException("Cannot protect the tunnel");
        }

        // Connect to the server.
        mTunnel.connect(server);

        // For simplicity, we use the same thread for both reading and
        // writing. Here we put the tunnel into non-blocking mode.
        mTunnel.configureBlocking(false);

        // Authenticate and configure the virtual network interface.
        handshake();

        // Now we are connected. Set the flag and show the message.
        connected = true;
        Message msgObj = mHandler.obtainMessage();
        msgObj.obj = "Connected";
        mHandler.sendMessage(msgObj);


        new Thread() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    DatagramSocket socket = new DatagramSocket(mServerPort);
                    DatagramPacket packet = new DatagramPacket(new byte[255],
                            255);

                    while (true) {
                        try {
                            socket.receive(packet);// 阻塞
                            socket.send(packet);
                            packet.setLength(255);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }

        }.start();


        new Thread() {
            public void run() {
                // Packets to be sent are queued in this input stream.
                FileInputStream in = new FileInputStream(
                        mInterface.getFileDescriptor());
                // Allocate the buffer for a single packet.
                ByteBuffer packet = ByteBuffer.allocate(32767);
                int length;
                try {
                    while (true) {
                        length = in.read(packet.array());

                        if (length > 0) {
                            // while ((length = in.read(packet.array())) > 0) {
                            // Write the outgoing packet to the tunnel.
                            packet.limit(length);
                            debugPacket(packet); // Packet size, Protocol,
                            // source, destination
                            mTunnel.write(packet);
                            packet.clear();

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();

        return connected;
    }

    private void handshake() throws Exception {
        if (mInterface == null) {
            Builder builder = new Builder();

            builder.setMtu(1500);
            builder.addAddress("10.0.2.0", 32);
            builder.addRoute("0.0.0.0", 0);
            try {
                mInterface.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mInterface = builder.setSession("TsinghuaIVI")
                    .setConfigureIntent(mConfigureIntent).establish();
        }
    }
}
