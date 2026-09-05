package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") && 
            !lpparam.packageName.equals("ir.resaneh1.iptv") && 
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Scanning process -> " + lpparam.packageName);

        // ۱. هوک کردن مستقیم متد markAsRead در کلاس‌های عمومی UI
        try {
            XposedHelpers.findAndHookMethod(
                "org.telegram.messenger.MessagesController",
                lpparam.classLoader,
                "markDialogAsRead",
                long.class, int.class, int.class, int.class, boolean.class, int.class, boolean.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(TAG + ": BLOCKED markDialogAsRead via MessagesController");
                        param.setResult(null);
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Standard MessagesController not found, trying fallback...");
        }

        // ۲. هوک کردن سازنده تمام کلاس‌های مربوط به درخواست‌های TLObject
        try {
            Class<?> tlObjectClazz = XposedHelpers.findClassIfExists("org.telegram.tgnet.TLObject", lpparam.classLoader);
            if (tlObjectClazz == null) {
                tlObjectClazz = XposedHelpers.findClassIfExists("org.rbmain.tgnet.TLObject", lpparam.classLoader);
            }

            if (tlObjectClazz != null) {
                XposedBridge.hookAllConstructors(tlObjectClazz, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String className = param.thisObject.getClass().getName();
                        if (className.toLowerCase().contains("readhistory") || 
                            className.toLowerCase().contains("updatestatus")) {
                            XposedBridge.log(TAG + ": Detected Object Creation -> " + className);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error tracing TLObject: " + t.getMessage());
        }
    }
}
