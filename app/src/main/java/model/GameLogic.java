package model;

import config.ScoringRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack; // Sử dụng Stack để khử đệ quy, tránh StackOverflowError

public class GameLogic {

    // --- isValidMove, calculateNextBoardState, hasLiberties, findGroup, captureOpponentStones ---
    // --- Giữ nguyên các phương thức này như trong phiên bản trước của bạn ---
    // ... (Code của các phương thức này ở đây) ...
    public boolean isValidMove(Move move, GameState gameState) {
        if (move.isPass() || move.isResign()) {
            return true;
        }

        Point point = move.getPoint();
        BoardState currentBoard = gameState.getBoardState();
        int boardSize = currentBoard.getSize();

        // 1. Kiểm tra vị trí có nằm trong bàn cờ không
        if (!currentBoard.isValidPosition(point.getX(), point.getY())) {
            return false;
        }

        // 2. Kiểm tra vị trí có trống không
        if (currentBoard.getStone(point.getX(), point.getY()) != Stone.EMPTY) {
            return false;
        }

        // --- Thử đặt quân để kiểm tra luật ---
        BoardState tempBoard = currentBoard.copy();
        tempBoard.setStone(point.getX(), point.getY(), move.getColor());

        // 3. Kiểm tra xem nước đi có bắt được quân đối thủ không
        List<Point> capturedStones = captureOpponentStonesIfNoLiberties(tempBoard, point, move.getColor());
        // Nếu bắt được quân, loại bỏ chúng khỏi tempBoard để kiểm tra tự sát/ko chính xác
        if (!capturedStones.isEmpty()) {
            for (Point p : capturedStones) {
                tempBoard.setStone(p.getX(), p.getY(), Stone.EMPTY);
            }
        }

        // 4. Kiểm tra tự sát (Suicide Rule)
        if (capturedStones.isEmpty() && !hasLiberties(tempBoard, point, move.getColor())) {
            return false;
        }

        // 5. Kiểm tra luật Ko (Ko Rule)
        BoardState previousBoardState = gameState.getPreviousBoardState();
        if (previousBoardState != null && tempBoard.equals(previousBoardState)) {
            return false; // Lặp lại trạng thái bàn cờ ngay trước đó -> vi phạm Ko
        }

        return true;
    }

    public CapturedResult calculateNextBoardState(Move move, BoardState currentBoard) {
        if (move.isPass() || move.isResign()) {
            // Trả về bản sao và không có quân bị bắt
            return new CapturedResult(currentBoard.copy(), 0);
        }

        Point point = move.getPoint();
        Stone color = move.getColor();
        BoardState nextBoard = currentBoard.copy();

        nextBoard.setStone(point.getX(), point.getY(), color);

        // Sử dụng hàm capture mới để lấy danh sách quân bị bắt
        List<Point> captured = captureOpponentStonesIfNoLiberties(nextBoard, point, color);
        for (Point p : captured) {
            nextBoard.setStone(p.getX(), p.getY(), Stone.EMPTY);
        }

        return new CapturedResult(nextBoard, captured.size());
    }


