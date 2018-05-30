package cn.edu.tsinghua.vpn4over6;

import java.util.Timer;
import java.util.TimerTask;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.content.Intent;
import android.net.VpnService;
import android.os.Environment;
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

    public Timer mTimer = new Timer();
    private TextView ipv6Addr = findViewById(R.id.textView3);
    private TextView ipv4Addr = findViewById(R.id.textView5);
    private TextView uploadInfo = findViewById(R.id.textView8);
    private TextView downloadInfo = findViewById(R.id.textView10);
    private TextView runTime = findViewById(R.id.textView12);
    private FloatingActionButton mFab = findViewById(R.id.fab);
    private int running = 0; //服务是否已开启（1）的标志
    private int flag = 0; //决定读取ip管道信息（0）或读取流量管道信息（1）的标志
    private byte[] readBuf;
    private byte[] writeBuf;

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: //需要刷新界面
                    System.out.println("good "); //调试用输出
                    renewUI();
                    break;
                case 2:
                    mTimer.cancel();
                    mTimer=null;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VPNBackend vpnBackend = new VPNBackend();
        Log.i("MainActivity", "result: " + vpnBackend.startThread());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivi);

        Toolbar mToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

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
                    Snackbar.make(view, "服务已停止。", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            }
        });
    }

    public void timerTask() { //计时器执行的定时任务
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (flag == 0) {
                    int readFlag = readPipe();
                    if (readFlag > 0) {//这里需要修改，判断是否读到了ip地址
                        startVPN();
                        writePipe();//把虚接口描述符写入管道
                        flag = 1;
                    }
                    mHandler.sendEmptyMessage(1);
                } else if (flag == 1) {
                    readPipe();
                    //对读取到对流量信息做转换
                    //上联ipv6地址
                    //下联ipv4虚地址
                    //上传总包数
                    //下载总包数
                    //上传速率（两次总上传流量相减）
                    //下载速率（两次总下载流量相减）
                    mHandler.sendEmptyMessage(2); //需要刷新ui
                }
            }
        }, 1000, 1000);
    }

    public int readPipe() {
        //读取pipe中的信息
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream in = new BufferedInputStream(fileInputStream); 
        try {
            int readLen = in.read(readBuf); //读取管道
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return readLen;
        } catch (IOException e){
            e.printStackTrace();
        }
        return 0;
    }//??需要从哪里读

    public void writePipe() {
        File extDir = Environment.getExternalStorageDirectory(); //获取当前路径 
        File file = new File(extDir,"cmd_pipe");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);
            try {
                out.write(writeBuf, 0, writeBuf.length); //writeBuf是存放数据的byte类型数组
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//??需要写到哪里去

    public void renewUI() {
        //根据读取到到值进行转换
        ipv6Addr.setText("A.A.A.A.A.A.A.A");
        ipv4Addr.setText("127.0.0.1");
        uploadInfo.setText("1 M ↑ 2 MB/s");
        downloadInfo.setText("1 M ↓ 2 MB/s");
        runTime.setText("00:00:03");
    }

    public void startVPN() { //开启vpn服务
        //准备工作，弹窗供用户确认
        Intent intent = VpnService.prepare(this);
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
            startService(intent);
        }
    }

    @Override
    protected void onStop() {
        mTimer.cancel();
        super.onStop();
    }
}