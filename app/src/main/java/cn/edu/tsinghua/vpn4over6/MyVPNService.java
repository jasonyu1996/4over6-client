package cn.edu.tsinghua.vpn4over6;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import java.util.Collection;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;


public class MyVpnService extends VpnService {
    @Override
    public IBinder onBind(Intent intent) {
        Builder builder = new Builder();
        builder.setMtu(1024); //需要修改
        //1）MTU（Maximun Transmission Unit），即表示虚拟网络端口的最大传输单元，如果发送的包长度超过这个数字，则会被分包；
        builder.addAddress("0.0.0.0/0", 1); //需要修改
        //2）Address，即这个虚拟网络端口的IP地址；
        builder.addRoute("0.0.0.0/0", 1); //需要修改
        //3）Route，只有匹配上的IP包，才会被路由到虚拟端口上去。如果是0.0.0.0/0的话，则会将所有的IP包都路由到虚拟端口上去；
        builder.addDnsServer("0.0.0.0/0"); //需要修改
        //4）DNS Server，就是该端口的DNS服务器地址；
        builder.addSearchDomain("0.0.0.0/0"); //需要修改
        //5）Search Domain，就是添加DNS域名的自动补齐。DNS服务器必须通过全域名进行搜索，但每次查找都输入全域名太麻烦了，可以通过配置域名的自动补齐规则予以简化;
        builder.setSession("0.0.0.0/0"); //需要修改
        //6）Session，就是你要建立的VPN连接的名字，它将会在系统管理的与VPN连接相关的通知栏和对话框中显示出来

        // builder.setConfigureIntent();
        //7）Configure Intent，这个intent指向一个配置页面，用来配置VPN链接。它不是必须的，如果没设置的话，则系统弹出的VPN相关对话框中不会出现配置按钮。

        ParcelFileDescriptor mInterface = builder.establish();
        //最后调用Builder.establish函数，如果一切正常的话，tun0虚拟网络接口就建立完成了。并且，同时还会通过iptables命令，修改NAT表，将所有数据转发到tun0接口上。

        //这之后，就可以通过读写VpnService.Builder返回的ParcelFileDescriptor实例来获得设备上所有向外发送的IP数据包和返回处理过后的IP数据包到TCP/IP协议栈：

        //这里还需要引入几个库文件！
        // Packets to be sent are queued in this input stream.
        FileInputStream in = new FileInputStream(interface.getFileDescriptor());
        // Packets received need to be written to this output stream.
        FileOutputStream out = new FileOutputStream(interface.getFileDescriptor());
        // Allocate the buffer for a single packet.
        ByteBuffer packet = ByteBuffer.allocate(32767);
        ...
        // Read packets sending to this interface
        int length = in.read(packet.array());
        ...
        // Write response packets back
        out.write(packet.array(), 0, length);

        //protect(my_socket);
        //VpnService类提供了一个叫protect的函数，在VPN程序自己建立socket之后，必须要对其进行保护：

        throw new UnsupportedOperationException("Not yet implemented");
    }

}