    private boolean hasLiberties(BoardState board, Point startPoint, Stone color) {
        if (!board.isValidPosition(startPoint.getX(), startPoint.getY()) || board.getStone(startPoint.getX(), startPoint.getY()) != color) {
            return false; // Hoặc ném lỗi nếu điểm bắt đầu không hợp lệ
        }
        Set<Point> group = new HashSet<>();
        Set<Point> liberties = new HashSet<>();
        Stack<Point> stack = new Stack<>();
        Set<Point> visited = new HashSet<>(); // Chỉ dùng visited trong phạm vi hàm này

        stack.push(startPoint);
        visited.add(startPoint);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!stack.isEmpty()) {
            Point current = stack.pop();
            group.add(current); // Thêm vào nhóm đang xét

            for (int[] dir : directions) {
                int nx = current.getX() + dir[0];
                int ny = current.getY() + dir[1];
                Point neighbor = new Point(nx, ny);

                if (board.isValidPosition(nx, ny)) {
                    Stone neighborStone = board.getStone(nx, ny);
                    if (neighborStone == Stone.EMPTY) {
                        liberties.add(neighbor); // Tìm thấy một khí
                        // Có thể return true ngay đây để tối ưu nếu chỉ cần biết có > 0 khí
                        // return true;
                    } else if (neighborStone == color && !visited.contains(neighbor)) {
                        // Nếu là quân cùng màu và chưa thăm, thêm vào stack để xét tiếp
                        visited.add(neighbor);
                        stack.push(neighbor);
                    }
                    // Bỏ qua quân khác màu
                }
            }
        }
        // return !liberties.isEmpty(); // Nếu không tối ưu ở trên thì kiểm tra cuối cùng
        return !liberties.isEmpty();
    }

    // Hàm tìm nhóm quân (đã sửa để dùng Stack, tránh đệ quy sâu)
    private void findGroup(BoardState board, Point startPoint, Stone color, List<Point> group, Set<Point> visited) {
        if (!board.isValidPosition(startPoint.getX(), startPoint.getY())
                || visited.contains(startPoint)
                || board.getStone(startPoint.getX(), startPoint.getY()) != color) {
            return;
        }

        Stack<Point> stack = new Stack<>();
        stack.push(startPoint);
        visited.add(startPoint);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!stack.isEmpty()) {
            Point current = stack.pop();
            group.add(current);

            for (int[] dir : directions) {
                int nx = current.getX() + dir[0];
                int ny = current.getY() + dir[1];
                Point neighbor = new Point(nx, ny);

                if (board.isValidPosition(nx, ny)
                        && !visited.contains(neighbor)
                        && board.getStone(nx, ny) == color) {
                    visited.add(neighbor);
                    stack.push(neighbor);
                }
            }
        }
    }


    // Hàm captureOpponentStones (đã sửa để gọi findGroup và hasLiberties đã tối ưu)
    private List<Point> captureOpponentStonesIfNoLiberties(BoardState board, Point lastMovePoint, Stone playerColor) {
        List<Point> totalCaptured = new ArrayList<>();
        Stone opponentColor = playerColor.opponent();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        // Sử dụng Set để tránh kiểm tra lại cùng một nhóm đối thủ nhiều lần
        Set<Point> processedOpponentGroups = new HashSet<>();

        for (int[] dir : directions) {
            int nx = lastMovePoint.getX() + dir[0];
            int ny = lastMovePoint.getY() + dir[1];
            Point neighbor = new Point(nx, ny);

            if (board.isValidPosition(nx, ny)
                    && board.getStone(nx, ny) == opponentColor
                    && !processedOpponentGroups.contains(neighbor)) { // Chỉ xử lý nếu chưa thuộc nhóm đã xét

                List<Point> opponentGroup = new ArrayList<>();
                Set<Point> visitedForGroup = new HashSet<>(); // Dùng visited riêng cho mỗi lần gọi findGroup
                findGroup(board, neighbor, opponentColor, opponentGroup, visitedForGroup);

                // Kiểm tra xem nhóm đó có hết khí không
                if (!opponentGroup.isEmpty() && !hasLiberties(board, opponentGroup.get(0), opponentColor)) {
                    totalCaptured.addAll(opponentGroup);
                }
                // Đánh dấu tất cả các quân trong nhóm này đã được xử lý
                processedOpponentGroups.addAll(opponentGroup);
            }
        }
        return totalCaptured;
    }

    // --------------------------------------------------------------------
    // PHẦN TÍNH ĐIỂM ĐÃ SỬA ĐỔI
    // --------------------------------------------------------------------

    /**
     * Tính điểm số cuối game.
     * QUAN TRỌNG: Hàm này giả định tất cả các quân trên bàn là SỐNG.
     * Cần phải có bước xử lý quân chết TRƯỚC KHI gọi hàm này.
     * @param state Trạng thái game (đã xử lý quân chết nếu cần).
     * @param rule Luật tính điểm (Nhật Bản/Trung Quốc).
     * @return Điểm số của Đen và Trắng.
     */
    public Score calculateScore(GameState state, ScoringRule rule) {
        BoardState board = state.getBoardState(); // Board này nên là board đã xử lý quân chết nếu theo luật Nhật

        // Tính lãnh thổ cho cả hai bên cùng lúc
        TerritoryResult territory = calculateTerritory(board);

        float blackScore;
        float whiteScore;

        if (rule == ScoringRule.JAPANESE) {
            // Luật Nhật: Lãnh thổ + Tù binh (bao gồm quân chết đã bắt)
            // Giả định state.getBlackCaptured() đã bao gồm quân trắng chết bị bắt cuối ván
            blackScore = territory.blackTerritory + state.getBlackCaptured();
            // Giả định state.getWhiteCaptured() đã bao gồm quân đen chết bị bắt cuối ván
            whiteScore = territory.whiteTerritory + state.getWhiteCaptured() + state.getKomi();
        } else { // ScoringRule.CHINESE
            // Luật Trung Quốc: Lãnh thổ + Quân sống trên bàn
            // countStones cần đảm bảo chỉ đếm quân sống nếu có cơ chế đánh dấu quân chết
            int blackStones = countStones(board, Stone.BLACK);
            int whiteStones = countStones(board, Stone.WHITE);
            blackScore = territory.blackTerritory + blackStones;
            whiteScore = territory.whiteTerritory + whiteStones + state.getKomi();
        }

        return new Score(blackScore, whiteScore);
    }

    /**
     * Tính toán lãnh thổ cho cả Đen và Trắng.
     * Giả định các quân trên bàn là quân sống.
     * @param board Trạng thái bàn cờ.
     * @return Một đối tượng TerritoryResult chứa điểm lãnh thổ của mỗi bên.
     */
    private TerritoryResult calculateTerritory(BoardState board) {
        int size = board.getSize();
        boolean[][] visited = new boolean[size][size]; // Theo dõi các ô trống đã được xét
        int blackTerritory = 0;
        int whiteTerritory = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Nếu là ô trống và chưa thuộc vùng nào đã xét
                if (board.getStone(x, y) == Stone.EMPTY && !visited[x][y]) {
                    Set<Point> regionPoints = new HashSet<>(); // Lưu các điểm trong vùng trống hiện tại
                    Set<Stone> borderingColors = new HashSet<>(); // Lưu các màu quân tiếp giáp vùng này

                    // Bắt đầu flood fill để tìm vùng và các màu bao quanh
                    floodFillForTerritory(board, x, y, visited, regionPoints, borderingColors);

                    // Phân tích kết quả sau khi flood fill xong vùng này
                    if (borderingColors.size() == 1) { // Chỉ giáp với 1 màu
                        if (borderingColors.contains(Stone.BLACK)) {
                            blackTerritory += regionPoints.size();
                        } else if (borderingColors.contains(Stone.WHITE)) {
                            whiteTerritory += regionPoints.size();
                        }
                        // else: Trường hợp chỉ giáp EMPTY (không thể xảy ra nếu board có quân)
                    }
                    // else if borderingColors.size() == 0 || borderingColors.size() == 2:
                    // Vùng Dame (không giáp quân nào hoặc giáp cả 2 màu) -> không tính điểm
                }
            }
        }
        return new TerritoryResult(blackTerritory, whiteTerritory);
    }

    /**
     * Hàm Flood Fill (đã sửa đổi) để tìm vùng trống và các màu quân bao quanh.
     * Sử dụng Stack để tránh lỗi tràn bộ nhớ đệ quy (StackOverflowError).
     * @param board Trạng thái bàn cờ.
     * @param startX Tọa độ x bắt đầu.
     * @param startY Tọa độ y bắt đầu.
     * @param visited Mảng 2D theo dõi các ô trống đã được thăm trong TOÀN BỘ quá trình tính lãnh thổ.
     * @param regionPoints Set để thu thập các điểm thuộc vùng trống hiện tại.
     * @param borderingColors Set để thu thập các màu quân (BLACK, WHITE) tiếp giáp với vùng trống này.
     */
    private void floodFillForTerritory(BoardState board, int startX, int startY, boolean[][] visited,
                                       Set<Point> regionPoints, Set<Stone> borderingColors) {

        int size = board.getSize();
        Stack<Point> stack = new Stack<>();

        // Kiểm tra điểm bắt đầu có hợp lệ không
        if (!board.isValidPosition(startX, startY) || visited[startX][startY] || board.getStone(startX, startY) != Stone.EMPTY) {
            return;
        }

        stack.push(new Point(startX, startY));
        visited[startX][startY] = true; // Đánh dấu đã thăm ngay khi đẩy vào stack

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!stack.isEmpty()) {
            Point current = stack.pop();
            regionPoints.add(current); // Thêm điểm trống này vào vùng hiện tại

            // Kiểm tra 4 hàng xóm
            for (int[] dir : directions) {
                int nx = current.getX() + dir[0];
                int ny = current.getY() + dir[1];

                if (board.isValidPosition(nx, ny)) {
                    Stone neighborStone = board.getStone(nx, ny);
                    if (neighborStone == Stone.EMPTY) {
                        // Nếu là ô trống và chưa thăm, thêm vào stack để xét tiếp
                        if (!visited[nx][ny]) {
                            visited[nx][ny] = true; // Đánh dấu đã thăm
                            stack.push(new Point(nx, ny));
                        }
                    } else {
                        // Nếu là ô có quân (Đen hoặc Trắng), ghi nhận màu quân bao quanh
                        borderingColors.add(neighborStone);
                    }
                }
                // else: Nếu ra ngoài biên thì không làm gì cả
            }
        }
    }


    // Hàm đếm quân trên bàn (giữ nguyên)
    private int countStones(BoardState board, Stone color) {
        int count = 0;
        int size = board.getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getStone(x, y) == color) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- Lớp lồng cho kết quả tính lãnh thổ ---
    private static class TerritoryResult {
        final int blackTerritory;
        final int whiteTerritory;

        TerritoryResult(int blackTerritory, int whiteTerritory) {
            this.blackTerritory = blackTerritory;
            this.whiteTerritory = whiteTerritory;
        }
    }

    // --- Lớp lồng CapturedResult và Score (giữ nguyên) ---
    public static class CapturedResult {
        private final BoardState boardState;
        private final int capturedCount;

        public CapturedResult(BoardState boardState, int capturedCount) {
            this.boardState = boardState;
            this.capturedCount = capturedCount;
        }

        public BoardState getBoardState() { return boardState; }
        public int getCapturedCount() { return capturedCount; }
    }

    public static class Score {
        public final float blackScore;
        public final float whiteScore;

        public Score(float blackScore, float whiteScore) {
            this.blackScore = blackScore;
            this.whiteScore = whiteScore;
        }

        @Override
        public String toString() {
            return "Score{" + "Black=" + blackScore + ", White=" + whiteScore + '}';
        }
    }
}