package cn.edu.tsinghua.vpn4over6;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.net.VpnService;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
//import android.view.View.OnClickListener;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

public class IVI extends AppCompatActivity {

    private int count = 1; //计数器，调试定时器用
    private int running = 0; //服务是否已开启（1）的标志
    private int flag = 0; //决定读取ip管道信息（0）或读取流量管道信息（1）的标志

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    System.out.println("count: " + count); //调试用输出
                    break;
                case 2:
                    mTimer.cancel();
                    mTimer=null;
            }
            super.handleMessage(msg);
        }
    };

    public Timer mTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VPNBackend vpnBackend = new VPNBackend();
        Log.i("MainActivity", "result: " + vpnBackend.startThread());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivi);

        Toolbar mToolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        FloatingActionButton mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //点击悬浮按钮，开启或停止服务
                try {
                    if(running == 0) { //服务尚未开启，点击按钮开启服务
                        mTimer = new Timer();
                        timerTask();
                        running = 1;
                        //读管道
                        startVPN();//实际应当先读管道再开启
                        Snackbar.make(view, "服务已开启。", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                    else if(running == 1){ //服务已经开启，点击按钮停止服务
                        if (mTimer != null) {
                            mTimer.cancel();
                        }
                        running = 0;
                        Snackbar.make(view, "服务已停止。", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
                catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void timerTask() { //计时器执行的定时任务
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(count <= 50){
                    System.out.println("TimerTaskId: "
                            + Thread.currentThread().getId());
                    mHandler.sendEmptyMessage(1);
                }
                else{
                    mHandler.sendEmptyMessage(2); //超时退出，调试用
                }
                count++;
            }
        }, 1000, 1000);
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

    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Intent intent = new Intent(this, MyVpnService.class);
            startService(intent);
        }
    }

    @Override
    protected void onStop() {
        mTimer.cancel();
        super.onStop();
    }

}
