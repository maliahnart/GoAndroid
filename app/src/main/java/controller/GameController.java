package controller;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import config.GameConfig;
import config.GameMode;
import config.TimeControl;
import model.GameLogic;
import model.GameState;
import model.Move;
import model.Point;
import model.Stone;
import model.player.BotPlayer;
import model.player.HumanPlayer;
import model.player.Player;
import model.ai.RandomAIStrategy;
import view.BoardView;
import view.GameActivity;
import view.GameInfoFragment;

public class GameController {
    private static final String TAG = "GameController";
    private final GameActivity gameActivity;
    private final BoardView boardView;
    private final GameInfoFragment gameInfoFragment;
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameConfig config;
    private final Player blackPlayer;
    private final Player whitePlayer;
    private final Handler handler;
    private long lastMoveTime;
    private int blackMovesInPeriod;
    private int whiteMovesInPeriod;
    private static final int MOVES_PER_PERIOD = 25;

    public GameController(GameActivity activity, BoardView boardView, GameInfoFragment fragment, GameConfig config) {
        if (activity == null || boardView == null || fragment == null || config == null) {
            throw new IllegalArgumentException("GameController parameters cannot be null");
        }

        this.gameActivity = activity;
        this.boardView = boardView;
        this.gameInfoFragment = fragment;
        this.gameState = new GameState(config);
        this.gameLogic = new GameLogic();
        this.config = config;
        this.handler = new Handler(Looper.getMainLooper());
        this.lastMoveTime = System.currentTimeMillis();
        this.blackMovesInPeriod = 0;
        this.whiteMovesInPeriod = 0;

        // Khởi tạo người chơi
        switch (config.getGameMode()) {
            case PVP:
                blackPlayer = new HumanPlayer(Stone.BLACK);
                whitePlayer = new HumanPlayer(Stone.WHITE);
                break;
            case PVB_EASY:
                blackPlayer = new HumanPlayer(Stone.BLACK);
                whitePlayer = new BotPlayer(Stone.WHITE, new RandomAIStrategy());
                break;
            default:
                throw new IllegalArgumentException("Unsupported game mode: " + config.getGameMode());
        }

        // Thiết lập BoardView
        try {
            boardView.setBoardState(gameState.getBoardState());
            boardView.setOnStonePlacedListener(this::handleStonePlacement);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up BoardView: " + e.getMessage(), e);
            throw e;
        }

        updateGameInfo();
    }

    private void handleStonePlacement(Point point) {
        if (gameState.isGameOver()) {
            showToast("Game is over!");
            return;
        }

        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        if (currentPlayer instanceof HumanPlayer) {
            Move move = new Move(point, currentPlayer.getColor());
            if (gameLogic.isValidMove(move, gameState)) {
                long timeSpent = System.currentTimeMillis() - lastMoveTime;
                applyMove(move, timeSpent);
                if (!gameState.isGameOver()) {
                    playBotMoveIfNeeded();
                }
            } else {
                showToast("Invalid move!");
            }
        }
    }

    public void handlePass() {
        if (gameState.isGameOver()) {
            showToast("Game is over!");
            return;
        }

        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        Move passMove = new Move(null, currentPlayer.getColor(), true, false);
        long timeSpent = System.currentTimeMillis() - lastMoveTime;
        applyMove(passMove, timeSpent);
        if (!gameState.isGameOver()) {
            playBotMoveIfNeeded();
        }
    }

    public void handleResign() {
        if (gameState.isGameOver()) {
            showToast("Game is over!");
            return;
        }

        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        Move resignMove = new Move(null, currentPlayer.getColor(), false, true);
        long timeSpent = System.currentTimeMillis() - lastMoveTime;
        applyMove(resignMove, timeSpent);
    }

