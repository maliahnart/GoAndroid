package controller;

import config.ConfigLoader;
import config.GameConfig;
import config.GameMode;
import config.ScoringRule;
import config.TimeControl;
import view.SettingActivity;

public class SettingsController {
    private final SettingActivity activity;
    private final ConfigLoader configLoader;

    public SettingsController(SettingActivity activity) {
        this.activity = activity;
        this.configLoader = new ConfigLoader(activity);
    }

    public void saveSettings(int boardSize, float komi, GameMode mode,
                             TimeControl timeControl, int timeLimit, ScoringRule scoringRule) {
        GameConfig config = new GameConfig(boardSize, komi, mode, timeControl, timeLimit, scoringRule);
        configLoader.saveGameConfig(config);
        activity.finish();
    }

    public GameConfig loadSettings() {
        return configLoader.loadGameConfig();
    }
}