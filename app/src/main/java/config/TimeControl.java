package config;

public enum TimeControl {
    SUDDEN_DEATH("Sudden Death"),
    BYO_YOMI("Byo-yomi"),
    CANADIAN("Canadian Overtime");

    private final String displayName;

    TimeControl(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}