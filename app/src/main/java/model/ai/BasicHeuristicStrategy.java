package model.ai;
import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BasicHeuristicStrategy extends AbstractAIStrategy {
    private final Random random = new Random();

    @Override
    public Move generateMove(GameState gameState, Stone color) {
        List<Point> validPoints = new ArrayList<>();
        BoardState board = gameState.getBoardState();
        GameLogic logic = new GameLogic();

        // Tìm các nước đi hợp lệ
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

        if (validPoints.isEmpty()) {
            return new Move(null, color, true, false); // Pass
        }

        // Ưu tiên nước đi gần trung tâm (heuristic đơn giản)
        Point bestPoint = validPoints.get(0);
        int center = board.getSize() / 2;
        int minDistance = Integer.MAX_VALUE;

        for (Point p : validPoints) {
            int distance = Math.abs(p.getX() - center) + Math.abs(p.getY() - center);
            if (distance < minDistance) {
                minDistance = distance;
                bestPoint = p;
            }
        }

        return new Move(bestPoint, color);
    }
}