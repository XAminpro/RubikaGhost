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

        XposedBridge.log(TAG + ": Deep scanning process -> " + lpparam.packageName);

        // ۱. هوک کردن تمام متدهای sendRequest بدون وابستگی به نام پکیج
        try {
            XposedHelpers.findAndHookMethod(
                ClassLoader.class,
                "loadClass",
                String.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Class<?> clazz = (Class<?>) param.getResult();
                        if (clazz == null) return;

                        String className = clazz.getName();
                        
                        // بررسی کلاس‌های هدف لایه شبکه و کنترلر
                        if (className.endsWith("ConnectionsManager") || className.endsWith("MessagesController")) {
                            XposedBridge.log(TAG + ": Dynamically intercepted class -> " + className);
                            hookTargetMethods(clazz);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error in Dynamic Scanner: " + t.getMessage());
        }
    }

    private static void hookTargetMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            String methodName = method.getName();

            // هوک کردن تابع ارسال درخواست شبکه (sendRequest)
            if (methodName.equals("sendRequest")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isGhostModeEnabled()) return;

                        if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                            String reqPayload = param.args[0].getClass().getName();
                            
                            if (reqPayload.toLowerCase().contains("readhistory") || 
                                reqPayload.toLowerCase().contains("updatestatus")) {
                                
                                XposedBridge.log(TAG + ": [SUCCESS] Blocked payload -> " + reqPayload);
                                param.setResult(0); // خنثی کردن ارسال به سرور
                            }
                        }
                    }
                });
            }

            // هوک کردن تابع علامت‌گذاری پیام‌ها به عنوان خوانده‌شده (markDialogAsRead)
            if (methodName.toLowerCase().contains("markdialogasread") || methodName.toLowerCase().contains("markasread")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isGhostModeEnabled()) {
                            XposedBridge.log(TAG + ": [SUCCESS] Blocked local read action -> " + methodName);
                            param.setResult(null);
                        }
                    }
                });
            }
        }
    }

    private static boolean isGhostModeEnabled() {
        if (prefs == null) {
            prefs = new XSharedPreferences("com.example.rubikaghost", "ghost_settings");
        }
        prefs.reload();
        return prefs.getBoolean("ghost_mode_toggle", true);
    }
}
