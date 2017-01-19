package com.cody.fly.core.process;


import android.support.annotation.NonNull;

/**
 * Created by cody.yi on 2017/1/19.
 * 多模块生命周期优先级包裹类
 */
public class CoreModuleWrapper implements Comparable<CoreModuleWrapper> {

    private int mPriority = 0;
    private Class<? extends CoreModuleLifeCycle> mModuleClass = null;
    private CoreModuleLifeCycle mInstance;

    public CoreModuleWrapper(int priority, Class<? extends CoreModuleLifeCycle> moduleClass) {
        this.mPriority = priority;
        this.mModuleClass = moduleClass;
    }

    @Override
    public int compareTo(@NonNull CoreModuleWrapper o) {
        return this.mPriority - o.mPriority;
    }

    public int getPriority() {
        return mPriority;
    }

    public Class<? extends CoreModuleLifeCycle> getModuleClass() {
        return mModuleClass;
    }

    public CoreModuleLifeCycle getInstance() {
        return mInstance;
    }

    public void setInstance(CoreModuleLifeCycle mInstance) {
        this.mInstance = mInstance;
    }
}
