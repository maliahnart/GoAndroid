package controller;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
import model.ai.BasicHeuristicStrategy;
import model.player.BotPlayer;
import model.player.HumanPlayer;
import model.player.Player;
import model.ai.RandomAIStrategy;
import view.BoardView;
import view.GameActivity;
import view.GameInfoFragment;

public class GameController {
    private static final String TAG = "GameController";
    private static final long BOT_MOVE_DELAY_MS = 0; // Độ trễ để bot đi
    private static final int MOVES_PER_PERIOD = 25; // Cho Canadian timing

    // Tham chiếu đến View và Model
    private final GameActivity gameActivity;
    private final BoardView boardView;
    private final GameInfoFragment gameInfoFragment;
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameConfig config;

    // Người chơi
    private final Player blackPlayer;
    private final Player whitePlayer;

    // Threading
    private final Handler mainThreadHandler;

    // Thời gian
    private long turnStartTime;
    private int blackMovesInPeriod;
    private int whiteMovesInPeriod;

    public GameController(GameActivity activity, BoardView boardView, GameInfoFragment fragment, GameConfig config) {
        if (activity == null || boardView == null || fragment == null || config == null) {
            throw new IllegalArgumentException("GameController constructor parameters cannot be null");
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

        // Khởi tạo người chơi
        this.blackPlayer = new BotPlayer(Stone.BLACK, new RandomAIStrategy());
        if (config.getGameMode() == GameMode.PVP) {
            this.whitePlayer = new HumanPlayer(Stone.WHITE);
        } else if (config.getGameMode() == GameMode.PVB_EASY) { // PVB_EASY
            this.whitePlayer = new BotPlayer(Stone.WHITE, new RandomAIStrategy());
        }
        else{
            this.whitePlayer = new BotPlayer(Stone.WHITE, new BasicHeuristicStrategy());
        }
        Log.d(TAG, "Players initialized: Black=" + blackPlayer.getClass().getSimpleName() +
                ", White=" + whitePlayer.getClass().getSimpleName());

        // Thiết lập BoardView
        try {
            this.boardView.setBoardState(gameState.getBoardState());
            this.boardView.setOnStonePlacedListener(this::handleHumanPlacement);
            Log.d(TAG, "BoardView setup complete.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up BoardView", e);
            showToast("Error initializing board!");
            throw e;
        }

        startTurn();
        Log.d(TAG, "GameController initialization finished.");
    }

    /** Bắt đầu lượt chơi: Cập nhật UI và kiểm tra bot */
    private void startTurn() {
        if (gameState.isGameOver()) {
            Log.d(TAG, "startTurn: Game is over.");
            return;
        }
        Log.d(TAG, "Starting turn for: " + gameState.getCurrentPlayer());
        this.turnStartTime = System.currentTimeMillis();

        updateGameInfoUI();

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer instanceof BotPlayer) {
            playBotMoveIfNeeded();
        } else {
            Log.d(TAG, "Human's turn. Waiting for input.");
        }
    }

    // Xử lý input người dùng

    private void handleHumanPlacement(Point point) {
        Log.d(TAG, "handleHumanPlacement at: " + point);
        if (!(getCurrentPlayer() instanceof HumanPlayer) || gameState.isGameOver()) {
            if (gameState.isGameOver()) showToast("Game is over!");
            else showToast("Not your turn!");
            return;
        }

        Move move = new Move(point, gameState.getCurrentPlayer());
        if (gameLogic.isValidMove(move, gameState)) {
            applyUserMove(move);
        } else {
            showToast("Invalid move!");
        }
    }

    public void handlePass() {
        Log.d(TAG, "handlePass called.");
        if (!(getCurrentPlayer() instanceof HumanPlayer) || gameState.isGameOver()) {
            if (gameState.isGameOver()) showToast("Game is over!");
            else showToast("Not your turn!");
            return;
        }
        Move passMove = new Move(null, gameState.getCurrentPlayer(), true, false);
        applyUserMove(passMove);
    }

