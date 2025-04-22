package controller;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import config.GameConfig;
import config.GameMode;
import config.ScoringRule;
import config.TimeControl;
import model.BoardState;
import model.GameLogic;
import model.GameState;
import model.Move;
import model.Point;
import model.Stone;
import model.ai.AIMoveCallback;
import model.ai.AlphaBetaStrategy;
import model.ai.BasicHeuristicStrategy;
//import model.ai.MCTSStrategy;
import model.ai.RandomAIStrategy;
import model.player.BotPlayer;
import model.player.HumanPlayer;
import model.player.Player;
import view.BoardView;
import view.GameActivity;
import view.GameInfoFragment;
import view.assets.SoundLoader;
import com.example.gogameproject.R;

public class GameController {
    private static final String TAG = "GameController";
    private static final long UI_UPDATE_INTERVAL_MS = 100;
    private static final int MOVES_PER_PERIOD = 25;

    private final GameActivity gameActivity;
    private final BoardView boardView;
    private final GameInfoFragment gameInfoFragment;
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameConfig config;
    private final SoundLoader soundLoader;
    private final Player blackPlayer;
    private final Player whitePlayer;
    private final Handler mainThreadHandler;
    private final Runnable timeUpdateRunnable;
    private boolean isTimeUpdateRunning;
    private long turnStartTime;
    private int blackMovesInPeriod;
    private int whiteMovesInPeriod;

