package com.cody.fly.core.router;

/**
 * Created by cody.yi on 2017/1/19.
 * 路由连接包裹类
 */
public class ConnectServiceWrapper {
    private boolean mAutoConnect = false;// 是否需要自动连接
    private Class<? extends LocalRouterConnectService> mServiceClass = null;

    public ConnectServiceWrapper(boolean autoConnect, Class<? extends LocalRouterConnectService> serviceClass) {
        this.mAutoConnect = autoConnect;
        this.mServiceClass = serviceClass;
    }

    public boolean isAutoConnect() {
        return mAutoConnect;
    }

    public Class<? extends LocalRouterConnectService> getServiceClass() {
        return mServiceClass;
    }
}