    public void handleResign() {
        Log.d(TAG, "handleResign called.");
        if (!(getCurrentPlayer() instanceof HumanPlayer) || gameState.isGameOver()) {
            if (gameState.isGameOver()) showToast("Game is already over!");
            else showToast("Not your turn!");
            return;
        }
        Move resignMove = new Move(null, gameState.getCurrentPlayer(), false, true);
        applyValidatedMove(resignMove, 0);
    }

    public void handleUndo() {
        Log.d(TAG, "handleUndo called.");
        if (gameState.isGameOver()) {
            showToast("Cannot undo: Game is over!");
            return;
        }
        if (!(getCurrentPlayer() instanceof HumanPlayer)) {
            showToast("Cannot undo: Not your turn!");
            return;
        }

        if (config.getGameMode() == GameMode.PVB_EASY && !gameState.getMoveHistory().isEmpty()) {
            // Trong PVB, undo hai nước (bot và người) để giữ lượt người chơi
            boolean undoneBot = gameState.undoLastMove(); // Undo nước bot
            boolean undoneHuman = gameState.undoLastMove(); // Undo nước người
            if (undoneBot || undoneHuman) {
                // Cập nhật Canadian timing
                if (config.getTimeControl() == TimeControl.CANADIAN) {
                    if (gameState.getCurrentPlayer() == Stone.BLACK) {
                        blackMovesInPeriod = Math.max(0, blackMovesInPeriod - 1);
                    } else {
                        whiteMovesInPeriod = Math.max(0, whiteMovesInPeriod - 1);
                    }
                }
                boardView.setBoardState(gameState.getBoardState());
                boardView.invalidate();
                updateGameInfoUI();
                showToast("Move undone");
                // Không gọi startTurn() vì lượt đã đúng (người chơi)
            } else {
                showToast("No moves to undo!");
            }
        } else if (gameState.undoLastMove()) {
            // Trong PVP, chỉ undo một nước
            if (config.getTimeControl() == TimeControl.CANADIAN) {
                if (gameState.getCurrentPlayer() == Stone.BLACK) {
                    blackMovesInPeriod = Math.max(0, blackMovesInPeriod - 1);
                } else {
                    whiteMovesInPeriod = Math.max(0, whiteMovesInPeriod - 1);
                }
            }
            boardView.setBoardState(gameState.getBoardState());
            boardView.invalidate();
            updateGameInfoUI();
            showToast("Move undone");
            startTurn(); // Kích hoạt bot nếu lượt sau Undo là bot
        } else {
            showToast("No moves to undo!");
        }
    }

    // Logic nước đi

    private void applyUserMove(Move move) {
        long timeSpentMillis = System.currentTimeMillis() - turnStartTime;
        Log.d(TAG, "Applying user move: " + move + ", Time spent: " + timeSpentMillis + "ms");
        applyValidatedMove(move, timeSpentMillis);
    }

