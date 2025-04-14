package config;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigLoader {
    private static final String PREFS_NAME = "GoGamePrefs";
    private final Context context;

    public ConfigLoader(Context context) {
        this.context = context;
    }

    public GameConfig loadGameConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int boardSize = prefs.getInt("boardSize", 9);
        float komi = prefs.getFloat("komi", 6.5f);
        String modeName = prefs.getString("gameMode", GameMode.PVP.name());
        String timeControlName = prefs.getString("timeControl", TimeControl.SUDDEN_DEATH.name());
        int timeLimit = prefs.getInt("timeLimit", 30 * 60); // 30 phút
        String scoringRuleName = prefs.getString("scoringRule", ScoringRule.JAPANESE.name());

        GameMode mode = GameMode.PVP;
        TimeControl timeControl = TimeControl.SUDDEN_DEATH;
        ScoringRule scoringRule = ScoringRule.JAPANESE;
        try {
            mode = GameMode.valueOf(modeName);
            timeControl = TimeControl.valueOf(timeControlName);
            scoringRule = ScoringRule.valueOf(scoringRuleName);
        } catch (IllegalArgumentException e) {
            // Sử dụng giá trị mặc định nếu lỗi
        }

        return new GameConfig(boardSize, komi, mode, timeControl, timeLimit, scoringRule);
    }

    public void saveGameConfig(GameConfig config) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("boardSize", config.getBoardSize());
        editor.putFloat("komi", config.getKomi());
        editor.putString("gameMode", config.getGameMode().name());
        editor.putString("timeControl", config.getTimeControl().name());
        editor.putInt("timeLimit", config.getTimeLimit());
        editor.putString("scoringRule", config.getScoringRule().name());
        editor.apply();
    }

    public AppConfig loadAppConfig() {
        return new AppConfig();
    }

    public void saveAppConfig(AppConfig config) {
        // TODO: Lưu AppConfig nếu cần
    }
}