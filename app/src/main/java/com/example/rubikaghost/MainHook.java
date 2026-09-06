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

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") &&
            !lpparam.packageName.equals("ir.resaneh1.iptv") &&
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Active -> " + lpparam.packageName);

        try {
            // به‌جای AndroidAppHelper (که تو این نسخه‌ی api جار نیست)،
            // مستقیم از android.app.ActivityThread.currentApplication() با reflection استفاده می‌کنیم
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader);
            Context ctx = (Context) XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication");

            File dir = ctx.getExternalFilesDir(null);
            if (dir == null) dir = ctx.getFilesDir();
            logFile = new File(dir, "rubikaghost_log.txt");
            writeLog("===== SESSION START =====");
            writeLog("Log file path: " + logFile.getAbsolutePath());
            XposedBridge.log(TAG + ": Writing logs to -> " + logFile.getAbsolutePath());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Could not set up log file: " + t);
        }

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

            writeLog("OkHttp Request.Builder.build() hooked successfully");
            XposedBridge.log(TAG + ": OkHttp hook active, writing to file");
        } catch (Throwable t) {
            writeLog("Hook setup FAILED: " + t);
            XposedBridge.log(TAG + ": Hook setup FAILED: " + t);
        }
    }

    private static synchronized void writeLog(String line) {
        if (logFile == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            String time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            pw.println("[" + time + "] " + line);
        } catch (Throwable ignored) {
        }
    }
}
