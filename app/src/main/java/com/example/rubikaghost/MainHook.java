package com.example.rubikaghost;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // ۱. بررسی نام پکیج روبیکا
        if (!lpparam.packageName.contains("rbmain") && 
            !lpparam.packageName.equals("ir.resaneh1.iptv") && 
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Rubika process hooked -> " + lpparam.packageName);

        // ۲. هوک کلی متدهای لایه شبکه (TLRPC) که مسئول ارسال درخواست سین و وضعیت آنلاین به سرور هستند
        try {
            Class<?> tgnetClass = XposedHelpers.findClassIfExists("org.telegram.tgnet.ConnectionsManager", lpparam.classLoader);
            if (tgnetClass == null) {
                tgnetClass = XposedHelpers.findClassIfExists("org.rbmain.tgnet.ConnectionsManager", lpparam.classLoader);
            }

            if (tgnetClass != null) {
                XposedBridge.log(TAG + ": ConnectionsManager found!");

                // هوک کردن تابع ارسال درخواست به سرور (sendRequest)
                XposedHelpers.findAndHookMethod(
                    tgnetClass,
                    "sendRequest",
                    XposedHelpers.findClassIfExists("org.telegram.tgnet.TLObject", lpparam.classLoader) != null ?
                        XposedHelpers.findClass("org.telegram.tgnet.TLObject", lpparam.classLoader) :
                        XposedHelpers.findClass("org.rbmain.tgnet.TLObject", lpparam.classLoader),
                    XposedHelpers.findClassIfExists("org.telegram.tgnet.RequestDelegate", lpparam.classLoader) != null ?
                        XposedHelpers.findClass("org.telegram.tgnet.RequestDelegate", lpparam.classLoader) :
                        XposedHelpers.findClass("org.rbmain.tgnet.RequestDelegate", lpparam.classLoader),
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object object = param.args[0];
                            if (object == null) return;

                            String requestName = object.getClass().getName();
                            
                            // بررسی درخواست‌های مربوط به سین زدن (Read History) یا آنلاین بودن (Status)
                            if (requestName.contains("messages_readHistory") || 
                                requestName.contains("messages_receivedQueue") || 
                                requestName.contains("account_updateStatus")) {
                                
                                XposedBridge.log(TAG + ": Blocked network request -> " + requestName);
                                // جلو گیری از ارسال درخواست سین به سرور روبیکا
                                param.setResult(0);
                            }
                        }
                    }
                );
            } else {
                XposedBridge.log(TAG + ": ConnectionsManager NOT found!");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error hooking network layer: " + t.getMessage());
        }
    }
}
