package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import org.json.JSONObject;

import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";
    private static final String MODULE_PKG = "com.example.rubikaghost";
    private static final String TARGET_CLASS = "androidMessenger.network.NetworkImpl";

    private XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") &&
            !lpparam.packageName.equals("ir.resaneh1.iptv") &&
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Active -> " + lpparam.packageName);

        // مرحله ۱: راه‌اندازی preferences — جدا از هوک اصلی، هر خطایی اینجا نباید کل هوک رو خراب کنه
        try {
            prefs = new XSharedPreferences(MODULE_PKG, "ghost_settings");
            prefs.makeWorldReadable();
            XposedBridge.log(TAG + ": Prefs ready, exists=" + prefs.getFile().exists());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Prefs setup FAILED (continuing anyway): " + t);
            prefs = null;
        }

        // مرحله ۲: پیدا کردن و هوک کردن کلاس شبکه
        try {
            XposedBridge.log(TAG + ": Looking for " + TARGET_CLASS);
            Class<?> networkImplClass = XposedHelpers.findClass(TARGET_CLASS, lpparam.classLoader);
            XposedBridge.log(TAG + ": Class found, scanning methods...");

            int hookedCount = 0;
            for (Method method : networkImplClass.getDeclaredMethods()) {
                Class<?>[] paramTypes = method.getParameterTypes();
                int jsonIndex = -1;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (paramTypes[i] == JSONObject.class) {
                        jsonIndex = i;
                        break;
                    }
                }
                if (jsonIndex <= 0 || paramTypes[jsonIndex - 1] != String.class) continue;

                final int methodNameIndex = jsonIndex - 1;

                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args == null || param.args.length <= methodNameIndex) return;
                        Object arg = param.args[methodNameIndex];
                        if (!(arg instanceof String)) return;

                        String apiMethod = ((String) arg).toLowerCase();
                        if (!apiMethod.contains("seen") &&
                            !apiMethod.contains("read") &&
                            !apiMethod.contains("status")) {
                            return;
                        }

                        boolean ghostOn = true;
                        if (prefs != null) {
                            try {
                                prefs.reload();
                                ghostOn = prefs.getBoolean("ghost_mode_toggle", true);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (!ghostOn) return;

                        XposedBridge.log(TAG + ": >>> BLOCKED -> " + arg);
                        param.setResult(0);
                    }
                });

                hookedCount++;
                XposedBridge.log(TAG + ": Hooked -> " + method.getName() + " (arity " + paramTypes.length + ")");
            }
            XposedBridge.log(TAG + ": Total hooked methods = " + hookedCount);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook setup FAILED: " + t);
        }
    }
}
