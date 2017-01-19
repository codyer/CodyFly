package com.cody.fly.core;

import android.content.Context;

import java.util.HashMap;

/**
 * Created by cody.yi on 2017/1/19.
 * 每个provider提供的action
 */
public abstract class CoreAction {
    public abstract boolean isAsync(Context context, HashMap<String,String> requestData);
    public abstract CoreActionResult invoke(Context context, HashMap<String,String> requestData);
}
