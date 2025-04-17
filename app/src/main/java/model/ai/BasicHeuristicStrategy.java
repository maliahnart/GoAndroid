package model.ai;

import android.util.Log;
import model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/**
 * AI heuristic đơn giản cho cờ vây, dùng trong chế độ PVB_EASY.
 * Ưu tiên bắt quân, cứu nhóm, tạo Atari, cắt đối thủ, mở rộng vùng.
 * Tránh lấp mắt thật và tự Atari, ưu tiên Pass khi đối phương hết lượt
 * và không còn nước đi tốt.
 */
public class BasicHeuristicStrategy extends AbstractAIStrategy {
    private static final String TAG = "BasicHeuristicStrategy";
    private final Random random = new Random();
    private final GameLogic logic = new GameLogic();

    // Điểm số heuristic
    private static final int CAPTURE_SCORE = 1000;     // Bắt quân đối thủ
    private static final int SAVE_ATARI_SCORE = 900;   // Cứu nhóm mình khỏi Atari
    private static final int CREATE_ATARI_SCORE = 200; // Đặt đối thủ vào Atari
    private static final int CUT_OPPONENT_SCORE = 150; // Ngăn đối thủ nối
    private static final int EXPAND_LIBERTY_SCORE = 50;// Tăng khí nhóm mình
    private static final int CONNECT_OWN_SCORE = 20;   // Kết nối quân mình
    private static final int CENTER_BIAS_SCORE = 5;    // Gần trung tâm
    private static final int SELF_ATARI_PENALTY = -800;// Phạt tự Atari
    private static final int FILL_EYE_PENALTY = -1000; // Phạt lấp mắt thật
    private static final int PASS_THRESHOLD = 100;     // Ngưỡng để Pass nếu không có nước tốt

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
            BoardState currentBoard = gameState.getBoardState();
            if (currentBoard == null) {
                Log.e(TAG, "BoardState is null");
                aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
                return null;
            }

            List<PotentialMove> potentialMoves = new ArrayList<>();

            // 1. Kiểm tra nếu đối phương vừa Pass
            boolean opponentPassed = false;
            List<Move> moveHistory = gameState.getMoveHistory();
            if (moveHistory != null && !moveHistory.isEmpty() && moveHistory.get(moveHistory.size() - 1).isPass()) {
                opponentPassed = true;
                Log.d(TAG, "Opponent passed. Prioritizing Pass unless strong move found.");
            }

            // 2. Lấy các điểm tiềm năng
            Set<Point> candidates = getCandidatePoints(currentBoard);
            if (candidates.isEmpty()) {
                candidates.add(new Point(currentBoard.getSize() / 2, currentBoard.getSize() / 2));
            }

            // 3. Đánh giá nước đi hợp lệ
            for (Point point : candidates) {
                Move testMove = new Move(point, color);
                if (logic.isValidMove(testMove, gameState)) {
                    int score = evaluateMove(gameState, testMove);
                    potentialMoves.add(new PotentialMove(testMove, score));
                }
            }

            // 4. Xử lý khi không có nước đi hoặc nước đi kém
            if (potentialMoves.isEmpty()) {
                Log.d(TAG, "No valid moves. Passing. Time: " +
                        (System.currentTimeMillis() - startTime) + "ms");
                aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
                return null;
            }

            // 5. Sắp xếp và chọn nước đi
            potentialMoves.sort((m1, m2) -> Integer.compare(m2.score, m1.score));
            int bestScore = potentialMoves.get(0).score;

            // 6. Nếu đối phương Pass và không có nước đi tốt -> Pass
            if (opponentPassed && bestScore < PASS_THRESHOLD) {
                Log.d(TAG, "Opponent passed and best score (" + bestScore +
                        ") below threshold. Passing. Time: " +
                        (System.currentTimeMillis() - startTime) + "ms");
                aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
                return null;
            }

            // 7. Chọn nước tốt nhất
            List<PotentialMove> bestMoves = new ArrayList<>();
            for (PotentialMove pm : potentialMoves) {
                if (pm.score == bestScore) {
                    bestMoves.add(pm);
                } else {
                    break;
                }
            }

