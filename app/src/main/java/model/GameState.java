package model;

import config.GameConfig;
import config.TimeControl;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private BoardState boardState;
    private BoardState previousBoardState;
    private Stone currentPlayer;
    private final List<Move> moveHistory;
    private int consecutivePasses;
    private int blackCaptured;
    private int whiteCaptured;
    private final float komi;
    private boolean gameOver;
    private String endGameReason;
    private long blackTimeLeft; // Thời gian còn lại của Đen (ms)
    private long whiteTimeLeft; // Thời gian còn lại của Trắng (ms)
    private final TimeControl timeControl; // Lưu TimeControl từ config

    public GameState(GameConfig config) {
        this.boardState = new BoardState(config);
        this.previousBoardState = null;
        this.currentPlayer = Stone.BLACK;
        this.moveHistory = new ArrayList<>();
        this.consecutivePasses = 0;
        this.blackCaptured = 0;
        this.whiteCaptured = 0;
        this.komi = config.getKomi();
        this.gameOver = false;
        this.endGameReason = "";
        this.blackTimeLeft = config.getTimeLimit() * 1000L; // Chuyển giây thành ms
        this.whiteTimeLeft = config.getTimeLimit() * 1000L;
        this.timeControl = config.getTimeControl(); // Lưu TimeControl
    }

    // Constructor sao chép
    public GameState(GameState other) {
        this.boardState = other.boardState.copy();
        this.previousBoardState = other.previousBoardState != null ? other.previousBoardState.copy() : null;
        this.currentPlayer = other.currentPlayer;
        this.moveHistory = new ArrayList<>(other.moveHistory);
        this.consecutivePasses = other.consecutivePasses;
        this.blackCaptured = other.blackCaptured;
        this.whiteCaptured = other.whiteCaptured;
        this.komi = other.komi;
        this.gameOver = other.gameOver;
        this.endGameReason = other.endGameReason;
        this.blackTimeLeft = other.blackTimeLeft;
        this.whiteTimeLeft = other.whiteTimeLeft;
        this.timeControl = other.timeControl;
    }

    public float getKomi() { return komi; }
    public BoardState getBoardState() { return boardState; }
    public BoardState getPreviousBoardState() { return previousBoardState; }
    public Stone getCurrentPlayer() { return currentPlayer; }
    public List<Move> getMoveHistory() { return new ArrayList<>(moveHistory); }
    public int getConsecutivePasses() { return consecutivePasses; }
    public boolean isGameOver() { return gameOver; }
    public String getEndGameReason() { return endGameReason; }
    public int getBlackCaptured() { return blackCaptured; }
    public int getWhiteCaptured() { return whiteCaptured; }
    public long getBlackTimeLeft() { return blackTimeLeft; }
    public long getWhiteTimeLeft() { return whiteTimeLeft; }
    public TimeControl getTimeControl() { return timeControl; }

    public void setCurrentPlayer(Stone currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void recordMove(Move move, BoardState nextBoardState, int capturedCount) {
        this.previousBoardState = this.boardState.copy();
        this.boardState = nextBoardState;
        moveHistory.add(move);

        if (move.isPass()) {
            consecutivePasses++;
        } else {
            consecutivePasses = 0;
        }

        if (move.getColor() == Stone.BLACK) {
            whiteCaptured += capturedCount;
        } else if (move.getColor() == Stone.WHITE) {
            blackCaptured += capturedCount;
        }

        if (!move.isResign()) {
            this.currentPlayer = this.currentPlayer.opponent();
        }

        this.gameOver = checkGameOverCondition();
        if (this.gameOver) {
            updateEndGameReason(move);
        }
    }

    public void setGameOver(boolean isOver, String reason) {
        this.gameOver = isOver;
        this.endGameReason = reason;
    }

    /**
     * Cập nhật thời gian còn lại sau mỗi nước đi.
     * @param player Người chơi vừa đi.
     * @param timeSpent Thời gian đã dùng (ms).
     */
    public void updateTime(Stone player, long timeSpent) {
        if (gameOver) return; // Không cập nhật nếu game over

        if (player == Stone.BLACK) {
            blackTimeLeft = Math.max(0, blackTimeLeft - timeSpent);
            if (blackTimeLeft == 0 && timeControl != TimeControl.CANADIAN) {
                setGameOver(true, "Black ran out of time. White wins.");
            }
        } else {
            whiteTimeLeft = Math.max(0, whiteTimeLeft - timeSpent);
            if (whiteTimeLeft == 0 && timeControl != TimeControl.CANADIAN) {
                setGameOver(true, "White ran out of time. Black wins.");
            }
        }
    }

    /**
     * Đặt lại thời gian cho người chơi trong giai đoạn Canadian.
     * @param player Người chơi cần reset (BLACK hoặc WHITE).
     * @param periodTime Thời gian mới cho giai đoạn (ms).
     */
    public void resetTimePeriod(Stone player, long periodTime) {
        if (gameOver) return; // Không reset nếu game over

        if (player == Stone.BLACK) {
            blackTimeLeft = Math.max(0, periodTime);
        } else if (player == Stone.WHITE) {
            whiteTimeLeft = Math.max(0, periodTime);
        }
    }

    /**
     * Hoàn tác nước đi cuối cùng.
     * @return true nếu undo thành công, false nếu không có nước đi để undo.
     */
    public boolean undoLastMove() {
        if (moveHistory.isEmpty() || gameOver) {
            return false;
        }

        moveHistory.remove(moveHistory.size() - 1);
        if (previousBoardState != null) {
            boardState = previousBoardState;
            previousBoardState = null; // Cần logic phức tạp hơn nếu muốn undo nhiều lần
        }
        consecutivePasses = moveHistory.stream().filter(Move::isPass).mapToInt(m -> 1).sum();
        currentPlayer = currentPlayer.opponent();
        // Không khôi phục captured vì chưa lưu lịch sử captured
        gameOver = false;
        endGameReason = "";
        return true;
    }

    private boolean checkGameOverCondition() {
        return consecutivePasses >= 2 || moveHistory.stream().anyMatch(Move::isResign) ||
                (blackTimeLeft <= 0 && timeControl != TimeControl.CANADIAN) ||
                (whiteTimeLeft <= 0 && timeControl != TimeControl.CANADIAN);
    }

    private void updateEndGameReason(Move lastMove) {
        if (blackTimeLeft <= 0 && timeControl != TimeControl.CANADIAN) {
            this.endGameReason = "Black ran out of time. White wins.";
        } else if (whiteTimeLeft <= 0 && timeControl != TimeControl.CANADIAN) {
            this.endGameReason = "White ran out of time. Black wins.";
        } else if (consecutivePasses >= 2) {
            this.endGameReason = "Two consecutive passes";
        } else if (lastMove != null && lastMove.isResign()) {
            Stone resigningPlayer = lastMove.getColor();
            this.endGameReason = resigningPlayer + " resigned. " + resigningPlayer.opponent() + " wins.";
        } else {
            this.endGameReason = "";
        }
    }
}