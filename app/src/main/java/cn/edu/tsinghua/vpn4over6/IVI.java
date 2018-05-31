package cn.edu.tsinghua.vpn4over6;

import java.io.FileDescriptor;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.content.ComponentName;
import android.content.Intent;
import android.net.VpnService;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
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

    private String ipv4addr, route, DNS1, DNS2, DNS3;
    private String ipv6addr;

    private byte[] readBuf;
    private byte[] writeBuf;
    private int[] intBuf;
    private long[] networkData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        VPNBackend vpnBackend = new VPNBackend();
        Log.i("MainActivity", "result: " + vpnBackend.startThread());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivi);

        Toolbar mToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        FloatingActionButton mFab = findViewById(R.id.fab);
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
                        if (mTimer != null) {
                            mTimer.cancel();
                        }
                        running = 0;
                        msecond = 0;
                        mminute = 0;
                        mhour = 0;
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
        textView8.setText("0 M ↑ 0 MB/s");
        textView10.setText("0 M ↓ 0 MB/s");
        textView12.setText("00:00:00");

    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i("MainActivity", "msg: " + msg.what);
            if (msg.what == 1) {
                textView3.setText(ipv6addr);
                textView5.setText(ipv4addr);
                textView8.setText("1 M ↑ 2 MB/s");
                textView10.setText("1 M ↓ 2 MB/s");
                textView12.setText(String.format("%02d", mhour)+":"+
                        String.format("%02d", mminute)+":"+
                        String.format("%02d", msecond));
            } else if (msg.what == 2) {
                for(int i = 0; i < 20; i++) {
                    intBuf[i] = readBuf[i];
                    if (intBuf[i] < 0)
                        intBuf[i] += 256;
                }
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
            } else if (msg.what == 3) {
                for(int i = 0; i < 32; i++) {
                    intBuf[i] = readBuf[i];
                    if (intBuf[i] < 0)
                        intBuf[i] += 256;
//                    Log.i("MainActivity", "intBufi : " + intBuf[i]);
                }
                int rank = 0;
                long sum = 0;
                for(int i = 0; i < 4; i++){
                    sum = 0;
                    for(int j = 0; j < 8; j++) {
                        rank = j + i * 8;
                        sum = sum + intBuf[rank];
                        sum = sum * 256;
                    }
                    networkData[i] = sum;
                }
                Log.i("MainActivity", "data1 : " + networkData[0]);
                Log.i("MainActivity", "data2 : " + networkData[1]);
                Log.i("MainActivity", "data3 : " + networkData[2]);
                Log.i("MainActivity", "data4 : " + networkData[3]);
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
                    int readFlag = readPipe();
                    Log.i("MainActivity", "len = "+readFlag);
                    if (readFlag > 0) {//这里需要修改，判断是否读到了ip地址
                        mHandler.sendEmptyMessage(2);
                        startVPN();
                        //把虚接口描述符写入管道
                        writePipe();
                        flag = 1;
                    }
                } else if (flag == 1) {
                    readPipe();
                    mHandler.sendEmptyMessage(3);
                    //对读取到对流量信息做转换
                    //上联ipv6地址
                    //下联ipv4虚地址
                    //上传总包数
                    //下载总包数
                    //上传速率（两次总上传流量相减）
                    //下载速率（两次总下载流量相减）
                }
                mHandler.sendEmptyMessage(1); //需要刷新ui
                Log.i("MainActivity", "flag = "+flag);
            }
        }, 1000, 1000);
    }

    public int readPipe() {
        try {
            byte[] buffer = new byte[32];
            int readLen = fileInputStream.read(buffer); //读取管道
            readBuf = buffer;
            Log.i("MainActivity", "read : " + readBuf);
            return readLen;
        } catch (IOException e){
            e.printStackTrace();
        }
        return 0;
    }

    public int writePipe() {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (1 >> (24 - i * 8));
        }
        try {
            fileOutputStream.write(b, 0, b.length); //读取管道
            writeBuf = b;
            fileOutputStream.flush();
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
            startService(intent);
        }
    }

    @Override
    protected void onStop() {
        mTimer.cancel();
        super.onStop();
    }
}