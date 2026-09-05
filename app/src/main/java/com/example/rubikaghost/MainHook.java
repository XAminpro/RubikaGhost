package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";
    private static final String MODULE_PKG = "com.example.rubikaghost";

    // جلوگیری از هوک کردن دوباره‌ی همون متد
    private final Set<Method> hookedMethods =
            Collections.newSetFromMap(new WeakHashMap<>());

    private XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") &&
            !lpparam.packageName.equals("ir.resaneh1.iptv") &&
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Active -> " + lpparam.packageName);

        prefs = new XSharedPreferences(MODULE_PKG, "ghost_settings");
        prefs.makeWorldReadable();

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

                    String className = clazz.getName();
                    // فقط کلاس‌های خود اپ روبیکا رو اسکن کن، نه android/kotlin/کتابخونه‌ها
                    if (className.startsWith("android.") ||
                        className.startsWith("androidx.") ||
                        className.startsWith("java.") ||
                        className.startsWith("kotlin.") ||
                        className.startsWith("com.google.")) {
                        return;
                    }

                    Method[] methods;
                    try {
                        methods = clazz.getDeclaredMethods();
                    } catch (Throwable t) {
                        return; // بعضی کلاس‌ها موقع reflection ارور می‌دن
                    }

                    for (Method method : methods) {
                        if (!method.getName().equals("sendRequest") ||
                            !Modifier.isPublic(method.getModifiers())) {
                            continue;
                        }
                        if (!hookedMethods.add(method)) continue; // قبلاً هوک شده

                        XposedBridge.log(TAG + ": FOUND sendRequest -> " + clazz.getName()
                                + " returns " + method.getReturnType());

                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                                if (prefs != null) {
                                    prefs.reload();
                                    if (!prefs.getBoolean("ghost_mode_toggle", true)) {
                                        return; // کاربر حالت روح رو خاموش کرده
                                    }
                                }

                                if (p.args == null || p.args.length == 0 || p.args[0] == null) return;

                                String requestClass = p.args[0].getClass().getName().toLowerCase();
                                if (requestClass.contains("read") ||
                                    requestClass.contains("status") ||
                                    requestClass.contains("seen")) {

                                    XposedBridge.log(TAG + ": >>> BLOCKED -> " + requestClass);

                                    Class<?> returnType = method.getReturnType();
                                    if (returnType == void.class) {
                                        p.setResult(null);
                                    } else if (returnType == int.class) {
                                        p.setResult(0);
                                    } else if (returnType == boolean.class) {
                                        p.setResult(false);
                                    } else if (returnType == long.class) {
                                        p.setResult(0L);
                                    } else if (returnType.isPrimitive()) {
                                        p.setResult(0);
                                    } else {
                                        p.setResult(null); // هر نوع آبجکتی
                                    }
                                }
                            }
                        });
                    }
                }
            }
        );
    }
}
