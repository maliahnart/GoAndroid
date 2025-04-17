package model.ai;

import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.util.Log;

public class MCTSStrategy implements AIStrategy {
    private static final String TAG = "MCTSStrategy";
    private final Random random = new Random();
    private final int simulations;
    private final GameLogic logic = new GameLogic();

    public MCTSStrategy(int simulations) {
        this.simulations = simulations;
    }

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
            long startTime = System.currentTimeMillis();
            BoardState board = gameState.getBoardState();
            if (board == null) {
                Log.e(TAG, "BoardState is null");
                aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
                return null;
            }

            // Tìm các nước đi hợp lệ
            List<Point> validPoints = new ArrayList<>();
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
                Log.d(TAG, "No valid moves. Passing. Time: " + (System.currentTimeMillis() - startTime) + "ms");
                aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
                return null;
            }

            // MCTS cơ bản: mô phỏng ngẫu nhiên để chọn nước đi
            Move bestMove = selectBestMove(gameState, color, validPoints);
            Log.d(TAG, "Selected move: " + bestMove + ", time: " + (System.currentTimeMillis() - startTime) + "ms");
            aiMoveCallback.onMoveGenerated(bestMove);
        } catch (Exception e) {
            Log.e(TAG, "Error generating move", e);
            aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
        }

        return null; // Kết quả qua callback
    }

    /**
     * Chọn nước đi tốt nhất dựa trên mô phỏng ngẫu nhiên.
     */
    private Move selectBestMove(GameState gameState, Stone color, List<Point> validPoints) {
        double bestScore = -1;
        List<Move> bestMoves = new ArrayList<>();
        BoardState board = gameState.getBoardState();

        for (Point point : validPoints) {
            Move move = new Move(point, color);
            double score = evaluateMove(gameState, move);
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }

        // Chọn ngẫu nhiên trong các nước tốt nhất
        return bestMoves.get(random.nextInt(bestMoves.size()));
    }

    /**
     * Đánh giá nước đi bằng mô phỏng ngẫu nhiên (playout).
     * Trả về tỷ lệ thắng trung bình qua các mô phỏng.
     */
    private double evaluateMove(GameState originalState, Move move) {
        int wins = 0;
        Stone myColor = move.getColor();

        for (int i = 0; i < simulations; i++) {
            GameState simState = new GameState(originalState); // Sao chép trạng thái
            BoardState simBoard = simState.getBoardState();

            // Áp dụng nước đi
            GameLogic.CapturedResult result = logic.calculateNextBoardState(move, simBoard);
            if (result == null || result.getBoardState() == null) {
                continue; // Bỏ qua nếu mô phỏng lỗi
            }
            simState.recordMove(move, result.getBoardState(), result.getCapturedCount());

            // Chạy mô phỏng ngẫu nhiên đến khi kết thúc
            boolean gameEnded = false;
            while (!gameEnded) {
                List<Point> simValidPoints = new ArrayList<>();
                for (int x = 0; x < simBoard.getSize(); x++) {
                    for (int y = 0; y < simBoard.getSize(); y++) {
                        if (simBoard.getStone(x, y) == Stone.EMPTY) {
                            Move simMove = new Move(new Point(x, y), simState.getCurrentPlayer());
                            if (logic.isValidMove(simMove, simState)) {
                                simValidPoints.add(new Point(x, y));
                            }
                        }
                    }
                }

                Move simMove;
                if (simValidPoints.isEmpty()) {
                    simMove = new Move(null, simState.getCurrentPlayer(), true, false);
                } else {
                    Point point = simValidPoints.get(random.nextInt(simValidPoints.size()));
                    simMove = new Move(point, simState.getCurrentPlayer());
                }

                result = logic.calculateNextBoardState(simMove, simBoard);
                if (result == null || result.getBoardState() == null) {
                    gameEnded = true;
                    continue;
                }
                simState.recordMove(simMove, result.getBoardState(), result.getCapturedCount());
                simBoard = result.getBoardState();

                // Kiểm tra kết thúc (Pass liên tiếp hoặc bàn đầy)
                List<Move> history = simState.getMoveHistory();
                if (history.size() >= 2 && history.get(history.size() - 1).isPass() &&
                        history.get(history.size() - 2).isPass()) {
                    gameEnded = true;
                }
            }

            // Đánh giá kết quả (giả sử: số quân trên bàn)
            int myStones = countStones(simBoard, myColor);
            int opponentStones = countStones(simBoard, myColor.opponent());
            if (myStones > opponentStones) {
                wins++;
            }
        }

        return (double) wins / simulations;
    }

    /**
     * Đếm số quân của một màu trên bàn.
     */
    private int countStones(BoardState board, Stone color) {
        int count = 0;
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                if (board.getStone(x, y) == color) {
                    count++;
                }
            }
        }
        return count;
    }
}