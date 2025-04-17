package model.player;

import android.util.Log;
import model.GameState;
import model.Move;
import model.Stone;
import model.ai.AIStrategy;
import model.ai.AIMoveCallback;

public class BotPlayer implements Player {
    private final Stone color;
    private final AIStrategy strategy;

    public BotPlayer(Stone color, AIStrategy strategy) {
        this.color = color;
        this.strategy = strategy;
    }

    @Override
    public Stone getColor() {
        return color;
    }

    @Override
    public Move generateMove(GameState gameState) {
        // Không dùng trực tiếp, chỉ để tương thích giao diện Player
        Log.w("BotPlayer", "Synchronous generateMove called; use generateMove with AIMoveCallback instead");
        return null;
    }

    /**
     * Tạo nước đi bất đồng bộ và thông báo qua callback.
     * @param gameState Trạng thái game hiện tại.
     * @param callback Callback để trả nước đi.
     */
    public void generateMove(GameState gameState, AIMoveCallback callback) {
        if (gameState == null || callback == null || color == null) {
            Log.e("BotPlayer", "Invalid input: gameState=" + gameState + ", callback=" + callback + ", color=" + color);
            callback.onMoveGenerated(new Move(null, color != null ? color : Stone.BLACK, true, false));
            return;
        }
        strategy.generateMove(gameState, color, callback);
    }
}