package com.cody.fly.core.process;

import android.content.res.Configuration;
import android.support.annotation.NonNull;

import com.cody.fly.core.CoreApplication;

/**
 * Created by cody.yi on 2017/1/19.
 * 添加一个模块时，原来需要在Application生命周期中实现的逻辑通过继承此类实现
 * 各个模块需要在Application中的生命周期做的操作可以在CoreModuleLifeCycle的子类中做。
 * 然后在主模块中将LifeCycle注册 *
 * protected void initializeLogic() {
 * registerApplicationLogic("com.cody.fly.core:demo",999, MainApplicationLogic.class);
 * }
 */
public class CoreModuleLifeCycle {
    protected CoreApplication mApplication;

    public CoreModuleLifeCycle() {
    }

    public void setApplication(@NonNull CoreApplication application) {
        mApplication = application;
    }

    public void onCreate() {
    }

    public void onTerminate() {
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }
}