    public void handleUndo() {
        if (gameState.isGameOver()) {
            showToast("Cannot undo: Game is over!");
            return;
        }

        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        if (currentPlayer instanceof BotPlayer) {
            showToast("Cannot undo during bot's turn!");
            return;
        }

        if (gameState.undoLastMove()) {
            // Giảm số nước trong giai đoạn Canadian
            if (config.getTimeControl() == TimeControl.CANADIAN) {
                if (currentPlayer.getColor() == Stone.BLACK) {
                    blackMovesInPeriod = Math.max(0, blackMovesInPeriod - 1);
                } else {
                    whiteMovesInPeriod = Math.max(0, whiteMovesInPeriod - 1);
                }
            }
            boardView.setBoardState(gameState.getBoardState());
            boardView.invalidate();
            updateGameInfo();
            showToast("Move undone");
        } else {
            showToast("No moves to undo!");
        }
    }

    private void applyMove(Move move, long timeSpent) {
        try {
            GameLogic.CapturedResult result = gameLogic.calculateNextBoardState(move, gameState.getBoardState());
            gameState.recordMove(move, result.getBoardState(), result.getCapturedCount());
            if (!move.isResign()) {
                updateTime(move.getColor(), timeSpent);
            }
            boardView.setBoardState(gameState.getBoardState());
            boardView.invalidate();
            updateGameInfo();
            lastMoveTime = System.currentTimeMillis();

            if (gameState.isGameOver()) {
                handleGameOver();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying move: " + e.getMessage(), e);
            showToast("Error applying move!");
        }
    }

    private void updateTime(Stone player, long timeSpent) {
        if (gameState.isGameOver()) return;

        if (config.getTimeControl() == TimeControl.CANADIAN) {
            if (player == Stone.BLACK) {
                blackMovesInPeriod++;
                if (blackMovesInPeriod >= MOVES_PER_PERIOD) {
                    gameState.resetTimePeriod(player, config.getTimeLimit() * 1000L);
                    blackMovesInPeriod = 0;
                    showToast("Black: New period started");
                }
            } else {
                whiteMovesInPeriod++;
                if (whiteMovesInPeriod >= MOVES_PER_PERIOD) {
                    gameState.resetTimePeriod(player, config.getTimeLimit() * 1000L);
                    whiteMovesInPeriod = 0;
                    showToast("White: New period started");
                }
            }
        }
        gameState.updateTime(player, timeSpent);
    }

    private void playBotMoveIfNeeded() {
        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        if (currentPlayer instanceof BotPlayer) {
            handler.post(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    Move botMove = currentPlayer.generateMove(gameState);
                    long timeSpent = System.currentTimeMillis() - startTime;
                    if (gameLogic.isValidMove(botMove, gameState)) {
                        applyMove(botMove, timeSpent);
                    } else {
                        Log.e(TAG, "Bot generated invalid move: " + botMove);
                        showToast("Bot generated an invalid move!");
                        gameState.setGameOver(true, "Bot error");
                        handleGameOver();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Bot failed to generate move: " + e.getMessage(), e);
                    showToast("Bot failed to generate move!");
                    gameState.setGameOver(true, "Bot error: " + e.getMessage());
                    handleGameOver();
                }
            });
        }
    }

    private void updateGameInfo() {
        try {
            int blackMovesLeft = MOVES_PER_PERIOD - blackMovesInPeriod;
            int whiteMovesLeft = MOVES_PER_PERIOD - whiteMovesInPeriod;
            gameInfoFragment.updateGameInfo(gameState, blackMovesLeft, whiteMovesLeft);
        } catch (Exception e) {
            Log.e(TAG, "Error updating game info: " + e.getMessage(), e);
        }
    }

    private void handleGameOver() {
        try {
            GameLogic.Score score = gameLogic.calculateScore(gameState, config.getScoringRule());
            String reason = gameState.getEndGameReason();
            String message = String.format("Game Over: %s\nScore: Black %.1f - White %.1f",
                    reason.isEmpty() ? "Unknown reason" : reason,
                    score.blackScore, score.whiteScore);
            gameActivity.showGameOverDialog(message,
                    () -> gameActivity.restartGame(config),
                    () -> gameActivity.finish());
        } catch (Exception e) {
            Log.e(TAG, "Error handling game over: " + e.getMessage(), e);
            showToast("Error displaying game over!");
        }
    }

    private void showToast(String message) {
        gameActivity.runOnUiThread(() -> Toast.makeText(gameActivity, message, Toast.LENGTH_SHORT).show());
    }
}