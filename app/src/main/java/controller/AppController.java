package controller;

import android.content.Context;
import config.ConfigLoader;
import config.GameConfig;

public class AppController {
    private final Context context;
    private final ConfigLoader configLoader;

    public AppController(Context context) {
        this.context = context;
        this.configLoader = new ConfigLoader(context);
    }

    public GameConfig getGameConfig() {
        return configLoader.loadGameConfig();
    }

    public void saveGameConfig(GameConfig config) {
        configLoader.saveGameConfig(config);
    }
}