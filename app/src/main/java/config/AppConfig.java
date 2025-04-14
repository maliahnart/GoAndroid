package config;
public class AppConfig {
    private String language;
    private boolean soundEnabled;
    private String theme;

    // Mặc định: tiếng Anh, bật âm thanh, theme sáng
    public AppConfig() {
        this.language = "en";
        this.soundEnabled = true;
        this.theme = "light";
    }

    public AppConfig(String language, boolean soundEnabled, String theme) {
        this.language = language;
        this.soundEnabled = soundEnabled;
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}