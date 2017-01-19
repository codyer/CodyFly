package com.cody.fly.core;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by cody.yi on 2017/1/19.
 * Action执行结果
 */
public class CoreActionResult {
    public static final int CODE_SUCCESS = 0x0000;
    public static final int CODE_ERROR = 0x0001;
    public static final int CODE_NOT_FOUND = 0X0002;
    public static final int CODE_INVALID = 0X0003;
    public static final int CODE_ROUTER_NOT_REGISTER = 0X0004;
    public static final int CODE_CANNOT_BIND_TARGET = 0X0005;
    public static final int CODE_REMOTE_EXCEPTION = 0X0005;
    public static final int CODE_CANNOT_BIND_WIDE = 0X0005;

    private int code;
    private String msg;
    private String data;
    private Object object;

    private CoreActionResult(Builder builder) {
        this.code = builder.mCode;
        this.msg = builder.mMsg;
        this.data = builder.mData;
        this.object = builder.mObject;
    }

    public Object getObject() {
        return object;
    }

    public String getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("code",code);
            jsonObject.put("msg",msg);
            jsonObject.put("data",data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public static class Builder {
        private int mCode;
        private String mMsg;
        private Object mObject;
        private String mData;

        public Builder() {
            mCode = CODE_ERROR;
            mMsg = "";
            mObject = null;
            mData = null;
        }

        public Builder resultString(String resultString) {
            CoreActionResult coreActionResult = new Gson().fromJson(resultString, CoreActionResult.class);
            this.mCode = coreActionResult.code;
            this.mMsg = coreActionResult.msg;
            this.mData = coreActionResult.data;
            return this;
        }

        public Builder code(int code) {
            this.mCode = code;
            return this;
        }

        public Builder msg(String msg) {
            this.mMsg = msg;
            return this;
        }

        public Builder data(String data) {
            this.mData = data;
            return this;
        }

        public Builder object(Object object) {
            this.mObject = object;
            return this;
        }

        public CoreActionResult build() {
            return new CoreActionResult(this);
        }
    }
}
