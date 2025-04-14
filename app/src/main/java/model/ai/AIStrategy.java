package model.ai;

import model.Move;
import model.GameState;
import model.Stone;

public interface AIStrategy {

        /**
         * Tạo một nước đi cho AI.
         * @param gameState Trạng thái game hiện tại.
         * @param color Màu quân của AI.
         * @return Nước đi được chọn.
         */
        Move generateMove(GameState gameState, Stone color);

}
