package com.cody.fly.core.router;


import com.cody.fly.core.process.CoreModuleLifeCycle;

/**
 * Created by cody.yi on 2017/1/19.
 * 进程间路由模块
 */
public final class WideRouterModuleLifeCycle extends CoreModuleLifeCycle {
    @Override
    public void onCreate() {
        super.onCreate();
        initRouter();
    }

    private void initRouter() {
        WideRouter.getInstance(mApplication);
        mApplication.initializeAllProcessRouter();
    }
}
