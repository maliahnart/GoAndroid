package com.example.gogameproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int SPLASH_TIME = 3000; // 3 giây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // màn hình nền tạm thời

        // Delay 3s rồi chuyển sang SettingActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, view.MainMenuActivity.class);
            startActivity(intent);
            finish(); // đóng splash
        }, SPLASH_TIME);
    }
}
