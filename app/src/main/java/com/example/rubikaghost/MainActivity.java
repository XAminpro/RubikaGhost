package com.example.rubikaghost;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Switch switchView = new Switch(this);
        switchView.setText("Ø­Ø§Ù„Øª Ø±ÙˆØ­ ÙØ¹Ø§Ù„ Ø¨Ø§Ø´Ø¯");
        switchView.setPadding(50, 100, 50, 0);
        setContentView(switchView);

        SharedPreferences pref = getSharedPreferences("ghost_settings", Context.MODE_PRIVATE);
        switchView.setChecked(pref.getBoolean("ghost_mode_toggle", true));
        makeWorldReadable(); // Ø§ÙˆÙ„ÛŒÙ† Ø§Ø¬Ø±Ø§ Ù‡Ù… Ù¾Ø±Ù…ÛŒØ´Ù†â€ŒÙ‡Ø§ Ø±Ùˆ Ø¨Ø§Ø² Ú©Ù†ØŒ Ù†Ù‡ ÙÙ‚Ø· Ø¨Ø¹Ø¯ Ø§Ø² ØªØºÛŒÛŒØ± Ø³ÙˆØ¦ÛŒÚ†

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.edit().putBoolean("ghost_mode_toggle", isChecked).commit();
            makeWorldReadable();
        });
    }

    /**
     * Ø§Ø² Ø§Ù†Ø¯Ø±ÙˆÛŒØ¯ Û· Ø¨Ù‡ Ø¨Ø¹Ø¯ØŒ Ø¯Ø§ÛŒØ±Ú©ØªÙˆØ±ÛŒ Ø®ÙˆØ¯Ù Ø§Ù¾ mode=700 Ø¯Ø§Ø±Ù‡ (ÙÙ‚Ø· Ø®ÙˆØ¯Ù Ø§Ù¾ Ù…ÛŒâ€ŒØªÙˆÙ†Ù‡ ÙˆØ§Ø±Ø¯Ø´ Ø¨Ø´Ù‡).
     * XSharedPreferences.makeWorldReadable() ÙÙ‚Ø· ÙØ§ÛŒÙ„ XML Ø±Ùˆ Ø¨Ø§Ø² Ù…ÛŒâ€ŒÚ©Ù†Ù‡ØŒ Ù†Ù‡ Ù¾ÙˆØ´Ù‡â€ŒÙ‡Ø§ÛŒ ÙˆØ§Ù„Ø¯Ø´ â€”
     * Ù¾Ø³ Ù¾Ø±ÙˆØ³Ù‡â€ŒÛŒ Ø±ÙˆØ¨ÛŒÚ©Ø§ Ø­ØªÛŒ Ù†Ù…ÛŒâ€ŒØªÙˆÙ†Ù‡ ÙˆØ§Ø±Ø¯ Ù¾ÙˆØ´Ù‡ Ø¨Ø´Ù‡ ØªØ§ Ø¨Ù‡ ÙØ§ÛŒÙ„ Ø¨Ø±Ø³Ù‡.
     * Ø§ÛŒÙ†Ø¬Ø§ ØµØ±ÛŒØ­Ø§Ù‹ Ù¾Ø±Ù…ÛŒØ´Ù† read+execute Ø±Ùˆ Ø±ÙˆÛŒ Ø²Ù†Ø¬ÛŒØ±Ù‡â€ŒÛŒ Ú©Ø§Ù…Ù„ Ù¾ÙˆØ´Ù‡â€ŒÙ‡Ø§ Ø¨Ø§Ø² Ù…ÛŒâ€ŒÚ©Ù†ÛŒÙ…
     * (execute Ø±ÙˆÛŒ Ù¾ÙˆØ´Ù‡ ÛŒØ¹Ù†ÛŒ Ø§Ø¬Ø§Ø²Ù‡â€ŒÛŒ "traverse"/ÙˆØ±ÙˆØ¯ Ø¨Ù‡Ø´ØŒ Ù†Ù‡ Ø§Ø¬Ø±Ø§).
     */
    private void makeWorldReadable() {
        try {
            File dataDir = new File(getApplicationInfo().dataDir);
            dataDir.setExecutable(true, false);
            dataDir.setReadable(true, false);

            File prefsDir = new File(dataDir, "shared_prefs");
            prefsDir.setExecutable(true, false);
            prefsDir.setReadable(true, false);

            File prefsFile = new File(prefsDir, "ghost_settings.xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {
            // Ø§Ú¯Ù‡ Ø¨Ù‡ Ù‡Ø± Ø¯Ù„ÛŒÙ„ÛŒ (Ù…Ø«Ù„Ø§Ù‹ Ù…Ø­Ø¯ÙˆØ¯ÛŒØª SELinux Ø®Ø§Øµ ÛŒÙ‡ ROM) Ø§ÛŒÙ† Ú©Ø§Ø± Ù†Ø´Ø¯ØŒ
            // MainHook Ø¨Ø§ Ù„Ø§Ú¯ ØªØ´Ø®ÛŒØµÛŒâ€ŒØ§ÛŒ Ú©Ù‡ Ø¯Ø§Ø±Ù‡ØŒ Ø¯Ù‚ÛŒÙ‚Ø§Ù‹ Ù†Ø´ÙˆÙ† Ù…ÛŒØ¯Ù‡ Ú©Ø¬Ø§ Ú¯ÛŒØ± Ú©Ø±Ø¯Ù‡
        }
    }
}
