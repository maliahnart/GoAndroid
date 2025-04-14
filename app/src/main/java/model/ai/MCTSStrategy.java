package model.ai;

import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MCTSStrategy extends AbstractAIStrategy {
    private final Random random = new Random();
    private final int simulations;

    public MCTSStrategy(int simulations) {
        this.simulations = simulations;
    }

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

        // TODO: Triển khai MCTS đầy đủ (giai đoạn: selection, expansion, simulation, backpropagation)
        // Hiện tại, chọn ngẫu nhiên để placeholder
        Point point = validPoints.get(random.nextInt(validPoints.size()));
        return new Move(point, color);
    }
}