package model;

import java.util.ArrayList;
import java.util.List;

import config.GameConfig;

public class GameState {
    private final BoardState boardState;
    private Stone currentPlayer;
    private final List<Move> moveHistory;
    private int consecutivePasses;

    private final float komi;

    public GameState(GameConfig config) {
        this.boardState = new BoardState(config);
        this.currentPlayer = Stone.BLACK;
        this.moveHistory = new ArrayList<>();
        this.consecutivePasses = 0;
        this.komi = config.getKomi();
    }

    public float getKomi() {
        return komi;
    }
    public BoardState getBoardState() {
        return boardState;
    }

    public Stone getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Stone currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public int getConsecutivePasses() {
        return consecutivePasses;
    }

    /**
     * Thêm một nước đi vào lịch sử và cập nhật trạng thái.
     * @param move Nước đi cần thêm.
     */
    public void addMove(Move move) {
        moveHistory.add(move);
        if (move.isPass()) {
            consecutivePasses++;
        } else {
            consecutivePasses = 0;
        }
    }

    /**
     * Kiểm tra xem game đã kết thúc chưa (hai lần pass liên tiếp hoặc resign).
     * @return true nếu game kết thúc.
     */
    public boolean isGameOver() {
        return consecutivePasses >= 2 || moveHistory.stream().anyMatch(Move::isResign);
    }
}