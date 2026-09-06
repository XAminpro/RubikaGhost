package com.example.rubikaghost;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";
    private static File logFile;
    private static ClassLoader targetClassLoader;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") &&
            !lpparam.packageName.equals("ir.resaneh1.iptv") &&
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Active -> " + lpparam.packageName);
        targetClassLoader = lpparam.classLoader;
        // ⚠️ اینجا دیگه سعی نمی‌کنیم Context بگیریم — هنوز زوده، Application ساخته نشده.
        // اولین باری که یه درخواست HTTP بیاد، اون موقع تلاش می‌کنیم (ensureLogFile).

        try {
            Class<?> requestBuilderClass = XposedHelpers.findClass("okhttp3.Request$Builder", lpparam.classLoader);
            final Class<?> bufferClass = XposedHelpers.findClass("okio.Buffer", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(requestBuilderClass, "build", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object request = param.getResult();
                        if (request == null) return;

                        Object urlObj = XposedHelpers.callMethod(request, "url");
                        String url = String.valueOf(urlObj);
                        String method = (String) XposedHelpers.callMethod(request, "method");

                        String bodyPreview = "(no body)";
                        Object body = XposedHelpers.callMethod(request, "body");
                        if (body != null) {
                            try {
                                Object buffer = bufferClass.getConstructor().newInstance();
                                XposedHelpers.callMethod(body, "writeTo", buffer);
                                String full = (String) XposedHelpers.callMethod(buffer, "readUtf8");
                                bodyPreview = full.length() > 500 ? full.substring(0, 500) + "...(truncated)" : full;
                            } catch (Throwable t) {
                                bodyPreview = "(could not read body: " + t + ")";
                            }
                        }

                        writeLog("HTTP " + method + " " + url + " | body: " + bodyPreview);
                    } catch (Throwable t) {
                        writeLog("logging error: " + t);
                    }
                }
            });

            XposedBridge.log(TAG + ": OkHttp hook active");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook setup FAILED: " + t);
        }
    }

    /**
     * تلاش برای پیدا کردن logFile، فقط وقتی واقعاً لازمش داریم (نه موقع handleLoadPackage).
     * تا اون لحظه، Application حتماً ساخته شده و Context در دسترسه.
     */
    private static synchronized boolean ensureLogFile() {
        if (logFile != null) return true;
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", targetClassLoader);
            Context ctx = (Context) XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication");
            if (ctx == null) {
                XposedBridge.log(TAG + ": currentApplication() still null, will retry later");
                return false;
            }
            File dir = ctx.getExternalFilesDir(null);
            if (dir == null) dir = ctx.getFilesDir();
            logFile = new File(dir, "rubikaghost_log.txt");
            XposedBridge.log(TAG + ": Writing logs to -> " + logFile.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": ensureLogFile failed: " + t);
            return false;
        }
    }

    private static synchronized void writeLog(String line) {
        if (!ensureLogFile()) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            String time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            pw.println("[" + time + "] " + line);
        } catch (Throwable ignored) {
        }
    }
}
