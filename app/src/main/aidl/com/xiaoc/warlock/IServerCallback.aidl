// IServerCallback.aidl
package com.xiaoc.warlock;

interface IServerCallback {
    void onSandboxDetected(String details);
    void ping();

}