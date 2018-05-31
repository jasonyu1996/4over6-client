package cn.edu.tsinghua.vpn4over6;

import android.content.Intent;
import android.net.VpnService;

import android.os.ParcelFileDescriptor;
import android.util.Log;


public class mVPNService extends VpnService{

    private static final String TAG = "mVPNService";

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

            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }

            mInterface = builder.establish();
        }

        return START_STICKY;
    }

}
