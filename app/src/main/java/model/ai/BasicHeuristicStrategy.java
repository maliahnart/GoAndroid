package model.ai;

import model.*;

import java.util.ArrayList;
import java.util.Collections; // Để xáo trộn nếu điểm bằng nhau
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

public class BasicHeuristicStrategy extends AbstractAIStrategy {
    private final Random random = new Random();
    private final GameLogic logic = new GameLogic(); // Tạo 1 lần để tái sử dụng

    // Điểm số cho các loại heuristic
    private static final int CAPTURE_SCORE = 1000;    // Bắt quân
    private static final int SAVE_ATARI_SCORE = 900;     // Cứu quân mình khỏi Atari
    private static final int CREATE_ATARI_SCORE = 100;    // Đặt đối thủ vào Atari
    private static final int CONNECT_OWN_SCORE = 10;     // Đi gần quân mình
    private static final int CENTER_BIAS_SCORE = 1;      // Điểm cộng nhỏ nếu gần trung tâm
    private static final int SELF_ATARI_PENALTY = -800;  // Phạt nặng nếu tự đặt mình vào Atari
    private static final int FILL_OWN_EYE_PENALTY = -950; // Phạt rất nặng nếu tự lấp mắt mình

    @Override
    public Move generateMove(GameState gameState, Stone color) {
        BoardState currentBoard = gameState.getBoardState();
        List<PotentialMove> potentialMoves = new ArrayList<>();

        // 1. Tìm tất cả các nước đi hợp lệ và đánh giá ban đầu
        for (int x = 0; x < currentBoard.getSize(); x++) {
            for (int y = 0; y < currentBoard.getSize(); y++) {
                if (currentBoard.getStone(x, y) == Stone.EMPTY) {
                    Point point = new Point(x, y);
                    Move testMove = new Move(point, color);

                    if (logic.isValidMove(testMove, gameState)) {
                        // Tính điểm heuristic cho nước đi này
                        int score = evaluateMove(gameState, testMove);
                        potentialMoves.add(new PotentialMove(testMove, score));
                    }
                }
            }
        }

        // 2. Nếu không có nước đi nào hợp lệ -> Pass
        if (potentialMoves.isEmpty()) {
            return new Move(null, color, true, false); // Pass
        }

        // 3. Tìm nước đi có điểm số cao nhất
        potentialMoves.sort((m1, m2) -> Integer.compare(m2.score, m1.score)); // Sắp xếp giảm dần theo điểm

        // 4. Chọn nước đi tốt nhất (có thể có nhiều nước cùng điểm cao nhất)
        int bestScore = potentialMoves.get(0).score;
        List<PotentialMove> bestMoves = new ArrayList<>();
        for (PotentialMove pm : potentialMoves) {
            if (pm.score == bestScore) {
                bestMoves.add(pm);
            } else {
                // Vì đã sắp xếp nên có thể dừng sớm
                break;
            }
        }

        // 5. Nếu có nhiều nước đi cùng điểm tốt nhất, chọn ngẫu nhiên 1 trong số đó
        if (bestMoves.size() > 1) {
            Collections.shuffle(bestMoves); // Xáo trộn danh sách các nước tốt nhất
        }
        return bestMoves.get(0).move;
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

        // --- Kiểm tra các hiệu ứng ngay lập tức của nước đi ---
        // Tạo trạng thái bàn cờ *sau khi* thực hiện nước đi
        // (Bao gồm cả việc bắt quân nếu có)
        GameLogic.CapturedResult result = logic.calculateNextBoardState(move, currentBoard);
        BoardState nextBoard = result.getBoardState();
        int stonesCaptured = result.getCapturedCount();

        // 1. Heuristic: Bắt quân
        if (stonesCaptured > 0) {
            score += CAPTURE_SCORE * stonesCaptured; // Điểm cộng lớn khi bắt được quân
        }

        // 2. Heuristic: Cứu quân của mình khỏi trạng thái Atari
        // Kiểm tra các nhóm quân *của mình* xung quanh điểm vừa đi
        for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
            if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) && currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                // Nếu nhóm quân đó *trước đây* bị Atari (chỉ 1 liberty)
                if (isInAtari(currentBoard, neighbor, myColor)) {
                    // Và *bây giờ* (sau nước đi) không còn bị Atari nữa
                    if (!isInAtari(nextBoard, neighbor, myColor)) {
                        score += SAVE_ATARI_SCORE;
                        break; // Chỉ cần cứu 1 nhóm là đủ cộng điểm này
                    }
                }
            }
        }

        // 3. Heuristic: Đặt quân đối thủ vào Atari
        // Kiểm tra các nhóm quân *của đối thủ* xung quanh điểm vừa đi *sau khi* đã đi
        for (Point neighbor : getNeighbors(point, nextBoard.getSize())) {
            if (nextBoard.isValidPosition(neighbor.getX(), neighbor.getY()) && nextBoard.getStone(neighbor.getX(), neighbor.getY()) == opponentColor) {
                // Nếu nhóm đối thủ đó *bây giờ* bị Atari (và chưa bị bắt)
                if (isInAtari(nextBoard, neighbor, opponentColor)) {
                    score += CREATE_ATARI_SCORE;
                    // Có thể cộng dồn nếu đặt nhiều nhóm vào Atari, hoặc chỉ cộng 1 lần
                    break; // Tạm thời chỉ cộng 1 lần
                }
            }
        }


        // 4. Phạt: Tự đặt mình vào Atari
        // Kiểm tra nhóm quân chứa quân vừa đặt *sau khi* đi
        if (isInAtari(nextBoard, point, myColor)) {
            // Chỉ phạt nếu nước đi này không phải là nước cứu quân khác
            // (heuristic cứu quân đã cộng điểm rồi nên có thể bỏ qua phạt này trong trường hợp đó)
            boolean didSaveAnotherGroup = false;
            for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
                if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) && currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                    if (isInAtari(currentBoard, neighbor, myColor) && !isInAtari(nextBoard, neighbor, myColor)) {
                        didSaveAnotherGroup = true;
                        break;
                    }
                }
            }
            if (!didSaveAnotherGroup && stonesCaptured == 0) { // Chỉ phạt nếu không bắt quân và không cứu quân khác
                score += SELF_ATARI_PENALTY;
            }
        }

        // 5. Phạt: Tự lấp mắt mình
        if (isFillingOwnEye(currentBoard, point, myColor)) {
            score += FILL_OWN_EYE_PENALTY;
        }


        // 6. Heuristic: Đi gần quân mình (Kết nối)
        for (Point neighbor : getNeighbors(point, currentBoard.getSize())) {
            if (currentBoard.isValidPosition(neighbor.getX(), neighbor.getY()) && currentBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor) {
                score += CONNECT_OWN_SCORE;
                // break; // Có thể chỉ cộng 1 lần hoặc cộng dồn cho mỗi quân hàng xóm
            }
        }

        // 7. Heuristic: Gần trung tâm (giữ lại từ code gốc của bạn, điểm rất nhỏ)
        int center = currentBoard.getSize() / 2;
        int distance = Math.abs(point.getX() - center) + Math.abs(point.getY() - center);
        // Điểm càng cao khi càng gần trung tâm (distance nhỏ)
        // Ví dụ: score += CENTER_BIAS_SCORE * (center * 2 - distance);
        // Hoặc đơn giản:
        if (distance <= center / 2) { // Ví dụ: trong 1/4 bán kính từ tâm
            score += CENTER_BIAS_SCORE;
        }


        // Thêm một chút ngẫu nhiên nhỏ để tránh các nước đi lặp lại khi điểm bằng nhau
        score += random.nextInt(3) - 1; // Cộng ngẫu nhiên -1, 0, hoặc 1

        return score;
    }

    /**
     * Kiểm tra xem một nhóm quân có đang ở trạng thái Atari không (chỉ còn 1 liberty).
     * Cần hàm getLiberties hoặc tương tự trong GameLogic hoặc ở đây.
     */
    private boolean isInAtari(BoardState board, Point point, Stone color) {
        return countLiberties(board, point, color) == 1;
    }

    /**
     * Đếm số lượng liberty của nhóm quân chứa điểm point.
     * (Cần implement hàm này - có thể dựa trên logic findGroup và kiểm tra hàng xóm trống)
     */
    private int countLiberties(BoardState board, Point startPoint, Stone color) {
        if (!board.isValidPosition(startPoint.getX(), startPoint.getY()) || board.getStone(startPoint.getX(), startPoint.getY()) != color) {
            return 0; // Không có quân hoặc không đúng màu
        }

        Set<Point> visitedGroup = new HashSet<>(); // Theo dõi các quân trong nhóm đã xét
        Set<Point> liberties = new HashSet<>();   // Lưu các điểm liberty duy nhất
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
                        liberties.add(neighbor); // Tìm thấy liberty, thêm vào set (tự động loại trùng)
                    } else if (neighborStone == color && !visitedGroup.contains(neighbor)) {
                        // Nếu là quân cùng màu chưa thăm, thêm vào stack để xét nhóm tiếp
                        visitedGroup.add(neighbor);
                        stack.push(neighbor);
                    }
                    // Bỏ qua quân khác màu
                }
            }
        }
        return liberties.size(); // Trả về số lượng liberty tìm được
    }

    /**
     * Kiểm tra xem việc đặt quân tại 'point' có phải là tự lấp một mắt an toàn của mình không.
     * Đây là một heuristic đơn giản, kiểm tra xem tất cả hàng xóm của 'point'
     * có phải là quân cùng màu hay không. Một định nghĩa mắt phức tạp hơn sẽ cần thiết
     * cho AI mạnh hơn.
     */
    private boolean isFillingOwnEye(BoardState board, Point point, Stone color) {
        // Điểm đó phải trống
        if (board.getStone(point.getX(), point.getY()) != Stone.EMPTY) return false;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int boardSize = board.getSize();
        boolean allNeighborsAreOwnColor = true;
        int neighborCount = 0;

        for (int[] dir : directions) {
            int nx = point.getX() + dir[0];
            int ny = point.getY() + dir[1];

            if (board.isValidPosition(nx, ny)) {
                neighborCount++;
                if (board.getStone(nx, ny) != color) {
                    allNeighborsAreOwnColor = false;
                    break; // Chỉ cần 1 hàng xóm không phải màu mình là không phải mắt đơn giản
                }
            } else {
                // Nếu điểm nằm ở biên, coi như nó không phải là mắt an toàn theo cách kiểm tra này
                allNeighborsAreOwnColor = false;
                break;
            }
        }

        // Chỉ coi là lấp mắt nếu điểm đó nằm trong bàn cờ (có đủ hàng xóm)
        // và tất cả hàng xóm đó đều là quân của mình.
        return neighborCount > 0 && allNeighborsAreOwnColor;
    }


    /** Trả về danh sách các điểm hàng xóm hợp lệ của một điểm */
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

    // Lớp nội bộ để lưu nước đi tiềm năng và điểm số của nó
    private static class PotentialMove {
        final Move move;
        final int score;

        PotentialMove(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}