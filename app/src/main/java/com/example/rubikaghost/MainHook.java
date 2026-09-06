package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import org.json.JSONObject;

import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RubikaGhostLog";
    private static final String MODULE_PKG = "com.example.rubikaghost";
    private static final String TARGET_CLASS = "androidMessenger.network.NetworkImpl";

    // âš ï¸ Ù†Ø³Ø®Ù‡â€ŒÛŒ ØªØ´Ø®ÛŒØµÛŒ: Ú†ÛŒØ²ÛŒ Ø±Ùˆ Ø¨Ù„Ø§Ú© Ù†Ù…ÛŒâ€ŒÚ©Ù†Ù‡ØŒ ÙÙ‚Ø· Ù‡Ù…Ù‡â€ŒÛŒ Ø§Ø³Ù… Ù…ØªØ¯Ù‡Ø§ÛŒ API Ø±Ùˆ Ù„Ø§Ú¯ Ù…ÛŒâ€ŒÚ©Ù†Ù‡
    // ØªØ§ Ø¨ÙÙ‡Ù…ÛŒÙ… Ú©Ø¯ÙˆÙ…â€ŒØ´ÙˆÙ† Ù…Ø³Ø¦ÙˆÙ„ Ø§Ø¹Ù„Ø§Ù… Ø¢Ù†Ù„Ø§ÛŒÙ†/Ø¢ÙÙ„Ø§ÛŒÙ† Ø¨ÙˆØ¯Ù†Ù‡.
    // Ø¨Ø¹Ø¯ Ø§Ø² Ø§ÛŒÙ†Ú©Ù‡ Ù„Ø§Ú¯ Ø±Ùˆ Ø¨Ø±Ø±Ø³ÛŒ Ú©Ø±Ø¯ÛŒÙ…ØŒ Ø¨Ø±Ù…ÛŒâ€ŒÚ¯Ø±Ø¯ÛŒÙ… Ø¨Ù‡ Ù†Ø³Ø®Ù‡â€ŒÛŒ Ø¨Ù„Ø§Ú©â€ŒÚ©Ù†Ù†Ø¯Ù‡.

    private XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") &&
            !lpparam.packageName.equals("ir.resaneh1.iptv") &&
            !lpparam.packageName.contains("rubika")) {
            return;
        }

        XposedBridge.log(TAG + ": Active -> " + lpparam.packageName);

        try {
            prefs = new XSharedPreferences(MODULE_PKG, "ghost_settings");
            prefs.makeWorldReadable();
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Prefs setup FAILED: " + t);
        }

        try {
            Class<?> networkImplClass = XposedHelpers.findClass(TARGET_CLASS, lpparam.classLoader);

            int hookedCount = 0;
            for (Method method : networkImplClass.getDeclaredMethods()) {
                if (method.getReturnType() != int.class) continue;

                Class<?>[] paramTypes = method.getParameterTypes();
                int jsonIndex = -1;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (paramTypes[i] == JSONObject.class) {
                        jsonIndex = i;
                        break;
                    }
                }
                if (jsonIndex <= 0 || paramTypes[jsonIndex - 1] != String.class) continue;

                final int methodNameIndex = jsonIndex - 1;
                final int jsonArgIndex = jsonIndex;

                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args == null || param.args.length <= methodNameIndex) return;
                        Object arg = param.args[methodNameIndex];
                        if (!(arg instanceof String)) return;

                        // ÙÙ‚Ø· Ù„Ø§Ú¯ â€” Ù‡ÛŒÚ†ÛŒ Ø±Ùˆ Ø¨Ù„Ø§Ú© Ù†Ù…ÛŒâ€ŒÚ©Ù†ÛŒÙ… ØªÙˆ Ø§ÛŒÙ† Ù†Ø³Ø®Ù‡
                        String jsonPreview = "";
                        try {
                            Object jsonArg = param.args[jsonArgIndex];
                            if (jsonArg instanceof JSONObject) {
                                String s = jsonArg.toString();
                                jsonPreview = s.length() > 200 ? s.substring(0, 200) + "..." : s;
                            }
                        } catch (Throwable ignored) {
                        }

                        XposedBridge.log(TAG + ": API-CALL -> " + arg + " | data: " + jsonPreview);
                    }
                });

                hookedCount++;
            }
            XposedBridge.log(TAG + ": Diagnostic mode active, hooked " + hookedCount + " methods (log-only, nothing blocked)");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook setup FAILED: " + t);
        }
    }
}
