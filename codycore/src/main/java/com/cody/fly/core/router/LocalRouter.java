package com.cody.fly.core.router;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cody.fly.core.CoreAction;
import com.cody.fly.core.CoreActionResult;
import com.cody.fly.core.CoreApplication;
import com.cody.fly.core.CoreProvider;
import com.cody.fly.core.DefaultAction;
import com.cody.fly.core.IWideRouter;
import com.cody.fly.core.utils.ProcessUtil;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by cody.yi on 2017/1/19.
 * 本地路由、进程内路由
 */
public class LocalRouter {
    private static final String TAG = "LocalRouter";
    private String mProcessName = ProcessUtil.UNKNOWN_PROCESS_NAME;
    private static LocalRouter sInstance = null;
    private HashMap<String, CoreProvider> mProviders = null;// 路由对象提供者集合
    private CoreApplication mApplication;
    private IWideRouter mWideRouterAIDL;// 进程间路由
    private static ExecutorService threadPool = null;// 线程池服务

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mWideRouterAIDL = IWideRouter.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mWideRouterAIDL = null;
        }
    };

    private LocalRouter(CoreApplication context) {
        mApplication = context;
        mProcessName = ProcessUtil.getProcessName(context, ProcessUtil.getMyProcessId());
        mProviders = new HashMap<>();
        // 是否需要多进程支持
        if (mApplication.needMultipleProcess()) {
            connectWideRouter();
        }
    }

    public static synchronized LocalRouter getInstance(@NonNull CoreApplication context) {
        if (sInstance == null) {
            sInstance = new LocalRouter(context);
        }
        return sInstance;
    }

    private static synchronized ExecutorService getThreadPool() {
        if (null == threadPool) {
            threadPool = Executors.newCachedThreadPool();
        }
        return threadPool;
    }

    void connectWideRouter() {
        Intent binderIntent = new Intent(mApplication, WideRouterConnectService.class);
        Bundle bundle = new Bundle();
        binderIntent.putExtras(bundle);
        mApplication.bindService(binderIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void disconnectWideRouter() {
        Intent binderIntent = new Intent(mApplication, WideRouterConnectService.class);
        Bundle bundle = new Bundle();
        binderIntent.putExtras(bundle);
        mApplication.unbindService(mServiceConnection);
    }

    public boolean registerProvider(String providerName, CoreProvider provider) {
        if (mProviders == null || providerName == null) {
            Log.e(TAG, "registerProvider providerName == null");
            return false;
        }
        if (!mProviders.containsKey(providerName)) {
            mProviders.put(providerName, provider);
            return true;
        }
        Log.e(TAG, "registerProvider providerName :" + providerName + " already register.");
        return false;
    }

    private boolean checkWideRouterConnection() {
        boolean result = false;
        if (mWideRouterAIDL != null) {
            result = true;
        }
        return result;
    }

    /**
     * 异步
     * @param routerRequest 路由请求
     * @return 结果
     */
    boolean answerWiderAsync(@NonNull RouterRequest routerRequest) {
        boolean result;
        if (mProcessName.equals(routerRequest.getDomain()) && checkWideRouterConnection()) {
            result = findRequestAction(routerRequest).isAsync(mApplication, routerRequest.getData());
        } else {
            return true;
        }
        return result;
    }

    public RouterResponse route(Context context, @NonNull RouterRequest routerRequest) throws Exception {
//        Log.e(TAG, mProcessName+", start: "+System.currentTimeMillis()+"\n"+routerRequest.toString());
        RouterResponse routerResponse = new RouterResponse();
        // Local request
        if (mProcessName.equals(routerRequest.getDomain())) {
            CoreAction targetAction = findRequestAction(routerRequest);
            routerResponse.mIsAsync = targetAction.isAsync(context, routerRequest.getData());
            // Sync result, return the result immediately.
            if (!routerResponse.mIsAsync) {
                CoreActionResult result = targetAction.invoke(context, routerRequest.getData());
                routerResponse.mResultString = result.toString();
                routerResponse.mObject = result.getObject();
//                Log.e(TAG, mProcessName+", local sync end: "+System.currentTimeMillis()+"\n"+routerRequest.toString());
            }
            // Async result, use the thread pool to execute the task.
            else {
                LocalTask task = new LocalTask(routerResponse, routerRequest, context, targetAction);
                routerResponse.mAsyncResponse = getThreadPool().submit(task);
            }
        } else if (!mApplication.needMultipleProcess()) {
            throw new Exception("Please make sure the returned value of needMultipleProcess in CoreApplication is true, so that you can invoke other process action.");
        }
        // IPC request
        else {
            // Has connected with wide router
            if (checkWideRouterConnection()) {
                routerResponse.mIsAsync = mWideRouterAIDL.isAsync(routerRequest.toString());
            }
            // Has not connected with the wide router.
            else {
                ConnectWideTask task = new ConnectWideTask(routerResponse, routerRequest);
                routerResponse.mAsyncResponse = getThreadPool().submit(task);
            }
            if (!routerResponse.mIsAsync) {
                routerResponse.mResultString = mWideRouterAIDL.route(routerRequest.toString());
                Log.e(TAG, mProcessName + ", wide sync end: " + System.currentTimeMillis() + "\n" + routerRequest.toString());
            }
            // Async result, use the thread pool to execute the task.
            else {
                WideTask task = new WideTask(routerRequest);
                routerResponse.mAsyncResponse = getThreadPool().submit(task);
            }
        }
        return routerResponse;
    }

    public boolean shutdownSelf(Class<? extends LocalRouterConnectService> clazz) {
        if (checkWideRouterConnection()) {
            try {
                if (mWideRouterAIDL.closeRouter(mProcessName)) {
                    mApplication.unbindService(mServiceConnection);
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            mApplication.stopService(new Intent(mApplication, clazz));
            return true;
        }
    }

    private CoreAction findRequestAction(RouterRequest routerRequest) {
        CoreProvider targetProvider = mProviders.get(routerRequest.getProvider());
        DefaultAction defaultNotFoundAction = new DefaultAction();
        if (null == targetProvider) {
            return defaultNotFoundAction;
        } else {
            CoreAction targetAction = targetProvider.getAction(routerRequest.getAction());
            if (null == targetAction) {
                return defaultNotFoundAction;
            } else {
                return targetAction;
            }
        }
    }

    private class LocalTask implements Callable<String> {
        private RouterResponse mResponse;
        private RouterRequest mRequest;
        private Context mContext;
        private CoreAction mAction;

        public LocalTask(RouterResponse routerResponse, RouterRequest routerRequest, Context context, CoreAction maAction) {
            this.mContext = context;
            this.mResponse = routerResponse;
            this.mRequest = routerRequest;
            this.mAction = maAction;
        }

        @Override
        public String call() throws Exception {
            CoreActionResult result = mAction.invoke(mContext, mRequest.getData());
            mResponse.mObject = result.getObject();
            Log.e(TAG, mProcessName + ", local async end: " + System.currentTimeMillis() + "\n" + mRequest.toString());
            return result.toString();
        }
    }

    private class WideTask implements Callable<String> {
        private RouterRequest mRequest;

        public WideTask(RouterRequest routerRequest) {
            this.mRequest = routerRequest;
        }

        @Override
        public String call() throws Exception {
            String result = mWideRouterAIDL.route(mRequest.toString());
            Log.e(TAG, mProcessName + ", wide end: " + System.currentTimeMillis() + "\n" + mRequest.toString());
            return result;
        }
    }

    private class ConnectWideTask implements Callable<String> {
        private RouterRequest mRequest;
        private RouterResponse mResponse;

        public ConnectWideTask(RouterResponse routerResponse, RouterRequest routerRequest) {
            this.mRequest = routerRequest;
            this.mResponse = routerResponse;
        }

        @Override
        public String call() throws Exception {
            Log.e(TAG, mProcessName + ", bind wide start: " + System.currentTimeMillis() + "\n" + mRequest.toString());
            connectWideRouter();
            mResponse.mIsAsync = true;
            int time = 0;
            while (true) {
                if (null == mWideRouterAIDL) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    time++;
                } else {
                    break;
                }
                if (time >= 600) {
                    DefaultAction defaultNotFoundAction = new DefaultAction(true, CoreActionResult.CODE_CANNOT_BIND_WIDE, "Can not bind wide router.");
                    CoreActionResult result = defaultNotFoundAction.invoke(mApplication, mRequest.getData());
                    mResponse.mIsAsync = true;
                    mResponse.mResultString = result.toString();
                    return result.toString();
                }
            }
            Log.e(TAG, mProcessName + ", bind wide end: " + System.currentTimeMillis() + "\n" + mRequest.toString());
            String result = mWideRouterAIDL.route(mRequest.toString());
            Log.e(TAG, mProcessName + ", connect wide end: " + System.currentTimeMillis() + "\n" + mRequest.toString());
            return result;
        }
    }
}
