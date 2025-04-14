package controller;

import config.GameConfig;
import config.GameMode;
import model.player.BotPlayer;
import model.GameLogic;
import model.GameState;
import model.player.HumanPlayer;
import model.Move;
import model.player.Player;
import model.Point;
import model.Stone;
import model.ai.RandomAIStrategy;
import view.BoardView;
import view.GameActivity;
import view.GameInfoFragment;

public class GameController {
    private final GameActivity gameActivity;
    private final BoardView boardView;
    private final GameInfoFragment gameInfoFragment;
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final Player blackPlayer;
    private final Player whitePlayer;

    public GameController(GameActivity activity, BoardView boardView, GameInfoFragment fragment, GameConfig config) {
        this.gameActivity = activity;
        this.boardView = boardView;
        this.gameInfoFragment = fragment;
        this.gameState = new GameState(config);
        this.gameLogic = new GameLogic();

        // Khởi tạo người chơi dựa trên chế độ
        if (config.getGameMode() == GameMode.PVP) {
            blackPlayer = new HumanPlayer(Stone.BLACK);
            whitePlayer = new HumanPlayer(Stone.WHITE);
        } else {
            blackPlayer = new HumanPlayer(Stone.BLACK);
            whitePlayer = new BotPlayer(Stone.WHITE, new RandomAIStrategy());
        }

        // Thiết lập giao diện ban đầu
        boardView.setBoardState(gameState.getBoardState());
        boardView.setOnStonePlacedListener(this::handleStonePlacement);
        updateGameInfo();
    }

    private void handleStonePlacement(Point point) {
        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        if (currentPlayer instanceof HumanPlayer) {
            Move move = new Move(point, currentPlayer.getColor());
            if (gameLogic.isValidMove(move, gameState)) {
                gameLogic.applyMove(move, gameState);
                boardView.invalidate();
                updateGameInfo();
                if (!gameState.isGameOver()) {
                    playBotMoveIfNeeded();
                } else {
                    handleGameOver();
                }
            }
        }
    }

    public void handlePass() {
        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        Move passMove = new Move(null, currentPlayer.getColor(), true, false);
        gameLogic.applyMove(passMove, gameState);
        updateGameInfo();
        if (gameState.isGameOver()) {
            handleGameOver();
        } else {
            playBotMoveIfNeeded();
        }
    }

    public void handleResign() {
        Move resignMove = new Move(null, gameState.getCurrentPlayer(), false, true);
        gameLogic.applyMove(resignMove, gameState);
        handleGameOver();
    }

    private void playBotMoveIfNeeded() {
        Player currentPlayer = gameState.getCurrentPlayer() == Stone.BLACK ? blackPlayer : whitePlayer;
        if (currentPlayer instanceof BotPlayer) {
            Move botMove = currentPlayer.generateMove(gameState);
            if (gameLogic.isValidMove(botMove, gameState)) {
                gameLogic.applyMove(botMove, gameState);
                boardView.invalidate();
                updateGameInfo();
                if (gameState.isGameOver()) {
                    handleGameOver();
                }
            }
        }
    }

    private void updateGameInfo() {
        gameInfoFragment.updateGameInfo(gameState);
    }

    private void handleGameOver() {
        // TODO: Thêm dialog game over nếu cần
    }
}