package cn.edu.tsinghua.vpn4over6;

public class VPNBackend {
    public native String startThread();
    static{
        System.loadLibrary("backend_jni");
    }
}
