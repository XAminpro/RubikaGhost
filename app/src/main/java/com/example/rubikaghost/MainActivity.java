package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";

    // âš ï¸ Ù†Ø³Ø®Ù‡â€ŒÛŒ ØªØ´Ø®ÛŒØµÛŒ Ù†Ù‡Ø§ÛŒÛŒ: Ø²ÛŒØ± Ù„Ø§ÛŒÙ‡â€ŒÛŒ JSON Ù…ÛŒØ±Ù‡ØŒ Ù…Ø³ØªÙ‚ÛŒÙ… Ø±Ùˆ Ø®ÙˆØ¯Ù okhttp
    // Ù‡Ø± Ø¯Ø±Ø®ÙˆØ§Ø³Øª HTTP Ø®Ø§Ù…ÛŒ Ú©Ù‡ Ø§Ø² Ø§Ù¾ Ø®Ø§Ø±Ø¬ Ù…ÛŒØ´Ù‡ Ø±Ùˆ Ù„Ø§Ú¯ Ù…ÛŒâ€ŒÚ©Ù†Ù‡ (URL Ú©Ø§Ù…Ù„ + Ù…ØªØ¯ + Ø¨Ø¯Ù†Ù‡)
    // Ø¨Ø¯ÙˆÙ† ØªÙˆØ¬Ù‡ Ø¨Ù‡ Ø§ÛŒÙ†Ú©Ù‡ Ø§Ø² Ú©Ø¯ÙˆÙ… Ú©Ù„Ø§Ø³ Ø¬Ø§ÙˆØ§ Ø§ÙˆÙ…Ø¯Ù‡.

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") &&
            !lpparam.packageName.equals("ir.resaneh1.iptv") &&
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Active -> " + lpparam.packageName);

        try {
            Class<?> requestBuilderClass = XposedHelpers.findClass("okhttp3.Request$Builder", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(requestBuilderClass, "build", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Request request = (Request) param.getResult();
                        if (request == null) return;

                        String url = request.url().toString();
                        String method = request.method();

                        String bodyPreview = "(no body)";
                        RequestBody body = request.body();
                        if (body != null) {
                            try {
                                Buffer buffer = new Buffer();
                                body.writeTo(buffer);
                                String full = buffer.readUtf8();
                                bodyPreview = full.length() > 300 ? full.substring(0, 300) + "...(truncated)" : full;
                            } catch (Throwable t) {
                                bodyPreview = "(could not read body: " + t + ")";
                            }
                        }

                        XposedBridge.log(TAG + ": HTTP " + method + " " + url + " | body: " + bodyPreview);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": logging error: " + t);
                    }
                }
            });

            XposedBridge.log(TAG + ": OkHttp Request.Builder.build() hooked successfully");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook setup FAILED: " + t);
        }
    }
}
