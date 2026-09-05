package com.example.rubikaghost;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {

    private static XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.contains("rbmain") && !lpparam.packageName.equals("ir.resaneh1.iptv")) {
            return;
        }

        // ۱. هوک کردن سین پیام (DialogsActivity)
        XposedHelpers.findAndHookMethod(
            "org.rbmain.ui.DialogsActivity",
            lpparam.classLoader,
            "markAsRead",
            long.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGhostModeEnabled()) {
                        param.setResult(null);
                    }
                }
            }
        );

        // ۲. هوک کردن آنلاین بودن (updateStatus)
        XposedHelpers.findAndHookMethod(
            "org.rbmain.ui.DialogsActivity",
            lpparam.classLoader,
            "updateStatus",
            XposedHelpers.findClass("org.rbmain.tgnet.TLRPC$User", lpparam.classLoader),
            boolean.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGhostModeEnabled()) {
                        param.setResult(null);
                    }
                }
            }
        );
    }

    private boolean isGhostModeEnabled() {
        if (prefs == null) {
            prefs = new XSharedPreferences("com.example.rubikaghost", "ghost_settings");
        }
        prefs.reload();
        return prefs.getBoolean("ghost_mode_toggle", true);
    }
}
