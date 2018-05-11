package cn.edu.tsinghua.vpn4over6;

public class VPNBackend {
    public native int startThread();
    static{
        System.loadLibrary("backend_jni");
    }
}