    private void applyValidatedMove(Move move, long timeSpentMillis) {
        try {
            Stone playerColor = move.getColor();
            GameLogic.CapturedResult result = gameLogic.calculateNextBoardState(move, gameState.getBoardState());
            gameState.recordMove(move, result.getBoardState(), result.getCapturedCount());
            Log.d(TAG, "GameState recorded move. Captured: " + result.getCapturedCount());

            if (!move.isResign()) {
                // Cập nhật thời gian
                if (config.getTimeControl() == TimeControl.CANADIAN) {
                    if (playerColor == Stone.BLACK) {
                        blackMovesInPeriod++;
                        if (blackMovesInPeriod >= MOVES_PER_PERIOD) {
                            gameState.resetTimePeriod(playerColor, config.getTimeLimit() * 1000L);
                            blackMovesInPeriod = 0;
                            showToast("Black: New period started");
                        }
                    } else {
                        whiteMovesInPeriod++;
                        if (whiteMovesInPeriod >= MOVES_PER_PERIOD) {
                            gameState.resetTimePeriod(playerColor, config.getTimeLimit() * 1000L);
                            whiteMovesInPeriod = 0;
                            showToast("White: New period started");
                        }
                    }
                }
                gameState.updateTime(playerColor, timeSpentMillis);
                Log.d(TAG, String.format("Time updated for %s. Remaining: B=%dms, W=%dms",
                        playerColor, gameState.getBlackTimeLeft(), gameState.getWhiteTimeLeft()));
            }

            boardView.setBoardState(gameState.getBoardState());
            boardView.invalidate();

            // Kiểm tra Pass liên tiếp
            List<Move> moveHistory = gameState.getMoveHistory();
            if (move.isPass() && moveHistory.size() >= 2 &&
                    moveHistory.get(moveHistory.size() - 1).isPass() &&
                    moveHistory.get(moveHistory.size() - 2).isPass()) {
                gameState.setGameOver(true, "Both players passed");
                Log.i(TAG, "Game over: Both players passed.");
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

    // Xử lý AI

    private void playBotMoveIfNeeded() {
        Player currentPlayer = getCurrentPlayer();
        if (!(currentPlayer instanceof BotPlayer) || gameState.isGameOver()) {
            return;
        }

        Log.i(TAG, "Bot's turn (" + currentPlayer.getColor() + "). Scheduling move...");
        mainThreadHandler.postDelayed(() -> {
            try {
                long botStartTime = System.currentTimeMillis();
                Move botMove = currentPlayer.generateMove(gameState);
                long botTimeSpentMillis = System.currentTimeMillis() - botStartTime;
                Log.i(TAG, "Bot generated move: " + botMove + " in " + botTimeSpentMillis + "ms");

                if (gameState.isGameOver()) {
                    Log.w(TAG, "Game ended while bot was preparing move.");
                    return;
                }

                if (gameLogic.isValidMove(botMove, gameState)) {
//                    showToast("Bot moved");
                    applyValidatedMove(botMove, botTimeSpentMillis);
                } else {
                    Log.e(TAG, "Bot generated invalid move: " + botMove);
                    showToast("Bot error: Invalid move!");
                    gameState.setGameOver(true, "Bot Error: Invalid Move");
                    handleGameOver();
                }
            } catch (Exception e) {
                Log.e(TAG, "Bot failed to generate move.", e);
                showToast("Bot Error: Failed to generate move!");
                gameState.setGameOver(true, "Bot Error");
                handleGameOver();
            }
        }, BOT_MOVE_DELAY_MS);
    }

    // Cập nhật giao diện

    private void updateGameInfoUI() {
        if (gameInfoFragment != null) {
            try {
                int blackMovesLeft = config.getTimeControl() == TimeControl.CANADIAN ? MOVES_PER_PERIOD - blackMovesInPeriod : -1;
                int whiteMovesLeft = config.getTimeControl() == TimeControl.CANADIAN ? MOVES_PER_PERIOD - whiteMovesInPeriod : -1;
                gameInfoFragment.updateGameInfo(gameState, blackMovesLeft, whiteMovesLeft);
                Log.d(TAG, "Game info UI updated.");
            } catch (Exception e) {
                Log.e(TAG, "Error updating GameInfoFragment", e);
            }
        }
    }

    private void handleGameOver() {
        mainThreadHandler.post(() -> {
            try {
                Log.w(TAG, "Calculating score without dead stone processing!");
                GameLogic.Score score = gameLogic.calculateScore(gameState, config.getScoringRule());
                Log.i(TAG, "Game Over! Score: " + score);

                String reason = gameState.getEndGameReason();
                if (reason == null || reason.isEmpty()) reason = "Game ended.";

                String message = String.format("%s\nScore: Black %.1f - White %.1f",
                        reason, score.blackScore, score.whiteScore);

                // Thay Toast bằng dialog
                gameActivity.showGameOverDialog(message,
                        () -> gameActivity.restartGame(config),
                        () -> gameActivity.finish());
                Log.i(TAG, "Game over dialog displayed.");
            } catch (Exception e) {
                Log.e(TAG, "Error during game over handling", e);
                showToast("Error showing game over!");
            }
        });
    }

    // Tiện ích

    private Player getCurrentPlayer() {
        return gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
    }

    private void showToast(String message) {
        gameActivity.runOnUiThread(() -> Toast.makeText(gameActivity, message, Toast.LENGTH_SHORT).show());
    }
}