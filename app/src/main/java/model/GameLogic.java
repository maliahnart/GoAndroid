package model;

import config.ScoringRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class GameLogic {

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

        // 5. Kiểm tra luật Ko (Ko Rule) bằng cách so sánh với lịch sử bàn cờ
        List<Move> moveHistory = gameState.getMoveHistory();
        for (Move pastMove : moveHistory) {
            BoardState pastBoardState = pastMove.getBoardState();
            if (pastBoardState != null && tempBoard.equals(pastBoardState)) {
                return false; // Lặp lại trạng thái bàn cờ trong lịch sử -> vi phạm Ko
            }
        }

        return true;
    }

    public CapturedResult calculateNextBoardState(Move move, BoardState currentBoard) {
        if (move.isPass() || move.isResign()) {
            // Trả về bản sao và không có quân bị bắt
            return new CapturedResult(currentBoard.copy(), 0, new ArrayList<>());
        }

        Point point = move.getPoint();
        Stone color = move.getColor();
        BoardState nextBoard = currentBoard.copy();

        nextBoard.setStone(point.getX(), point.getY(), color);

        // Sử dụng hàm capture để lấy danh sách quân bị bắt
        List<Point> captured = captureOpponentStonesIfNoLiberties(nextBoard, point, color);
        for (Point p : captured) {
            nextBoard.setStone(p.getX(), p.getY(), Stone.EMPTY);
        }

        return new CapturedResult(nextBoard, captured.size(), captured);
    }

    private boolean hasLiberties(BoardState board, Point startPoint, Stone color) {
        if (!board.isValidPosition(startPoint.getX(), startPoint.getY()) || board.getStone(startPoint.getX(), startPoint.getY()) != color) {
            return false;
        }
        Set<Point> group = new HashSet<>();
        Set<Point> liberties = new HashSet<>();
        Stack<Point> stack = new Stack<>();
        Set<Point> visited = new HashSet<>();

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

                if (board.isValidPosition(nx, ny)) {
                    Stone neighborStone = board.getStone(nx, ny);
                    if (neighborStone == Stone.EMPTY) {
                        liberties.add(neighbor);
                    } else if (neighborStone == color && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        stack.push(neighbor);
                    }
                }
            }
        }
        return !liberties.isEmpty();
    }

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

    private List<Point> captureOpponentStonesIfNoLiberties(BoardState board, Point lastMovePoint, Stone playerColor) {
        List<Point> totalCaptured = new ArrayList<>();
        Stone opponentColor = playerColor.opponent();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        Set<Point> processedOpponentGroups = new HashSet<>();

        for (int[] dir : directions) {
            int nx = lastMovePoint.getX() + dir[0];
            int ny = lastMovePoint.getY() + dir[1];
            Point neighbor = new Point(nx, ny);

            if (board.isValidPosition(nx, ny)
                    && board.getStone(nx, ny) == opponentColor
                    && !processedOpponentGroups.contains(neighbor)) {

                List<Point> opponentGroup = new ArrayList<>();
                Set<Point> visitedForGroup = new HashSet<>();
                findGroup(board, neighbor, opponentColor, opponentGroup, visitedForGroup);

                if (!opponentGroup.isEmpty() && !hasLiberties(board, opponentGroup.get(0), opponentColor)) {
                    totalCaptured.addAll(opponentGroup);
                }
                processedOpponentGroups.addAll(opponentGroup);
            }
        }
        return totalCaptured;
    }

    /**
     * Tính điểm số cuối game.
     * QUAN TRỌNG: Hàm này giả định tất cả các quân trên bàn là SỐNG.
     * Cần phải có bước xử lý quân chết TRƯỚC KHI gọi hàm này.
     * @param state Trạng thái game (đã xử lý quân chết nếu cần).
     * @param rule Luật tính điểm (Nhật Bản/Trung Quốc).
     * @return Điểm số của Đen và Trắng.
     */
    public Score calculateScore(GameState state, ScoringRule rule) {
        BoardState board = state.getBoardState();

        TerritoryResult territory = calculateTerritory(board);

        float blackScore;
        float whiteScore;

        if (rule == ScoringRule.JAPANESE) {
            blackScore = territory.blackTerritory + state.getBlackCaptured();
            whiteScore = territory.whiteTerritory + state.getWhiteCaptured() + state.getKomi();
        } else { // ScoringRule.CHINESE
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
        boolean[][] visited = new boolean[size][size];
        int blackTerritory = 0;
        int whiteTerritory = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getStone(x, y) == Stone.EMPTY && !visited[x][y]) {
                    Set<Point> regionPoints = new HashSet<>();
                    Set<Stone> borderingColors = new HashSet<>();

                    floodFillForTerritory(board, x, y, visited, regionPoints, borderingColors);

                    if (borderingColors.size() == 1) {
                        if (borderingColors.contains(Stone.BLACK)) {
                            blackTerritory += regionPoints.size();
                        } else if (borderingColors.contains(Stone.WHITE)) {
                            whiteTerritory += regionPoints.size();
                        }
                    }
                }
            }
        }
        return new TerritoryResult(blackTerritory, whiteTerritory);
    }

    /**
     * Hàm Flood Fill để tìm vùng trống và các màu quân bao quanh.
     * Sử dụng Stack để tránh lỗi tràn bộ nhớ đệ quy (StackOverflowError).
     * @param board Trạng thái bàn cờ.
     * @param startX Tọa độ x bắt đầu.
     * @param startY Tọa độ y bắt đầu.
     * @param visited Mảng 2D theo dõi các ô trống đã được thăm.
     * @param regionPoints Set để thu thập các điểm thuộc vùng trống hiện tại.
     * @param borderingColors Set để thu thập các màu quân (BLACK, WHITE) tiếp giáp.
     */
    private void floodFillForTerritory(BoardState board, int startX, int startY, boolean[][] visited,
                                       Set<Point> regionPoints, Set<Stone> borderingColors) {
        int size = board.getSize();
        Stack<Point> stack = new Stack<>();

        if (!board.isValidPosition(startX, startY) || visited[startX][startY] || board.getStone(startX, startY) != Stone.EMPTY) {
            return;
        }

        stack.push(new Point(startX, startY));
        visited[startX][startY] = true;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!stack.isEmpty()) {
            Point current = stack.pop();
            regionPoints.add(current);

            for (int[] dir : directions) {
                int nx = current.getX() + dir[0];
                int ny = current.getY() + dir[1];

                if (board.isValidPosition(nx, ny)) {
                    Stone neighborStone = board.getStone(nx, ny);
                    if (neighborStone == Stone.EMPTY) {
                        if (!visited[nx][ny]) {
                            visited[nx][ny] = true;
                            stack.push(new Point(nx, ny));
                        }
                    } else {
                        borderingColors.add(neighborStone);
                    }
                }
            }
        }
    }

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

    private static class TerritoryResult {
        final int blackTerritory;
        final int whiteTerritory;

        TerritoryResult(int blackTerritory, int whiteTerritory) {
            this.blackTerritory = blackTerritory;
            this.whiteTerritory = whiteTerritory;
        }
    }

    public static class CapturedResult {
        private final BoardState boardState;
        private final int capturedCount;
        private final List<Point> capturedPoints;

        public CapturedResult(BoardState boardState, int capturedCount, List<Point> capturedPoints) {
            this.boardState = boardState;
            this.capturedCount = capturedCount;
            this.capturedPoints = capturedPoints != null ? new ArrayList<>(capturedPoints) : new ArrayList<>();
        }

        public BoardState getBoardState() {
            return boardState;
        }

        public int getCapturedCount() {
            return capturedCount;
        }

        public List<Point> getCapturedPoints() {
            return new ArrayList<>(capturedPoints);
        }
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
            return "Score{Black=" + blackScore + ", White=" + whiteScore + '}';
        }
    }
}