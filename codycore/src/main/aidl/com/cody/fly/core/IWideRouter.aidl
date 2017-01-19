// IWideRouter.aidl
package com.cody.fly.core;

// Declare any non-default types here with import statements

interface IWideRouter {
    boolean isAsync(String routerRequest);
    boolean closeRouter(String domain);
    String route(String routerRequest);
}
