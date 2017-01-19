package com.cody.fly.core;

import android.content.Context;

import java.util.HashMap;

/**
 * Created by cody.yi on 2017/1/19.
 * 默认未找到Action
 */
public class DefaultAction extends CoreAction {

    private static final String DEFAULT_MESSAGE = "Something was really wrong. Ha ha!";
    private int mCode;
    private String mMessage;
    private boolean mAsync;

    public DefaultAction() {
        mCode = CoreActionResult.CODE_ERROR;
        mMessage = DEFAULT_MESSAGE;
        mAsync = false;
    }

    public DefaultAction(boolean isAsync, int code, String message) {
        this.mCode = code;
        this.mMessage = message;
        this.mAsync = isAsync;
    }

    @Override
    public boolean isAsync(Context context, HashMap<String, String> requestData) {
        return mAsync;
    }

    @Override
    public CoreActionResult invoke(Context context, HashMap<String, String> requestData) {
        return new CoreActionResult.Builder()
                .code(mCode)
                .msg(mMessage)
                .data(null)
                .object(null)
                .build();
    }

}
