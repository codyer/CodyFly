package com.cody.fly.core.router;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cody.fly.core.CoreActionResult;
import com.cody.fly.core.CoreApplication;
import com.cody.fly.core.IWideRouter;

/**
 * Created by cody.yi on 2017/1/19.
 */

public final class WideRouterConnectService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!(getApplication() instanceof CoreApplication)) {
            throw new RuntimeException("Please check your AndroidManifest.xml and make sure the application is instance of MaApplication.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("WRCS", "onDestroy");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    IWideRouter.Stub stub = new IWideRouter.Stub() {

        @Override
        public boolean isAsync(String routerRequest) throws RemoteException {
            return WideRouter
                    .getInstance((CoreApplication) getApplication())
                    .answerLocalAsync(new RouterRequest
                            .Builder(getApplicationContext())
                            .requestString(routerRequest)
                            .build());
        }

        @Override
        public boolean closeRouter(String domain) throws RemoteException {
            return WideRouter
                    .getInstance((CoreApplication) getApplication())
                    .shutdownRouter(domain);
        }

        @Override
        public String route(String routerRequest) {
            try {
                return WideRouter
                        .getInstance((CoreApplication) getApplication())
                        .route(new RouterRequest
                                .Builder(getApplicationContext())
                                .requestString(routerRequest)
                                .build())
                        .get();
            } catch (Exception e) {
                e.printStackTrace();
                return new CoreActionResult.Builder()
                        .code(CoreActionResult.CODE_ERROR)
                        .msg(e.getMessage())
                        .build()
                        .toString();
            }
        }
    };
}