            if (bestMoves.size() > 1) {
                Collections.shuffle(bestMoves, random);
            }
            Move selectedMove = bestMoves.get(0).move;
            Log.d(TAG, "Selected move: " + selectedMove + ", score: " +
                    bestScore + ", time: " + (System.currentTimeMillis() - startTime) + "ms");
            aiMoveCallback.onMoveGenerated(selectedMove);
        } catch (Exception e) {
            Log.e(TAG, "Error generating move", e);
            aiMoveCallback.onMoveGenerated(new Move(null, color, true, false));
        }

        return null; // Kết quả qua callback
    }

    /**
     * Đánh giá điểm số heuristic cho một nước đi hợp lệ.
     */
    private int evaluateMove(GameState originalState, Move move) {
        int score = 0;
        Point point = move.getPoint();
        Stone myColor = move.getColor();
        Stone opponentColor = myColor.opponent();
        BoardState currentBoard = originalState.getBoardState();

        // Tính trạng thái sau khi đi
        GameLogic.CapturedResult result = logic.calculateNextBoardState(move, currentBoard);
        BoardState nextBoard = result.getBoardState();
        int stonesCaptured = result.getCapturedCount();

        // 1. Bắt quân
        if (stonesCaptured > 0) {
            score += CAPTURE_SCORE * stonesCaptured;
        }

        // 2. Cứu nhóm khỏi Atari
        for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
            if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                    currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                if (isInAtari(currentBoard, neighbor, myColor) &&
                        !isInAtari(nextBoard, neighbor, myColor)) {
                    score += SAVE_ATARI_SCORE;
                    break;
                }
            }
        }

        // 3. Đặt đối thủ vào Atari
        for (Point neighbor : getNeighbors(point, nextBoard.getSize())) {
            if (nextBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                    nextBoard.getStone(neighbor.getX(), neighbor.getY()) == opponentColor) {
                if (isInAtari(nextBoard, neighbor, opponentColor)) {
                    score += CREATE_ATARI_SCORE;
                    break;
                }
            }
        }

        // 4. Tăng khí nhóm mình
        for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
            if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                    currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                int beforeLiberties = countLiberties(currentBoard, neighbor, myColor);
                int afterLiberties = countLiberties(nextBoard, neighbor, myColor);
                if (afterLiberties > beforeLiberties) {
                    score += EXPAND_LIBERTY_SCORE * (afterLiberties - beforeLiberties);
                }
            }
        }

        // 5. Ngăn đối thủ nối
        int opponentConnectionsBlocked = 0;
        for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
            if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                    currentBoard.getStone(neighbor.getX(), neighbor.getY()) == opponentColor) {
                for (Point neighbor2 : getNeighbors(neighbor, currentBoard.getSize())) {
                    if (!neighbor2.equals(point) &&
                            currentBoard.isValidPosition(neighbor2.getX(), neighbor2.getY()) &&
                            currentBoard.getStone(neighbor2.getX(), neighbor2.getY()) == opponentColor) {
                        opponentConnectionsBlocked++;
                    }
                }
            }
        }
        score += CUT_OPPONENT_SCORE * opponentConnectionsBlocked;

        // 6. Kết nối quân mình
        for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
            if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                    currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                score += CONNECT_OWN_SCORE;
            }
        }

        // 7. Gần trung tâm
        int center = currentBoard.getSize() / 2;
        int distance = Math.abs(point.getX() - center) + Math.abs(point.getY() - center);
        if (distance <= center / 2) {
            score += CENTER_BIAS_SCORE;
        }

        // 8. Phạt tự Atari
        if (isInAtari(nextBoard, point, myColor)) {
            boolean didSaveOrCapture = stonesCaptured > 0;
            for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
                if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                        currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                    if (isInAtari(currentBoard, neighbor, myColor) &&
                            !isInAtari(nextBoard, neighbor, myColor)) {
                        didSaveOrCapture = true;
                        break;
                    }
                }
            }
            if (!didSaveOrCapture) {
                score += SELF_ATARI_PENALTY;
            }
        }

        // 9. Phạt lấp mắt thật
        if (isFillingTrueEye(currentBoard, point, myColor)) {
            score += FILL_EYE_PENALTY;
        }

        // Thêm ngẫu nhiên nhỏ
        score += random.nextInt(3) - 1;
        return score;
    }

    /**
     * Lấy các điểm tiềm năng (gần quân hiện tại, bán kính 2 ô).
     */
    private Set<Point> getCandidatePoints(BoardState board) {
        Set<Point> candidates = new HashSet<>();
        int boardSize = board.getSize();
        int radius = 2;

        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                if (board.getStone(x, y) != Stone.EMPTY) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (board.isValidPosition(nx, ny) &&
                                    board.getStone(nx, ny) == Stone.EMPTY) {
                                candidates.add(new Point(nx, ny));
                            }
                        }
                    }
                }
            }
        }
        return candidates;
    }

    /**
     * Kiểm tra xem nhóm chứa điểm có đang ở Atari không.
     */
    private boolean isInAtari(BoardState board, Point point, Stone color) {
        return countLiberties(board, point, color) == 1;
    }

    /**
     * Đếm số khí của nhóm chứa điểm.
     */
    private int countLiberties(BoardState board, Point startPoint, Stone color) {
        if (!board.isValidPosition(startPoint.getX(), startPoint.getY()) ||
                board.getStone(startPoint.getX(), startPoint.getY()) != color) {
            return 0;
        }

        Set<Point> visitedGroup = new HashSet<>();
        Set<Point> liberties = new HashSet<>();
        Stack<Point> stack = new Stack<>();
        stack.push(startPoint);
        visitedGroup.add(startPoint);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!stack.isEmpty()) {
            Point current = stack.pop();
            for (int[] dir : directions) {
                int nx = current.getX() + dir[0];
                int ny = current.getY() + dir[1];
                Point neighbor = new Point(nx, ny);
                if (board.isValidPosition(nx, ny)) {
                    Stone neighborStone = board.getStone(nx, ny);
                    if (neighborStone == Stone.EMPTY) {
                        liberties.add(neighbor);
                    } else if (neighborStone == color && !visitedGroup.contains(neighbor)) {
                        visitedGroup.add(neighbor);
                        stack.push(neighbor);
                    }
                }
            }
        }
        return liberties.size();
    }

    /**
     * Kiểm tra xem điểm có phải là mắt thật không.
     */
    private boolean isFillingTrueEye(BoardState board, Point point, Stone color) {
        if (board.getStone(point.getX(), point.getY()) != Stone.EMPTY) return false;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int[][] diagonals = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        int boardSize = board.getSize();

        // Kiểm tra hàng xóm
        boolean allNeighborsSafe = true;
        for (int[] dir : directions) {
            int nx = point.getX() + dir[0];
            int ny = point.getY() + dir[1];
            if (!board.isValidPosition(nx, ny)) continue;
            if (board.getStone(nx, ny) != color) {
                allNeighborsSafe = false;
                break;
            }
        }
        if (!allNeighborsSafe) return false;

        // Kiểm tra chéo
        int safeDiagonals = 0;
        for (int[] diag : diagonals) {
            int nx = point.getX() + diag[0];
            int ny = point.getY() + diag[1];
            if (!board.isValidPosition(nx, ny) ||
                    board.getStone(nx, ny) == color ||
                    board.getStone(nx, ny) == Stone.EMPTY) {
                safeDiagonals++;
            }
        }
        if (safeDiagonals < 3) return false;

        // Kiểm tra nhóm xung quanh có sống không (ít nhất 2 khí)
        for (int[] dir : directions) {
            int nx = point.getX() + dir[0];
            int ny = point.getY() + dir[1];
            if (board.isValidPosition(nx, ny)) {
                int liberties = countLiberties(board, new Point(nx, ny), color);
                if (liberties < 2) {
                    return false; // Nhóm không sống -> không phải mắt thật
                }
            }
        }

        return true;
    }

    /**
     * Lấy danh sách hàng xóm hợp lệ.
     */
    private List<Point> getNeighbors(Point p, int boardSize) {
        List<Point> neighbors = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int nx = p.getX() + dir[0];
            int ny = p.getY() + dir[1];
            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                neighbors.add(new Point(nx, ny));
            }
        }
        return neighbors;
    }

    /**
     * Lớp lưu nước đi và điểm số.
     */
    private static class PotentialMove {
        final Move move;
        final int score;

        PotentialMove(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}