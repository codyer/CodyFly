package com.cody.fly.core;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cody.fly.core.process.CoreModuleLifeCycle;
import com.cody.fly.core.process.CoreModuleWrapper;
import com.cody.fly.core.router.LocalRouter;
import com.cody.fly.core.router.WideRouter;
import com.cody.fly.core.router.WideRouterConnectService;
import com.cody.fly.core.router.WideRouterModuleLifeCycle;
import com.cody.fly.core.utils.ProcessUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by cody.yi on 2017/1/19.
 * 多进程架构封装的Base Application
 */
public abstract class CoreApplication extends Application {
    private ArrayList<CoreModuleWrapper> mModuleWrapperList;// 模块列表
    private HashMap<String, ArrayList<CoreModuleWrapper>> mProcessWrapperMap;// 进程map
    public static LocalRouter sLocalRouter;

    @CallSuper
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("application", "start" + System.currentTimeMillis());
        init();
        startWideRouter();
        registerAllProcesses();
        dispatchLogic();
        instantiationLogic();
        if (null != mModuleWrapperList && mModuleWrapperList.size() > 0) {
            for (CoreModuleWrapper moduleWrapper : mModuleWrapperList) {
                if (null != moduleWrapper && null != moduleWrapper.getInstance()) {
                    moduleWrapper.getInstance().onCreate();
                    Log.i("application", "onCreate:" + moduleWrapper.getModuleClass().getName());
                }
            }
        }
        Log.e("application", "end" + System.currentTimeMillis());
    }

    private void init() {
        mProcessWrapperMap = new HashMap<>();
        sLocalRouter = LocalRouter.getInstance(this);
    }

    protected void startWideRouter() {
        if (needMultipleProcess()) {
            Log.i("application", "startWideRouter:" + System.currentTimeMillis());
            registerProcess(WideRouter.PROCESS_NAME, 1000, WideRouterModuleLifeCycle.class);
            Intent intent = new Intent(this, WideRouterConnectService.class);
            startService(intent);
        }
    }

    /**
     * 初始化所有的进程内路由到进程间路由表
     */
    public abstract void initializeAllProcessRouter();

    /**
     * 注册所有的进程
     */
    protected abstract void registerAllProcesses();

    /**
     * 是否需要多进程
     *
     * @return true 是
     */
    public abstract boolean needMultipleProcess();

    /**
     * 注册进程
     *
     * @param processName  进程名
     * @param priority     优先级
     * @param processClass class
     * @return 结果
     */
    protected boolean registerProcess(String processName, int priority, @NonNull Class<? extends CoreModuleLifeCycle> processClass) {
        boolean result = false;
        if (null != mProcessWrapperMap) {
            ArrayList<CoreModuleWrapper> processWrappers = mProcessWrapperMap.get(processName);
            if (null == processWrappers) {
                processWrappers = new ArrayList<>();
                mProcessWrapperMap.put(processName, processWrappers);
            }
            if (processWrappers.size() > 0) {
                for (CoreModuleWrapper coreModuleWrapper : processWrappers) {
                    if (processClass.getName().equals(coreModuleWrapper.getModuleClass().getName())) {
                        throw new RuntimeException(processClass.getName() + " has registered.");
                    }
                }
            }
            CoreModuleWrapper coreModuleWrapper = new CoreModuleWrapper(priority, processClass);
            processWrappers.add(coreModuleWrapper);
            result = true;
        }
        return result;
    }

    private void dispatchLogic() {
        if (null != mProcessWrapperMap) {
            mModuleWrapperList = mProcessWrapperMap.get(ProcessUtil.getProcessName(this, ProcessUtil.getMyProcessId()));
        }
    }

    private void instantiationLogic() {
        if (null != mModuleWrapperList && mModuleWrapperList.size() > 0) {
            Collections.sort(mModuleWrapperList);
            for (CoreModuleWrapper coreModuleWrapper : mModuleWrapperList) {
                if (null != coreModuleWrapper) {
                    try {
                        coreModuleWrapper.setInstance(coreModuleWrapper.getModuleClass().newInstance());
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if (null != coreModuleWrapper.getInstance()) {
                        coreModuleWrapper.getInstance().setApplication(this);
                    }
                }
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (null != mModuleWrapperList && mModuleWrapperList.size() > 0) {
            for (CoreModuleWrapper coreModuleWrapper : mModuleWrapperList) {
                if (null != coreModuleWrapper && null != coreModuleWrapper.getInstance()) {
                    coreModuleWrapper.getInstance().onTerminate();
                }
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (null != mModuleWrapperList && mModuleWrapperList.size() > 0) {
            for (CoreModuleWrapper coreModuleWrapper : mModuleWrapperList) {
                if (null != coreModuleWrapper && null != coreModuleWrapper.getInstance()) {
                    coreModuleWrapper.getInstance().onLowMemory();
                }
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (null != mModuleWrapperList && mModuleWrapperList.size() > 0) {
            for (CoreModuleWrapper coreModuleWrapper : mModuleWrapperList) {
                if (null != coreModuleWrapper && null != coreModuleWrapper.getInstance()) {
                    coreModuleWrapper.getInstance().onTrimMemory(level);
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (null != mModuleWrapperList && mModuleWrapperList.size() > 0) {
            for (CoreModuleWrapper coreModuleWrapper : mModuleWrapperList) {
                if (null != coreModuleWrapper && null != coreModuleWrapper.getInstance()) {
                    coreModuleWrapper.getInstance().onConfigurationChanged(newConfig);
                }
            }
        }
    }

}
