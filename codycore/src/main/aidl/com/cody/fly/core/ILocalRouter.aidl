// ILocalRouter.aidl
package com.cody.fly.core;

// Declare any non-default types here with import statements

interface ILocalRouter {
    boolean isAsync(String routerRequest);
    String route(String routerRequest);
}
