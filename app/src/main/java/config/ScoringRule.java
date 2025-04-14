package config;

public enum ScoringRule {
    JAPANESE("Japanese"),
    CHINESE("Chinese");

    private final String displayName;

    ScoringRule(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}