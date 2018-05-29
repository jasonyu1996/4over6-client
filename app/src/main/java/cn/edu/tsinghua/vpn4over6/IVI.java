package cn.edu.tsinghua.vpn4over6;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class IVI extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VPNBackend vpnBackend = new VPNBackend();
        Log.i("MainActivity", "result: " + vpnBackend.startThread());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

}
