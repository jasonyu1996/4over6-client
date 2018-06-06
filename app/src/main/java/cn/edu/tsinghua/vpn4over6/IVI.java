package cn.edu.tsinghua.vpn4over6;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;


public class IVI extends AppCompatActivity {

    private static final String TAG = "IVI";

    public Timer mTimer = new Timer();
    private TextView textView3, textView5, textView8, textView10, textView12;

    FileInputStream fileInputStream;
    FileOutputStream fileOutputStream;

    private int running = 0; //服务是否已开启（1）的标志
    private int flag = 0; //决定读取ip管道信息（0）或读取流量管道信息（1）的标志
    private int mhour = 0;
    private int mminute = 0;
    private int msecond = 0;
    private long packSentNum = 0;
    private long packRecvNum = 0;
    private double packSentSpeed = 0;
    private double packRecvSpeed = 0;
    private double packSentSize = 0;
    private double packRecvSize = 0;
    private int socketFd;

    private String ipv4addr, route, DNS1, DNS2, DNS3;
    private String ipv6addr;

    private byte[] readBuf;
    private int[] intBuf;
    private long[] networkData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        VPNBackend vpnBackend = new VPNBackend();
        Log.i("MainActivity", "result: " + (socketFd = vpnBackend.startThread()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivi);

        Toolbar mToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        final FloatingActionButton mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //点击悬浮按钮，开启或停止服务
                try {
                    if (running == 0) { //服务尚未开启，点击按钮开启服务
                        mTimer = new Timer();
                        timerTask();
                        running = 1;
                        Snackbar.make(view, "服务已开启。", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else if (running == 1) { //服务已经开启，点击按钮停止服务
                        running = 0;
                        msecond = 0;
                        mminute = 0;
                        mhour = 0;
                        if (mTimer != null) {
                            mTimer.cancel();
                        }
                        Snackbar.make(view, "服务已停止。", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            fileInputStream = new FileInputStream("/data/data/"+
                    "cn.edu.tsinghua.vpn4over6"+
                    "/vpn4over6_pipe_out");
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }
        try {
            fileOutputStream = new FileOutputStream("/data/data/"+
                    "cn.edu.tsinghua.vpn4over6"+
                    "/vpn4over6_pipe_in");
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }

        readBuf = new byte[32];
        intBuf = new int[32];
        networkData = new long[4];

        textView3 = (TextView) findViewById(R.id.textView3);
        textView5 = (TextView) findViewById(R.id.textView5);
        textView8 = (TextView) findViewById(R.id.textView8);
        textView10 = (TextView) findViewById(R.id.textView10);
        textView12 = (TextView) findViewById(R.id.textView12);
        textView3.setText("尚未连接");
        textView5.setText("尚未连接");
        textView8.setText("-");
        textView10.setText("-");
        textView12.setText("00:00:00");


    }

    public String transformDataSize(double dataSize) {
        String tmpResult = "";
        if (dataSize > 500) {
            if (dataSize > 500000) {
                if (dataSize > 500000000) {
                    tmpResult = String.format("%.2f", dataSize / 1000000000);
                    tmpResult += " GB";
                } else {
                    tmpResult = String.format("%.2f", dataSize / 1000000);
                    tmpResult += " MB";
                }
            } else {
                tmpResult = String.format("%.2f", dataSize / 1000);
                tmpResult += " KB";
            }
        } else {
            tmpResult = String.format("%.0f", dataSize);
            tmpResult += " B";
        }
        return tmpResult;
    }
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i("MainActivity", "msg: " + msg.what);
            if (msg.what == 1) {
                textView5.setText(ipv4addr);
                String sentSize = transformDataSize(packSentSize);
                String sentSpeed = transformDataSize(packSentSpeed);
                String recvSize = transformDataSize(packRecvSize);
                String recvSpeed = transformDataSize(packRecvSpeed);
                textView8.setText(packSentNum + " Packets ↑ "
                        + sentSize +" ↑ "
                        + sentSpeed +"/s");
                textView10.setText(packRecvNum + " Packets ↓ "
                        + recvSize +" ↓ "
                        + recvSpeed +"/s");
                textView12.setText(String.format("%02d", mhour)+":"+
                        String.format("%02d", mminute)+":"+
                        String.format("%02d", msecond));
            } else if (msg.what == 2) {
                int sum = 0;
                for (int i = 0; i < 20; i++) {
                    intBuf[i] = readBuf[i];
                    if (intBuf[i] < 0)
                        intBuf[i] += 256;
                    sum += intBuf[i];
                }
                if (sum > 0) {
                    ipv4addr = intBuf[0] + "." +
                            intBuf[1] + "." +
                            intBuf[2] + "." +
                            intBuf[3];
                    route = intBuf[4] + "." +
                            intBuf[5] + "." +
                            intBuf[6] + "." +
                            intBuf[7];
                    DNS1 = intBuf[8] + "." +
                            intBuf[9] + "." +
                            intBuf[10] + "." +
                            intBuf[11];
                    DNS2 = intBuf[12] + "." +
                            intBuf[13] + "." +
                            intBuf[14] + "." +
                            intBuf[15];
                    DNS3 = intBuf[16] + "." +
                            intBuf[17] + "." +
                            intBuf[18] + "." +
                            intBuf[19];
                }
                if (ipv4addr != null) {
                    Log.i("IVI", "good");
                    startVPN();
                    ipv6addr = getLocalHostIp();
                    textView3.setText(ipv6addr);
                    flag = 1;
                }
            } else if (msg.what == 3) {
                for(int i = 0; i < 32; i++) {
                    intBuf[i] = readBuf[i];
                    if (intBuf[i] < 0)
                        intBuf[i] += 256;
                }
                int rank = 0;
                long sum = 0;
                for(int i = 0; i < 4; i++){
                    sum = 0;
                    for(int j = 0; j < 8; j++) {
                        rank = 32 - j - i * 8 - 1;
                        sum = (sum << 8) + intBuf[rank];
                    }
                    networkData[i] = sum;
                }
                packSentNum = networkData[2];
                packRecvNum = networkData[0];
                packSentSpeed = ((double) networkData[3] -  packSentSize);
                packRecvSpeed = ((double) networkData[1] -  packRecvSize);
                packSentSize = (double) networkData[3];
                packRecvSize = (double) networkData[1];
                Log.i("MainActivity", "data0 : " + networkData[0]);
                Log.i("MainActivity", "data1 : " + networkData[1]);
                Log.i("MainActivity", "data2 : " + networkData[2]);
                Log.i("MainActivity", "data3 : " + networkData[3]);
            }
            super.handleMessage(msg);
        }
    };

    public void timerTask() { //计时器执行的定时任务
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                msecond ++;
                if (msecond == 60) {
                    msecond = 0;
                    mminute ++;
                    if (mminute == 60) {
                        mminute = 0;
                        mhour ++;
                    }
                }
                if (flag == 0) {
                    int readFlag = readPipe(20);
                    Log.i("MainActivity", "len = "+readFlag);
                    if (readFlag == 20) {
                        mHandler.sendEmptyMessage(2);
                        Log.i("IVI", "bad");
                    }
                } else if (flag == 1) {
                    readPipe(32);
                    mHandler.sendEmptyMessage(3);
                }
                mHandler.sendEmptyMessage(1); //需要刷新ui
                Log.i("MainActivity", "flag = "+flag);
            }
        }, 1000, 1000);
    }

    public int readPipe(int len) {
        try {
            byte[] buffer = new byte[32];
            int readLen = fileInputStream.read(buffer, 0, len); //读取管道
            readBuf = buffer;
            Log.i("MainActivity", "read : " + readBuf + " " + readLen);
            return readLen;
        } catch (IOException e){
            e.printStackTrace();
        }
        return 0;
    }

    public void startVPN() {
        Intent intent = VpnService.prepare(getApplicationContext());
        if (intent != null) {
            startActivityForResult(intent, 0);
        }
        else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, mVPNService.class);
            intent.putExtra("data", ipv4addr+";"+
                    route+";"+ DNS1+";"+
                    DNS2+";"+ DNS3);
            intent.putExtra("protectFd", socketFd);
            Log.i("vpn recv: " , ipv4addr+";"+
                    route+";"+ DNS1+";"+
                    DNS2+";"+ DNS3);
            startService(intent);
        }
    }

    public String getLocalHostIp() {
        String ipaddress = "";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
            // 遍历所用的网络接口
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();// 得到每一个网络接口绑定的所有ip
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                // 遍历每一个接口绑定的所有ip
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("IVI", "获取本地ip地址失败");
            e.printStackTrace();
        }
        return ipaddress;

    }

    @Override
    protected void onStop() {
        mTimer.cancel();
        super.onStop();
    }
}