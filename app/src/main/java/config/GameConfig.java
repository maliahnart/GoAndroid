package config;

public class GameConfig {
    private int boardSize;
    private float komi;
    private GameMode gameMode;
    private TimeControl timeControl;
    private int timeLimit; // Tính bằng giây
    private ScoringRule scoringRule;

    // Constructor mặc định
    public GameConfig() {
        this.boardSize = 9;
        this.komi = 6.5f;
        this.gameMode = GameMode.PVP;
        this.timeControl = TimeControl.SUDDEN_DEATH;
        this.timeLimit = 30 * 60; // 30 phút
        this.scoringRule = ScoringRule.JAPANESE;
    }

    public GameConfig(int boardSize, float komi, GameMode gameMode,
                      TimeControl timeControl, int timeLimit, ScoringRule scoringRule) {
        if (!isValidBoardSize(boardSize)) {
            throw new IllegalArgumentException("Invalid board size: " + boardSize);
        }
        if (komi < 0 || komi > 50) {
            throw new IllegalArgumentException("Komi must be between 0 and 50");
        }
        if (timeLimit < 0) {
            throw new IllegalArgumentException("Time limit must be non-negative");
        }
        this.boardSize = boardSize;
        this.komi = komi;
        this.gameMode = gameMode;
        this.timeControl = timeControl;
        this.timeLimit = timeLimit;
        this.scoringRule = scoringRule;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        if (!isValidBoardSize(boardSize)) {
            throw new IllegalArgumentException("Invalid board size: " + boardSize);
        }
        this.boardSize = boardSize;
    }

    public float getKomi() {
        return komi;
    }

    public void setKomi(float komi) {
        if (komi < 0 || komi > 50) {
            throw new IllegalArgumentException("Komi must be between 0 and 50");
        }
        this.komi = komi;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public TimeControl getTimeControl() {
        return timeControl;
    }

    public void setTimeControl(TimeControl timeControl) {
        this.timeControl = timeControl;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        if (timeLimit < 0) {
            throw new IllegalArgumentException("Time limit must be non-negative");
        }
        this.timeLimit = timeLimit;
    }

    public ScoringRule getScoringRule() {
        return scoringRule;
    }

    public void setScoringRule(ScoringRule scoringRule) {
        this.scoringRule = scoringRule;
    }

    private boolean isValidBoardSize(int size) {
        return size == 4 || size == 9 || size == 13 || size == 19;
    }
}