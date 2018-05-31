package cn.edu.tsinghua.vpn4over6;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;

import android.os.ParcelFileDescriptor;
import android.util.Log;


public class mVPNService extends VpnService{

    private static final String TAG = "mVPNService";

    private String ipv4addr, route, DNS1, DNS2, DNS3;
    private String mServerAddress = "127.0.0.1";
    private int mServerPort = 1080;
    private PendingIntent mConfigureIntent;

    private ParcelFileDescriptor mInterface;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ipv4addr = intent.getStringExtra("ipv4addr");
        route = intent.getStringExtra("route");
        DNS1 = intent.getStringExtra("DNS1");
        DNS2 = intent.getStringExtra("DNS2");
        DNS3 = intent.getStringExtra("DNS3");

        Log.i("mVPNService", "ipv4addr: " + ipv4addr);

        if (mInterface == null) {
            Builder builder = new Builder();

            builder.setMtu(1500);
            builder.addAddress(ipv4addr, 32);
            builder.addRoute(route, 0);
            builder.addDnsServer(DNS1);
            builder.addDnsServer(DNS2);
            builder.addDnsServer(DNS3);

            // Close the old interface since the parameters have been changed.
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }

            // Create a new interface using the builder and save the parameters.
            mInterface = builder.setSession("VPNServiceDemo")
                    .setConfigureIntent(mConfigureIntent).establish();
        }

        return START_STICKY;
    }

}
