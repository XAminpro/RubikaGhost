package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") && 
            !lpparam.packageName.equals("ir.resaneh1.iptv") && 
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": ClassLoader Hook Active -> " + lpparam.packageName);

        // ۱. هوک کردن مستقیم متد sendRequest در تمام کلاس‌هایی که نام متد آن‌ها sendRequest است
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.ClassLoader",
                lpparam.classLoader,
                "loadClass",
                String.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Class<?> clazz = (Class<?>) param.getResult();
                        if (clazz == null) return;

                        for (Method method : clazz.getDeclaredMethods()) {
                            // اگر متد sendRequest با پارامترهای اصلی تلگرام/روبیکا پیدا شد
                            if (method.getName().equals("sendRequest") && Modifier.isPublic(method.getModifiers())) {
                                XposedBridge.log(TAG + ": FOUND sendRequest in class -> " + clazz.getName());
                                
                                XposedBridge.hookMethod(method, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                                        if (p.args != null && p.args.length > 0 && p.args[0] != null) {
                                            String requestClass = p.args[0].getClass().getName();
                                            XposedBridge.log(TAG + ": OUTGOING REQUEST -> " + requestClass);
                                            
                                            if (requestClass.toLowerCase().contains("read") || 
                                                requestClass.toLowerCase().contains("status") ||
                                                requestClass.toLowerCase().contains("seen")) {
                                                XposedBridge.log(TAG + ": >>> BLOCKED SEEN/STATUS REQUEST <<<");
                                                p.setResult(0);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Error: " + t.getMessage());
        }
    }
}
