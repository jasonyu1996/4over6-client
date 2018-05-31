package cn.edu.tsinghua.vpn4over6;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;

import android.os.ParcelFileDescriptor;
import android.util.Log;


public class mVPNService extends VpnService{

    private static final String TAG = "mVPNService";
//    private String ipv4addr = "13.8.0.2";
//    private String route = "0.0.0.0";
//    private String DNS1 = "59.66.16.64";
//    private String DNS2 = "8.8.8.8";
//    private String DNS3 = "202.106.0.20";
    //private PendingIntent mConfigureIntent;

    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String data = intent.getStringExtra("data");
        String[] dataArray = data.split(";");
        String ipv4addr = dataArray[0];
        String route = dataArray[1];
        String DNS1 = dataArray[2];
        String DNS2 = dataArray[3];
        String DNS3 = dataArray[4];

        if (mInterface == null) {
            Builder builder = new Builder();

            builder.setMtu(1500);
            builder.addAddress(ipv4addr, 32);
            builder.addRoute(route, 0);
            builder.addDnsServer(DNS1);
            builder.addDnsServer(DNS2);
            builder.addDnsServer(DNS3);
            builder.setSession("mVPNService");

            // Close the old interface since the parameters have been changed.
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }

            // Create a new interface using the builder and save the parameters.
            mInterface = builder.establish();
        }

        return START_STICKY;
    }

}
