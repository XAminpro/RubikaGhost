package com.example.rubikaghost;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Switch switchView = new Switch(this);
        switchView.setText("حالت روح فعال باشد");
        switchView.setPadding(50, 100, 50, 0);
        setContentView(switchView);

        SharedPreferences pref = getSharedPreferences("ghost_settings", Context.MODE_PRIVATE);
        switchView.setChecked(pref.getBoolean("ghost_mode_toggle", true));

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.edit().putBoolean("ghost_mode_toggle", isChecked).apply();
        });
    }
}
