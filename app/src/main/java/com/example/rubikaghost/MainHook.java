package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";

    // Ù†Ø³Ø®Ù‡â€ŒÛŒ ØªØ´Ø®ÛŒØµÛŒ: Ø²ÛŒØ± Ù„Ø§ÛŒÙ‡â€ŒÛŒ JSON Ù…ÛŒØ±Ù‡ØŒ Ù…Ø³ØªÙ‚ÛŒÙ… Ø±Ùˆ okhttp â€” Ø¨Ø§ reflection Ø®Ø§Ù„Øµ
    // (Ø¨Ø¯ÙˆÙ† import Ù…Ø³ØªÙ‚ÛŒÙ… okhttp3/okioØŒ ØªØ§ NoClassDefFoundError Ù†Ø¯Ù‡)

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
            final Class<?> bufferClass = XposedHelpers.findClass("okio.Buffer", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(requestBuilderClass, "build", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object request = param.getResult();
                        if (request == null) return;

                        // url() -> HttpUrl Ø¢Ø¨Ø¬Ú©ØªØ› ÙÙ‚Ø· toString ØµØ¯Ø§Ø´ Ù…ÛŒâ€ŒØ²Ù†ÛŒÙ…ØŒ Ù†ÛŒØ§Ø²ÛŒ Ø¨Ù‡ Ø´Ù†Ø§Ø®ØªÙ† ØªØ§ÛŒÙ¾Ø´ Ù†ÛŒØ³Øª
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
