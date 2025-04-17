package model.ai;

import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.util.Log;

public class RandomAIStrategy extends AbstractAIStrategy {
    private static final String TAG = "RandomAIStrategy";
    private final Random random = new Random();

    @Override
    public Move generateMove(GameState gameState, Stone color, AIMoveCallback aiMoveCallback) {
        if (gameState == null || color == null || aiMoveCallback == null) {
            Log.e(TAG, "Invalid input: gameState=" + gameState + ", color=" + color + ", callback=" + aiMoveCallback);
            if (aiMoveCallback != null) {
                aiMoveCallback.onMoveGenerated(new Move(null, color != null ? color : Stone.BLACK, true, false));
            }
            return null;
        }

        try {
            BoardState board = gameState.getBoardState();
            if (board == null) {
                Log.e(TAG, "BoardState is null");
                aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
                return null;
            }

            GameLogic logic = new GameLogic();
            List<Point> validPoints = new ArrayList<>();

            // Tìm tất cả ô trống hợp lệ
            for (int x = 0; x < board.getSize(); x++) {
                for (int y = 0; y < board.getSize(); y++) {
                    if (board.getStone(x, y) == Stone.EMPTY) {
                        Move move = new Move(new Point(x, y), color);
                        if (logic.isValidMove(move, gameState)) {
                            validPoints.add(new Point(x, y));
                        }
                    }
                }
            }

            Move move;
            if (!validPoints.isEmpty()) {
                Point point = validPoints.get(random.nextInt(validPoints.size()));
                move = new Move(point, color);
                Log.d(TAG, "Generated move: " + move);
            } else {
                move = new Move(null, color, true, false); // Pass nếu không có nước đi
                Log.d(TAG, "No valid moves, passing: " + move);
            }

            aiMoveCallback.onMoveGenerated(move);
        } catch (Exception e) {
            Log.e(TAG, "Error generating move", e);
            aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
        }

        return null; // Kết quả qua callback
    }
}