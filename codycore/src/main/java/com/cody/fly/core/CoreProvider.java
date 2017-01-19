package com.cody.fly.core;

import java.util.HashMap;

/**
 * Created by cody.yi on 2017/1/19.
 * 服务提供者
 */
public abstract class CoreProvider {
    private boolean mValid = true;
    private HashMap<String, CoreAction> mActions;

    public CoreProvider() {
        mActions = new HashMap<>();
        registerActions();
    }

    protected void registerAction(String actionName, CoreAction action) {
        mActions.put(actionName, action);
    }

    public CoreAction getAction(String actionName) {
        if (mActions != null) {
            return mActions.get(actionName);
        } else {
            return null;
        }
    }

    public boolean isValid() {
        return mValid;
    }

    protected abstract void registerActions();
}
