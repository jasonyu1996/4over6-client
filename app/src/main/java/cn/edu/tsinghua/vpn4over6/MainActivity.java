package cn.edu.tsinghua.vpn4over6;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VPNBackend vpnBackend = new VPNBackend();
        Log.i("MainActivity", vpnBackend.startThread());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
