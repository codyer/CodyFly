package com.cody.fly.core.router;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.cody.fly.core.CoreActionResult;
import com.cody.fly.core.CoreApplication;
import com.cody.fly.core.ILocalRouter;

/**
 * Created by cody.yi on 2017/1/19.
 * 进程内路由服务，每个module继承此服务并在manifest文件中注册
 */
public class LocalRouterConnectService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    ILocalRouter.Stub stub = new ILocalRouter.Stub() {

        @Override
        public boolean isAsync(String routerRequest) throws RemoteException {
            return LocalRouter.getInstance((CoreApplication) getApplication()).
                    answerWiderAsync(new RouterRequest
                    .Builder(getApplicationContext())
                    .requestString(routerRequest)
                    .build());
        }

        @Override
        public String route(String routerRequest) {
            try {
                return LocalRouter
                        .getInstance((CoreApplication) getApplication())
                        .route(LocalRouterConnectService.this,new RouterRequest
                                .Builder(getApplicationContext())
                                .requestString(routerRequest)
                                .build())
                        .get();
            } catch (Exception e) {
                e.printStackTrace();
                return new CoreActionResult.Builder().msg(e.getMessage()).build().toString();
            }
        }
    };
}
