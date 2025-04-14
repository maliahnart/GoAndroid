package view;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gogameproject.MainActivity;
import com.example.gogameproject.R;

import config.ConfigLoader;
import config.GameMode;
import controller.AppController;
import controller.MainMenuController;
import config.GameConfig;

public class MainMenuActivity extends AppCompatActivity {
    private MainMenuController controller;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        controller = new MainMenuController(this);
        findViewById(R.id.playButton).setOnClickListener(v -> {
            ConfigLoader configLoader = new ConfigLoader(this);
            GameConfig config = configLoader.loadGameConfig();
            controller.startGame(config);
        });

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingActivity.class));
        });

        findViewById(R.id.exitButton).setOnClickListener(v -> finish());
    }
}