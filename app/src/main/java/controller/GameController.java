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
import model.ai.AIMoveCallback;
import model.ai.AlphaBetaStrategy;
import model.ai.BasicHeuristicStrategy;
import model.ai.RandomAIStrategy;
import model.player.BotPlayer;
import model.player.HumanPlayer;
import model.player.Player;
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
        this.blackPlayer = new HumanPlayer(Stone.BLACK);
        if (config.getGameMode() == GameMode.PVP) {
            this.whitePlayer = new HumanPlayer(Stone.WHITE);
        } else if (config.getGameMode() == GameMode.PVB_EASY) {
            this.whitePlayer = new BotPlayer(Stone.WHITE, new RandomAIStrategy());
        } else if (config.getGameMode() == GameMode.PVB_MEDIUM) {
            this.whitePlayer = new BotPlayer(Stone.WHITE, new BasicHeuristicStrategy());
        }
        else{
            this.whitePlayer = new BotPlayer(Stone.WHITE, new AlphaBetaStrategy());
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
        if (gameState == null || gameState.isGameOver()) {
            Log.d(TAG, "startTurn: Game is over or gameState is null.");
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

        Move move = new Move(point, gameState.getCurrentPlayer());
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
        Move passMove = new Move(null, gameState.getCurrentPlayer(), true, false);
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
        Move resignMove = new Move(null, gameState.getCurrentPlayer(), false, true);
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

        if (config.getGameMode() == GameMode.PVB_EASY && gameState.getMoveHistory() != null && !gameState.getMoveHistory().isEmpty()) {
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
                Log.e(TAG, "Invalid input in applyValidatedMove: gameState=" + gameState + ", move=" + move);
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

            if (boardView != null) {
                boardView.setBoardState(gameState.getBoardState());
                boardView.invalidate();
            } else {
                Log.e(TAG, "boardView is null in applyValidatedMove");
            }

            // Kiểm tra Pass liên tiếp
            List<Move> moveHistory = gameState.getMoveHistory();
            if (move.isPass() && moveHistory != null && moveHistory.size() >= 2 &&
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
            bot.generateMove(gameState, botMove -> mainThreadHandler.postDelayed(() -> {
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
//                        showToast("Bot moved");
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
            }, BOT_MOVE_DELAY_MS));
        } catch (Exception e) {
            Log.e(TAG, "Error requesting bot move", e);
            showToast("Bot Error: Failed to generate move!");
            gameState.setGameOver(true, "Bot Error: " + e.getMessage());
            handleGameOver();
        }
    }

    // Cập nhật giao diện

    private void updateGameInfoUI() {
        if (gameInfoFragment == null || gameState == null) {
            Log.e(TAG, "Cannot update UI: gameInfoFragment or gameState is null");
            return;
        }
        try {
            int blackMovesLeft = config.getTimeControl() == TimeControl.CANADIAN ? MOVES_PER_PERIOD - blackMovesInPeriod : -1;
            int whiteMovesLeft = config.getTimeControl() == TimeControl.CANADIAN ? MOVES_PER_PERIOD - whiteMovesInPeriod : -1;
            gameInfoFragment.updateGameInfo(gameState, blackMovesLeft, whiteMovesLeft);
            Log.d(TAG, "Game info UI updated.");
        } catch (Exception e) {
            Log.e(TAG, "Error updating GameInfoFragment", e);
        }
    }

    private void handleGameOver() {
        mainThreadHandler.post(() -> {
            try {
                if (gameState == null) {
                    Log.e(TAG, "gameState is null in handleGameOver");
                    showToast("Error: Game state is null!");
                    return;
                }
                Log.w(TAG, "Calculating score without dead stone processing!");
                GameLogic.Score score = gameLogic != null ? gameLogic.calculateScore(gameState, config.getScoringRule()) : null;
                if (score == null) {
                    Log.e(TAG, "Score is null in handleGameOver");
                    showToast("Error: Cannot calculate score!");
                    return;
                }
                Log.i(TAG, "Game Over! Score: " + score);

                String reason = gameState.getEndGameReason();
                if (reason == null || reason.isEmpty()) reason = "Game ended.";

                String message = String.format("%s\nScore: Black %.1f - White %.1f",
                        reason, score.blackScore, score.whiteScore);

                // Thay Toast bằng dialog
                if (gameActivity != null) {
                    gameActivity.showGameOverDialog(message,
                            () -> gameActivity.restartGame(config),
                            () -> gameActivity.finish());
                    Log.i(TAG, "Game over dialog displayed.");
                } else {
                    Log.e(TAG, "gameActivity is null in handleGameOver");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during game over handling", e);
                showToast("Error showing game over!");
            }
        });
    }

    // Tiện ích

    private Player getCurrentPlayer() {
        if (gameState == null) {
            Log.e(TAG, "gameState is null in getCurrentPlayer");
            return blackPlayer; // Fallback
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