package model.player;
import model.GameState;
import model.Move;
import model.Stone;
public interface Player {
    /**
     * Trả về màu quân của người chơi.
     * @return BLACK hoặc WHITE.
     */
    Stone getColor();

    /**
     * Tạo một nước đi dựa trên trạng thái game.
     * @param gameState Trạng thái game hiện tại.
     * @return Nước đi được chọn.
     */
    Move generateMove(GameState gameState);
}
