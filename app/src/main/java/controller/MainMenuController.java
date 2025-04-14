package controller;

import android.content.Intent;
import config.GameConfig;
import view.GameActivity;
import view.MainMenuActivity;
import view.SettingActivity;

/**
 * Controller xử lý logic menu chính.
 */
public class MainMenuController {
    private final MainMenuActivity activity;

    public MainMenuController(MainMenuActivity activity) {
        this.activity = activity;
    }

    public void startGame(GameConfig config) {
        Intent intent = new Intent(activity, GameActivity.class);
        intent.putExtra("boardSize", config.getBoardSize());
        intent.putExtra("komi", config.getKomi());
        intent.putExtra("gameMode", config.getGameMode().name());
        activity.startActivity(intent);
    }

    public void openSettings() {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    public void exit() {
        activity.finish();
    }
}