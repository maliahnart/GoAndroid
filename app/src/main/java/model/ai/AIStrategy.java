package model.ai;

import model.GameState;
import model.Move;
import model.Stone;

public interface AIStrategy {
        /**
         * Tạo nước đi bất đồng bộ và thông báo qua callback.
         * @param gameState Trạng thái game hiện tại.
         * @param color Màu quân của bot.
         * @param callback Callback để trả nước đi.
         * @return Move (có thể null vì kết quả chính qua callback).
         */
        Move generateMove(GameState gameState, Stone color, AIMoveCallback callback);
}