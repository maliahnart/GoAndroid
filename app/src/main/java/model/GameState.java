package model;

import config.GameConfig;
import config.TimeControl;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private BoardState boardState;
    private final List<Move> moveHistory;
    private Stone currentPlayer;
    private int consecutivePasses;
    private int blackCaptured;
    private int whiteCaptured;
    private final float komi;
    private boolean gameOver;
    private String endGameReason;
    private long blackTimeLeft; // Time left for Black (ms)
    private long whiteTimeLeft; // Time left for White (ms)
    private final TimeControl timeControl; // TimeControl from config

    public GameState(GameConfig config) {
        this.boardState = new BoardState(config);
        this.moveHistory = new ArrayList<>();
        this.currentPlayer = Stone.BLACK;
        this.consecutivePasses = 0;
        this.blackCaptured = 0;
        this.whiteCaptured = 0;
        this.komi = config.getKomi();
        this.gameOver = false;
        this.endGameReason = "";
        this.blackTimeLeft = config.getTimeLimit() * 1000L; // Seconds to ms
        this.whiteTimeLeft = config.getTimeLimit() * 1000L;
        this.timeControl = config.getTimeControl();
    }

    // Copy constructor
    public GameState(GameState other) {
        this.boardState = other.boardState.copy();
        this.moveHistory = new ArrayList<>(other.moveHistory);
        this.currentPlayer = other.currentPlayer;
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
        if (move == null || nextBoardState == null) {
            throw new IllegalArgumentException("Move or nextBoardState cannot be null");
        }
        moveHistory.add(move);
        this.boardState = nextBoardState;

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
        this.endGameReason = reason != null ? reason : "";
    }

    /**
     * Update time left after a move.
     * @param player Player who moved.
     * @param timeSpent Time used (ms).
     */
    public void updateTime(Stone player, long timeSpent) {
        if (gameOver) return;

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
     * Reset time for a player in Canadian time control.
     * @param player Player to reset (BLACK or WHITE).
     * @param periodTime New period time (ms).
     */
    public void resetTimePeriod(Stone player, long periodTime) {
        if (gameOver) return;

        if (player == Stone.BLACK) {
            blackTimeLeft = Math.max(0, periodTime);
        } else if (player == Stone.WHITE) {
            whiteTimeLeft = Math.max(0, periodTime);
        }
    }

    /**
     * Undo the last move, restoring board state and captured stones.
     * @return true if undo successful, false if no moves to undo.
     */
    public boolean undoLastMove() {
        if (moveHistory.isEmpty() || gameOver) {
            return false;
        }

        Move lastMove = moveHistory.remove(moveHistory.size() - 1);
        BoardState previousState = lastMove.getBoardState();
        if (previousState == null) {
            return false;
        }

        // Restore board state
        this.boardState = previousState;

        // Update captured counts
        int capturedCount = lastMove.getCapturedStones().size();
        if (lastMove.getColor() == Stone.BLACK) {
            whiteCaptured = Math.max(0, whiteCaptured - capturedCount);
        } else if (lastMove.getColor() == Stone.WHITE) {
            blackCaptured = Math.max(0, blackCaptured - capturedCount);
        }

        // Update game state
        consecutivePasses = moveHistory.stream().filter(Move::isPass).mapToInt(m -> 1).sum();
        currentPlayer = lastMove.getColor();
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