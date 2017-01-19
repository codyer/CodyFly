package com.cody.fly.core.router;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.cody.fly.core.CoreActionResult;
import com.cody.fly.core.CoreApplication;
import com.cody.fly.core.DefaultAction;
import com.cody.fly.core.ILocalRouter;
import com.cody.fly.core.utils.ProcessUtil;

import java.util.HashMap;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by cody.yi on 2017/1/19.
 * 多进程间路由
 */
public class WideRouter {
    public static final String PROCESS_NAME = "com.cody.fly.wideRouter";
    private static final String TAG = "WideRouter";
    private static HashMap<String, ConnectServiceWrapper> sLocalRouterClasses;
    private static WideRouter sInstance = null;
    private CoreApplication mApplication;
    private HashMap<String, ILocalRouter> mLocalRouters;
    private HashMap<String, ServiceConnection> mLocalRouterConnections;


    private WideRouter(CoreApplication context) {
        mApplication = context;
        String processName = ProcessUtil.getProcessName(context, ProcessUtil.getMyProcessId());
        if (!PROCESS_NAME.equals(processName)) {
            throw new RuntimeException("You should not initialize the WideRouter in process:" + processName);
        }
        sLocalRouterClasses = new HashMap<>();
        mLocalRouters = new HashMap<>();
        mLocalRouterConnections = new HashMap<>();
    }

    public static synchronized WideRouter getInstance(@NonNull CoreApplication context) {
        if (sInstance == null) {
            sInstance = new WideRouter(context);
        }
        return sInstance;
    }

    public static void registerLocalRouter(String processName, Class<? extends LocalRouterConnectService> targetClass, boolean needAutoConnect) {
        if (null == sLocalRouterClasses) {
            sLocalRouterClasses = new HashMap<>();
        }
        ConnectServiceWrapper connectServiceWrapper = new ConnectServiceWrapper(needAutoConnect, targetClass);
        sLocalRouterClasses.put(processName, connectServiceWrapper);
    }

    protected boolean connectLocalRouter(final String domain) {
        Class<? extends LocalRouterConnectService> clazz = sLocalRouterClasses.get(domain).getServiceClass();
        if (null == clazz) {
            return false;
        }
        Intent binderIntent = new Intent(mApplication, clazz);
        Bundle bundle = new Bundle();
        binderIntent.putExtras(bundle);
        final ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ILocalRouter localRouter = ILocalRouter.Stub.asInterface(service);
                ILocalRouter temp = mLocalRouters.get(domain);
                if (null == temp) {
                    mLocalRouters.put(domain, localRouter);
                    mLocalRouterConnections.put(domain, this);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mLocalRouters.remove(domain);
                mLocalRouterConnections.remove(domain);
            }
        };
        mApplication.bindService(binderIntent, serviceConnection, BIND_AUTO_CREATE);
        return true;
    }

    boolean answerLocalAsync(@NonNull RouterRequest routerRequest) {
        ILocalRouter target = mLocalRouters.get(routerRequest.getDomain());
        if (target == null) {
            Class<? extends LocalRouterConnectService> clazz = sLocalRouterClasses.get(routerRequest.getDomain()).getServiceClass();
            if (null == clazz) {
                return false;
            } else {
                return true;
            }
        } else {
            try {
                return target.isAsync(routerRequest.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
                return true;
            }
        }
    }

    public RouterResponse route(RouterRequest routerRequest) {
        Log.e(TAG, PROCESS_NAME + ", start: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
        RouterResponse routerResponse = new RouterResponse();
        if (PROCESS_NAME.equals(routerRequest.getDomain())) {

        }
        ILocalRouter target = mLocalRouters.get(routerRequest.getDomain());
        if (null == target) {
            if (!connectLocalRouter(routerRequest.getDomain())) {
                DefaultAction defaultNotFoundAction = new DefaultAction(false, CoreActionResult.CODE_ROUTER_NOT_REGISTER, "The " + routerRequest.getDomain() + " has not registered.");
                CoreActionResult result = defaultNotFoundAction.invoke(mApplication, routerRequest.getData());
                routerResponse.mIsAsync = false;
                routerResponse.mResultString = result.toString();
                Log.e(TAG, PROCESS_NAME + ", no register end: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
                return routerResponse;
            } else {
                // Wait to bind the target process connect service, timeout is 30s.
                Log.e(TAG, PROCESS_NAME + ", bind start: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
                int time = 0;
                while (true) {
                    target = mLocalRouters.get(routerRequest.getDomain());
                    if (null == target) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        time++;
                    } else {
                        Log.e(TAG, PROCESS_NAME + ", bind end: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
                        break;
                    }
                    if (time >= 600) {
                        DefaultAction defaultNotFoundAction = new DefaultAction(true, CoreActionResult.CODE_CANNOT_BIND_TARGET, "Can not bind " + routerRequest.getDomain());
                        CoreActionResult result = defaultNotFoundAction.invoke(mApplication, routerRequest.getData());
                        routerResponse.mIsAsync = true;
                        routerResponse.mResultString = result.toString();
                        Log.e(TAG, PROCESS_NAME + ", time out end: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
                        return routerResponse;
                    }
                }
            }
        }
        try {
            String resultString = target.route(routerRequest.toString());
            routerResponse.mIsAsync = target.isAsync(routerRequest.toString());
            routerResponse.mResultString = resultString;
            Log.e(TAG, PROCESS_NAME + ", end: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
            DefaultAction defaultNotFoundAction = new DefaultAction(true, CoreActionResult.CODE_REMOTE_EXCEPTION, e.getMessage());
            CoreActionResult result = defaultNotFoundAction.invoke(mApplication, routerRequest.getData());
            routerResponse.mIsAsync = true;
            routerResponse.mResultString = result.toString();
            Log.e(TAG, PROCESS_NAME + ", error end: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
            return routerResponse;

        }
        return routerResponse;
    }

    boolean shutdownRouter(String domain) {
        if (TextUtils.isEmpty(domain)) {
            return false;
        }
        else if (PROCESS_NAME.equals(domain)) {
            //TODO
            return false;
        } else if (null == mLocalRouterConnections.get(domain)) {
            return false;
        } else {
            mApplication.unbindService(mLocalRouterConnections.get(domain));
            mLocalRouters.remove(domain);
            mLocalRouterConnections.remove(domain);
            return true;
        }
    }
}
