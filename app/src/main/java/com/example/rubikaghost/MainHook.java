package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";
    private static XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // ۱. بررسی دقیق پکیج روبیکا مطابق لاگ گوشی شما
        if (!lpparam.packageName.contains("rbmain") && 
            !lpparam.packageName.equals("ir.resaneh1.iptv") && 
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Successfully loaded in process -> " + lpparam.packageName);

        // ۲. پیدا کردن کلاس شبکه به روش تعمیم‌یافته (Dynamic Finding)
        String[] possibleClasses = {
            "org.telegram.tgnet.ConnectionsManager",
            "org.rbmain.tgnet.ConnectionsManager",
            "ir.resaneh1.iptv.tgnet.ConnectionsManager"
        };

        Class<?> connectionsManagerClass = null;
        for (String className : possibleClasses) {
            connectionsManagerClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (connectionsManagerClass != null) {
                XposedBridge.log(TAG + ": Found ConnectionsManager class -> " + className);
                break;
            }
        }

        if (connectionsManagerClass != null) {
            // هوک کردن تمام متدهای sendRequest موجود در کلاس
            for (Method method : connectionsManagerClass.getDeclaredMethods()) {
                if (method.getName().equals("sendRequest")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!isGhostModeEnabled()) {
                                return; // اگر حالت روح خاموش باشد، اجازه ارسال عادی داده می‌شود
                            }

                            if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                                Object requestObj = param.args[0];
                                String reqName = requestObj.getClass().getName();

                                // مسدودسازی درخواست‌های خوانده شدن پیام (Read History) و وضعیت آنلاین بودن (Status)
                                if (reqName.toLowerCase().contains("readhistory") || 
                                    reqName.toLowerCase().contains("updatestatus") ||
                                    reqName.toLowerCase().contains("receivedqueue")) {
                                    
                                    XposedBridge.log(TAG + ": [GHOST MODE ACTIVE] Successfully BLOCKED -> " + reqName);
                                    param.setResult(0); // خنثی‌سازی ارسال به سرور
                                }
                            }
                        }
                    });
                }
            }
        } else {
            XposedBridge.log(TAG + ": Could not locate ConnectionsManager dynamically.");
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
