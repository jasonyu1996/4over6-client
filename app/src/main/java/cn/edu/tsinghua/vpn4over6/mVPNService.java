package cn.edu.tsinghua.vpn4over6;

import android.content.Intent;
import android.net.VpnService;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


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
            int interfaceFd = mInterface.detachFd();
            Log.i("MainActivity", "detachFd : " + interfaceFd);
            protect(interfaceFd);

            FileOutputStream fileOutputStream;
            try{
                fileOutputStream =
                        new FileOutputStream("/data/data/"+
                        "cn.edu.tsinghua.vpn4over6"+
                        "/vpn4over6_pipe_in");
                byte[] b = new byte[4];
                for (int i = 0; i < 4; i++) {
                    b[i] = (byte) (interfaceFd >> (24 - i * 8));
                }
                try {
                    fileOutputStream.write(b, 0, b.length);
                    fileOutputStream.flush();
                } catch (IOException e){
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e){
                e.printStackTrace();
            }
        }

        return START_STICKY;
    }

}
