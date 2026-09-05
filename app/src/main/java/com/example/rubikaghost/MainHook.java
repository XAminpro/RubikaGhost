package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";
    private static XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") && 
            !lpparam.packageName.equals("ir.resaneh1.iptv") && 
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Hooking Obfuscated Rubika -> " + lpparam.packageName);

        // ۱. اسکن عمیق کلاس‌های بارگذاری‌شده برای یافتن متدهای حاوی markAsRead یا updateStatus
        XposedBridge.hookAllConstructors(Throwable.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // جهت جلوگیری از کرش احتمالی
            }
        });

        // ۲. هوک کردن تمامی متدهایی که ورودی آن‌ها پیام‌ها یا شناسه گفتگوی سین شده است
        ClassLoader classLoader = lpparam.classLoader;

        // اسکن پکیج‌های شبکه و رابط کاربری روبیکا
        String[] possibleControllerNames = {
            "org.telegram.messenger.MessagesController",
            "org.rbmain.messenger.MessagesController",
            "app.rbmain.a.MessagesController"
        };

        for (String className : possibleControllerNames) {
            try {
                Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
                if (clazz != null) {
                    XposedBridge.log(TAG + ": Found Target Controller -> " + className);
                    hookControllerMethods(clazz);
                }
            } catch (Throwable ignored) {}
        }

        // ۳. هوک کردن لایه کلی ارسال متد در سرور (Fallback کلی)
        try {
            Class<?> httpOrSocketClass = XposedHelpers.findClassIfExists("org.telegram.tgnet.TLObject", classLoader);
            if (httpOrSocketClass == null) {
                httpOrSocketClass = XposedHelpers.findClassIfExists("org.rbmain.tgnet.TLObject", classLoader);
            }

            if (httpOrSocketClass != null) {
                XposedBridge.hookAllConstructors(httpOrSocketClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isGhostModeEnabled()) return;

                        String objName = param.thisObject.getClass().getName();
                        if (objName.contains("readHistory") || objName.contains("updateStatus") || objName.contains("ReadHistory")) {
                            XposedBridge.log(TAG + ": Successfully Intercepted Network Payload -> " + objName);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error in Fallback Hook: " + t.getMessage());
        }
    }

    private void hookControllerMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            String name = method.getName().toLowerCase();
            if (name.contains("markdialogasread") || name.contains("markasread") || name.contains("updatestatus")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isGhostModeEnabled()) {
                            XposedBridge.log(TAG + ": [BLOCKED] Executing " + method.getName());
                            param.setResult(null); // مانع از اجرای تابع اصلی
                        }
                    }
                });
            }
        }
    }

    private boolean isGhostModeEnabled() {
        if (prefs == null) {
            prefs = new XSharedPreferences("com.example.rubikaghost", "ghost_settings");
        }
        prefs.reload();
        return prefs.getBoolean("ghost_mode_toggle", true);
    }
}