    public GameController(GameActivity activity, BoardView boardView, GameInfoFragment fragment, GameConfig config) {
        if (activity == null || boardView == null || fragment == null || config == null) {
            throw new IllegalArgumentException("GameController parameters cannot be null");
        }
        Log.d(TAG, "Initializing GameController...");

        this.gameActivity = activity;
        this.boardView = boardView;
        this.gameInfoFragment = fragment;
        this.config = config;
        this.gameState = new GameState(config);
        this.gameLogic = new GameLogic();
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
        this.turnStartTime = System.currentTimeMillis();
        this.blackMovesInPeriod = 0;
        this.whiteMovesInPeriod = 0;
        this.isTimeUpdateRunning = false;

        this.soundLoader = new SoundLoader(activity);
        try {
            soundLoader.loadSound(R.raw.stone_click);
            Log.d(TAG, "SoundLoader initialized.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load sound", e);
            showToast("Warning: Cannot load sound effect!");
        }

        this.blackPlayer = new HumanPlayer(Stone.BLACK);
        switch (config.getGameMode()) {
            case PVP:
                this.whitePlayer = new HumanPlayer(Stone.WHITE);
                break;
            case PVB_EASY:
                this.whitePlayer = new BotPlayer(Stone.WHITE, new RandomAIStrategy());
                break;
            case PVB_MEDIUM:
                this.whitePlayer = new BotPlayer(Stone.WHITE, new BasicHeuristicStrategy());
                break;
            default:
                this.whitePlayer = new BotPlayer(Stone.WHITE, new AlphaBetaStrategy());
        }
        Log.d(TAG, "Players: Black=" + blackPlayer.getClass().getSimpleName() +
                ", White=" + whitePlayer.getClass().getSimpleName());

        try {
            this.boardView.setBoardState(gameState.getBoardState());
            this.boardView.setOnStonePlacedListener(this::handleHumanPlacement);
            Log.d(TAG, "BoardView setup complete.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up BoardView", e);
            showToast("Error initializing board!");
            throw e;
        }

        this.timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimeUpdateRunning && gameState != null && !gameState.isGameOver()) {
                    updateGameInfoUI();
                    mainThreadHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
                }
            }
        };

        startTurn();
        Log.d(TAG, "GameController initialized.");
    }

    private void startTurn() {
        if (gameState == null || gameState.isGameOver()) {
            Log.d(TAG, "startTurn: Game is over or gameState is null.");
            stopTimeUpdate();
            return;
        }
        Log.d(TAG, "Starting turn for: " + gameState.getCurrentPlayer());
        this.turnStartTime = System.currentTimeMillis();
        startTimeUpdate();

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer instanceof BotPlayer) {
            playBotMoveIfNeeded();
        }
    }

    private void startTimeUpdate() {
        if (!isTimeUpdateRunning) {
            isTimeUpdateRunning = true;
            mainThreadHandler.post(timeUpdateRunnable);
            Log.d(TAG, "Time update started.");
        }
    }

    private void stopTimeUpdate() {
        if (isTimeUpdateRunning) {
            isTimeUpdateRunning = false;
            mainThreadHandler.removeCallbacks(timeUpdateRunnable);
            Log.d(TAG, "Time update stopped.");
        }
    }

    private void handleHumanPlacement(Point point) {
        Log.d(TAG, "handleHumanPlacement at: " + point);
        if (gameState == null || gameState.isGameOver()) {
            showToast("Game is over!");
            return;
        }
        if (!(getCurrentPlayer() instanceof HumanPlayer)) {
            showToast("Not your turn!");
            return;
        }
        if (point == null) {
            Log.e(TAG, "Null point in handleHumanPlacement");
            showToast("Invalid placement!");
            return;
        }

        Move move = new Move(point, gameState.getCurrentPlayer(), gameState.getBoardState(), new ArrayList<>());
        if (gameLogic != null && gameLogic.isValidMove(move, gameState)) {
            applyUserMove(move);
        } else {
            showToast("Invalid move!");
        }
    }

    public void handlePass() {
        Log.d(TAG, "handlePass called.");
        if (gameState == null || gameState.isGameOver()) {
            showToast("Game is over!");
            return;
        }
        if (!(getCurrentPlayer() instanceof HumanPlayer)) {
            showToast("Not your turn!");
            return;
        }
        Move passMove = new Move(null, gameState.getCurrentPlayer(), true, false, gameState.getBoardState(), new ArrayList<>());
        applyUserMove(passMove);
    }

    public void handleResign() {
        Log.d(TAG, "handleResign called.");
        if (gameState == null || gameState.isGameOver()) {
            showToast("Game is already over!");
            return;
        }
        if (!(getCurrentPlayer() instanceof HumanPlayer)) {
            showToast("Not your turn!");
            return;
        }
        Move resignMove = new Move(null, gameState.getCurrentPlayer(), false, true, gameState.getBoardState(), new ArrayList<>());
        applyValidatedMove(resignMove, 0);
    }

    public void handleUndo() {
        Log.d(TAG, "handleUndo called.");
        if (gameState == null || gameState.isGameOver()) {
            showToast("Cannot undo: Game is over!");
            return;
        }
        if (!(getCurrentPlayer() instanceof HumanPlayer)) {
            showToast("Cannot undo: Not your turn!");
            return;
        }

        List<Move> moveHistory = gameState.getMoveHistory();
        if (moveHistory == null || moveHistory.isEmpty()) {
            showToast("No moves to undo!");
            return;
        }

        if (config.getGameMode() == GameMode.PVP) {
            if (gameState.undoLastMove()) {
                updateTimeControlAfterUndo(moveHistory.get(moveHistory.size() - 1).getColor());
                boardView.setBoardState(gameState.getBoardState());
                boardView.invalidate();
                updateGameInfoUI();
                showToast("Move undone");
                startTurn();
            } else {
                showToast("Failed to undo move!");
                Log.e(TAG, "Undo failed in PVP mode");
            }
        } else {
            if (moveHistory.size() >= 2) {
                boolean undoneBot = gameState.undoLastMove(); // Undo bot's move
                boolean undoneHuman = gameState.undoLastMove(); // Undo human's move
                if (undoneBot && undoneHuman) {
                    updateTimeControlAfterUndo(gameState.getCurrentPlayer());
                    boardView.setBoardState(gameState.getBoardState());
                    boardView.invalidate();
                    updateGameInfoUI();
                    showToast("Moves undone");
                    startTurn();
                } else {
                    showToast("Failed to undo moves!");
                    Log.e(TAG, "Undo failed: bot=" + undoneBot + ", human=" + undoneHuman);
                }
            } else if (moveHistory.size() == 1) {
                if (gameState.undoLastMove()) {
                    updateTimeControlAfterUndo(gameState.getCurrentPlayer());
                    boardView.setBoardState(gameState.getBoardState());
                    boardView.invalidate();
                    updateGameInfoUI();
                    showToast("Move undone");
                    startTurn();
                } else {
                    showToast("Failed to undo move!");
                    Log.e(TAG, "Undo failed for single move");
                }
            } else {
                showToast("No moves to undo!");
            }
        }
    }

    private void updateTimeControlAfterUndo(Stone undonePlayer) {
        if (config.getTimeControl() == TimeControl.CANADIAN) {
            if (undonePlayer == Stone.BLACK) {
                blackMovesInPeriod = Math.max(0, blackMovesInPeriod - 1);
                if (blackMovesInPeriod == MOVES_PER_PERIOD - 1) {
                    gameState.resetTimePeriod(Stone.BLACK, config.getTimeLimit() * 1000L);
                    Log.d(TAG, "Black time period reset after undo");
                }
            } else if (undonePlayer == Stone.WHITE) {
                whiteMovesInPeriod = Math.max(0, whiteMovesInPeriod - 1);
                if (whiteMovesInPeriod == MOVES_PER_PERIOD - 1) {
                    gameState.resetTimePeriod(Stone.WHITE, config.getTimeLimit() * 1000L);
                    Log.d(TAG, "White time period reset after undo");
                }
            }
        }
    }

    private void applyUserMove(Move move) {
        if (move == null) {
            Log.e(TAG, "Null move in applyUserMove");
            showToast("Error: Invalid move!");
            return;
        }
        long timeSpentMillis = System.currentTimeMillis() - turnStartTime;
        Log.d(TAG, "Applying user move: " + move + ", Time spent: " + timeSpentMillis + "ms");
        applyValidatedMove(move, timeSpentMillis);
    }

    private void applyValidatedMove(Move move, long timeSpentMillis) {
        try {
            if (gameState == null || move == null || move.getColor() == null) {
                Log.e(TAG, "Invalid input: gameState=" + gameState + ", move=" + move);
                showToast("Error: Invalid game state or move!");
                return;
            }
            Stone playerColor = move.getColor();
            GameLogic.CapturedResult result = gameLogic != null ? gameLogic.calculateNextBoardState(move, gameState.getBoardState()) : null;
            if (result == null || result.getBoardState() == null) {
                Log.e(TAG, "Invalid move result: " + move);
                showToast("Error: Invalid move result!");
                return;
            }

            // Create a new Move with captured stones
            Move recordedMove = new Move(
                    move.getPoint(),
                    move.getColor(),
                    move.isPass(),
                    move.isResign(),
                    gameState.getBoardState(),
                    result.getCapturedPoints()
            );
            gameState.recordMove(recordedMove, result.getBoardState(), result.getCapturedCount());
            Log.d(TAG, "Recorded move. Captured: " + result.getCapturedCount());

            if (!move.isPass() && !move.isResign()) {
                try {
                    soundLoader.playSound();
                    Log.d(TAG, "Played stone_click sound");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to play sound", e);
                }
            }

            if (!move.isResign()) {
                if (config.getTimeControl() == TimeControl.CANADIAN) {
                    if (playerColor == Stone.BLACK) {
                        blackMovesInPeriod++;
                        if (blackMovesInPeriod >= MOVES_PER_PERIOD) {
                            gameState.resetTimePeriod(playerColor, config.getTimeLimit() * 1000L);
                            blackMovesInPeriod = 0;
                            Log.d(TAG, "Black: New period started");
                        }
                    } else {
                        whiteMovesInPeriod++;
                        if (whiteMovesInPeriod >= MOVES_PER_PERIOD) {
                            gameState.resetTimePeriod(playerColor, config.getTimeLimit() * 1000L);
                            whiteMovesInPeriod = 0;
                            Log.d(TAG, "White: New period started");
                        }
                    }
                }
                gameState.updateTime(playerColor, timeSpentMillis);
                Log.d(TAG, String.format("Time updated for %s. Remaining: B=%dms, W=%dms",
                        playerColor, gameState.getBlackTimeLeft(), gameState.getWhiteTimeLeft()));

                if (gameState.getBlackTimeLeft() <= 0) {
                    gameState.setGameOver(true, "Black ran out of time. White wins!");
                    handleGameOver();
                    return;
                } else if (gameState.getWhiteTimeLeft() <= 0) {
                    gameState.setGameOver(true, "White ran out of time. Black wins!");
                    handleGameOver();
                    return;
                }
            }

            boardView.setBoardState(gameState.getBoardState());
            boardView.invalidate();

            List<Move> moveHistory = gameState.getMoveHistory();
            if (move.isPass() && moveHistory != null && moveHistory.size() >= 2 &&
                    moveHistory.get(moveHistory.size() - 1).isPass() &&
                    moveHistory.get(moveHistory.size() - 2).isPass()) {
                GameLogic.Score score = gameLogic != null ? gameLogic.calculateScore(gameState, config.getScoringRule()) : null;
                String endGameReason;
                if (score == null) {
                    endGameReason = "Both players passed. Cannot determine winner.";
                } else {
                    if (score.blackScore > score.whiteScore) {
                        endGameReason = "Both players passed. Black wins!";
                    } else if (score.whiteScore > score.blackScore) {
                        endGameReason = "Both players passed. White wins!";
                    } else {
                        endGameReason = "Both players passed. Draw!";
                    }
                }
                gameState.setGameOver(true, endGameReason);
                Log.i(TAG, "Game over: " + endGameReason);
            }

            if (gameState.isGameOver()) {
                handleGameOver();
            } else {
                startTurn();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying move: " + move, e);
            showToast("Error applying move!");
        }
    }

    private void playBotMoveIfNeeded() {
        if (gameState == null || gameState.isGameOver()) {
            Log.d(TAG, "Skipping bot move: Game is over or gameState is null.");
            return;
        }
        Player currentPlayer = getCurrentPlayer();
        if (!(currentPlayer instanceof BotPlayer)) {
            Log.d(TAG, "Skipping bot move: Not bot's turn.");
            return;
        }

        Log.i(TAG, "Bot's turn (" + currentPlayer.getColor() + "). Requesting move...");
        BotPlayer bot = (BotPlayer) currentPlayer;
        try {
            bot.generateMove(gameState, botMove -> mainThreadHandler.post(() -> {
                try {
                    if (gameState.isGameOver()) {
                        Log.w(TAG, "Game ended before applying bot move.");
                        return;
                    }
                    if (botMove == null) {
                        Log.e(TAG, "Bot returned null move");
                        showToast("Bot Error: Null move!");
                        gameState.setGameOver(true, "Bot Error: Null Move");
                        handleGameOver();
                        return;
                    }
                    long botTimeSpentMillis = System.currentTimeMillis() - turnStartTime;
                    Log.i(TAG, "Applying bot move: " + botMove + ", time spent: " + botTimeSpentMillis + "ms");
                    if (gameLogic != null && gameLogic.isValidMove(botMove, gameState)) {
                        applyValidatedMove(botMove, botTimeSpentMillis);
                    } else {
                        Log.e(TAG, "Bot generated invalid move: " + botMove);
                        showToast("Bot error: Invalid move!");
                        gameState.setGameOver(true, "Bot Error: Invalid Move");
                        handleGameOver();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error applying bot move: " + botMove, e);
                    showToast("Bot Error: Failed to apply move!");
                    gameState.setGameOver(true, "Bot Error: " + e.getMessage());
                    handleGameOver();
                }
            }));
        } catch (Exception e) {
            Log.e(TAG, "Error requesting bot move", e);
            showToast("Bot Error: Failed to generate move!");
            gameState.setGameOver(true, "Bot Error: " + e.getMessage());
            handleGameOver();
        }
    }

    private void updateGameInfoUI() {
        if (gameInfoFragment == null || gameState == null) {
            Log.e(TAG, "Cannot update UI: gameInfoFragment or gameState is null");
            return;
        }
        try {
            Stone currentPlayer = gameState.getCurrentPlayer();
            long elapsedMillis = System.currentTimeMillis() - turnStartTime;
            long currentTimeLeft = currentPlayer == Stone.BLACK
                    ? gameState.getBlackTimeLeft() - elapsedMillis
                    : gameState.getWhiteTimeLeft() - elapsedMillis;
            currentTimeLeft = Math.max(0, currentTimeLeft);

            int blackMovesLeft = config.getTimeControl() == TimeControl.CANADIAN ? MOVES_PER_PERIOD - blackMovesInPeriod : -1;
            int whiteMovesLeft = config.getTimeControl() == TimeControl.CANADIAN ? MOVES_PER_PERIOD - whiteMovesInPeriod : -1;

            gameInfoFragment.updateGameInfo(gameState, blackMovesLeft, whiteMovesLeft, currentTimeLeft);
            Log.d(TAG, String.format("UI updated: Current=%s, Time left=%dms, Black moves=%d, White moves=%d",
                    currentPlayer, currentTimeLeft, blackMovesLeft, whiteMovesLeft));
        } catch (Exception e) {
            Log.e(TAG, "Error updating GameInfoFragment", e);
        }
    }

    private void handleGameOver() {
        stopTimeUpdate();
        try {
            soundLoader.release();
            Log.d(TAG, "SoundLoader released.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to release SoundLoader", e);
        }

        mainThreadHandler.post(() -> {
            try {
                if (gameState == null) {
                    Log.e(TAG, "gameState is null in handleGameOver");
                    showToast("Error: Game state is null!");
                    return;
                }
                Log.w(TAG, "Calculating score for final display...");
                GameLogic.Score score = gameLogic != null ? gameLogic.calculateScore(gameState, config.getScoringRule()) : null;
                if (score == null) {
                    Log.e(TAG, "Score is null in handleGameOver");
                    showToast("Error: Cannot calculate score!");
                    return;
                }
                Log.i(TAG, "Game end! Score: Black=" + score.blackScore + ", White=" + score.whiteScore);

                String reason = gameState.getEndGameReason();
                if (reason == null || reason.isEmpty()) {
                    reason = "Game ended.";
                }

                String message = String.format("%s\nScore: Black %.1f - White %.1f",
                        reason, score.blackScore, score.whiteScore);

                if (gameActivity != null) {
                    gameActivity.showGameOverDialog(message,
                            () -> gameActivity.restartGame(config),
                            () -> gameActivity.finish());
                    Log.i(TAG, "Game over dialog displayed: " + message);
                } else {
                    Log.e(TAG, "gameActivity is null in handleGameOver");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during game over handling", e);
                showToast("Error showing game over!");
            }
        });
    }

    private Player getCurrentPlayer() {
        if (gameState == null) {
            Log.e(TAG, "gameState is null in getCurrentPlayer");
            return blackPlayer;
        }
        return gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
    }

    private void showToast(String message) {
        if (gameActivity != null) {
            gameActivity.runOnUiThread(() -> Toast.makeText(gameActivity, message, Toast.LENGTH_SHORT).show());
        } else {
            Log.e(TAG, "gameActivity is null in showToast");
        }
    }
}