package com.xiaoc.warlock.Provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xiaoc.warlock.Util.AppChecker;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.service.WarLockServer;

public class AppProvider extends ContentProvider {
    static {
        System.loadLibrary("warlockCore");
    }

    @Override
    public boolean onCreate() {
        startWarLockService();
        AppChecker.checkReflectionSupport();
        return true;
    }
    private void startWarLockService() {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), WarLockServer.class);
            getContext().startService(intent);
        }
    }
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

}
