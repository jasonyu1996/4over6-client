package cn.edu.tsinghua.vpn4over6;

public class VPNBackend {
    public native int startThread();
    public native int endThread();
    static{
        System.loadLibrary("backend_jni");
    }
}
