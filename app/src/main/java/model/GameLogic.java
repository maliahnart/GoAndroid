package model;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameLogic {
    /**
     * Kiểm tra nước đi có hợp lệ không.
     * @param move Nước đi cần kiểm tra.
     * @param gameState Trạng thái game hiện tại.
     * @return true nếu nước đi hợp lệ.
     */
    public boolean isValidMove(Move move, GameState gameState) {
        if (move.isPass() || move.isResign()) {
            return true;
        }

        Point point = move.getPoint();
        BoardState board = gameState.getBoardState();

        // Kiểm tra vị trí có trống không
        if (board.getStone(point.getX(), point.getY()) != Stone.EMPTY) {
            return false;
        }

        // Thử đặt quân và kiểm tra luật (tự do, tự sát, Ko)
        BoardState tempBoard = board.copy();
        tempBoard.setStone(point.getX(), point.getY(), move.getColor());

        // Kiểm tra tự sát (không có tự do sau khi đặt)
        if (!hasLiberties(tempBoard, point, move.getColor())) {
            // Cho phép nếu nước đi này bắt được quân đối thủ
            List<Point> captured = captureOpponentStones(tempBoard, point, move.getColor());
            return !captured.isEmpty() || hasLiberties(tempBoard, point, move.getColor());
        }

        // Kiểm tra luật Ko (so sánh với trạng thái trước đó)
        // TODO: Cần lưu lịch sử bàn cờ trong GameState để kiểm tra đầy đủ
        return true;
    }

    /**
     * Áp dụng một nước đi vào game.
     * @param move Nước đi cần áp dụng.
     * @param gameState Trạng thái game hiện tại.
     */
    public void applyMove(Move move, GameState gameState) {
        if (move.isPass() || move.isResign()) {
            gameState.addMove(move);
            if (!move.isResign()) {
                gameState.setCurrentPlayer(gameState.getCurrentPlayer().opponent());
            }
            return;
        }

        Point point = move.getPoint();
        BoardState board = gameState.getBoardState();

        // Đặt quân
        board.setStone(point.getX(), point.getY(), move.getColor());

        // Bắt quân đối thủ
        List<Point> captured = captureOpponentStones(board, point, move.getColor());
        for (Point p : captured) {
            board.setStone(p.getX(), p.getY(), Stone.EMPTY);
        }

        // Thêm vào lịch sử và chuyển lượt
        gameState.addMove(move);
        gameState.setCurrentPlayer(gameState.getCurrentPlayer().opponent());
    }

    /**
     * Kiểm tra xem một nhóm quân tại điểm point có tự do không.
     */
    private boolean hasLiberties(BoardState board, Point point, Stone color) {
        Set<Point> visited = new HashSet<>();
        List<Point> group = new ArrayList<>();
        findGroup(board, point, color, group, visited);

        for (Point p : group) {
            // Kiểm tra các điểm lân cận
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int nx = p.getX() + dir[0];
                int ny = p.getY() + dir[1];
                if (board.isValidPosition(nx, ny) && board.getStone(nx, ny) == Stone.EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tìm nhóm quân cùng màu bắt đầu từ điểm point.
     */
    private void findGroup(BoardState board, Point point, Stone color, List<Point> group, Set<Point> visited) {
        if (!board.isValidPosition(point.getX(), point.getY()) || visited.contains(point)) {
            return;
        }
        if (board.getStone(point.getX(), point.getY()) != color) {
            return;
        }

        visited.add(point);
        group.add(point);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            Point next = new Point(point.getX() + dir[0], point.getY() + dir[1]);
            findGroup(board, next, color, group, visited);
        }
    }

    /**
     * Bắt quân đối thủ xung quanh điểm point.
     * @return Danh sách các điểm quân bị bắt.
     */
    private List<Point> captureOpponentStones(BoardState board, Point point, Stone color) {
        List<Point> captured = new ArrayList<>();
        Stone opponent = color.opponent();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int nx = point.getX() + dir[0];
            int ny = point.getY() + dir[1];
            Point neighbor = new Point(nx, ny);
            if (board.isValidPosition(nx, ny) && board.getStone(nx, ny) == opponent) {
                List<Point> group = new ArrayList<>();
                Set<Point> visited = new HashSet<>();
                findGroup(board, neighbor, opponent, group, visited);
                if (!hasLiberties(board, neighbor, opponent)) {
                    captured.addAll(group);
                }
            }
        }
        return captured;
    }
}
