package model;

import config.ScoringRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameLogic {
    public boolean isValidMove(Move move, GameState gameState) {
        if (move.isPass() || move.isResign()) {
            return true;
        }

        Point point = move.getPoint();
        BoardState currentBoard = gameState.getBoardState();

        if (currentBoard.getStone(point.getX(), point.getY()) != Stone.EMPTY) {
            return false;
        }

        BoardState tempBoard = currentBoard.copy();
        tempBoard.setStone(point.getX(), point.getY(), move.getColor());

        List<Point> captured = captureOpponentStones(tempBoard, point, move.getColor());
        for (Point p : captured) {
            tempBoard.setStone(p.getX(), p.getY(), Stone.EMPTY);
        }

        if (captured.isEmpty() && !hasLiberties(tempBoard, point, move.getColor())) {
            return false;
        }

        BoardState previousBoard = gameState.getPreviousBoardState();
        if (previousBoard != null && tempBoard.equals(previousBoard)) {
            return false;
        }

        return true;
    }

    public CapturedResult calculateNextBoardState(Move move, BoardState currentBoard) {
        if (move.isPass() || move.isResign()) {
            return new CapturedResult(currentBoard.copy(), 0);
        }

        Point point = move.getPoint();
        Stone color = move.getColor();
        BoardState nextBoard = currentBoard.copy();

        nextBoard.setStone(point.getX(), point.getY(), color);

        List<Point> captured = captureOpponentStones(nextBoard, point, color);
        for (Point p : captured) {
            nextBoard.setStone(p.getX(), p.getY(), Stone.EMPTY);
        }

        return new CapturedResult(nextBoard, captured.size());
    }

    /**
     * Tính điểm số cuối game.
     * @param state Trạng thái game.
     * @param rule Luật tính điểm (Nhật Bản/Trung Quốc).
     * @return Điểm số của Đen và Trắng.
     */
    public Score calculateScore(GameState state, ScoringRule rule) {
        BoardState board = state.getBoardState();
        int blackTerritory = countTerritory(board, Stone.BLACK);
        int whiteTerritory = countTerritory(board, Stone.WHITE);

        float blackScore;
        float whiteScore;

        if (rule == ScoringRule.JAPANESE) {
            blackScore = blackTerritory + state.getBlackCaptured();
            whiteScore = whiteTerritory + state.getWhiteCaptured() + state.getKomi();
        } else { // ScoringRule.CHINESE
            int blackStones = countStones(board, Stone.BLACK);
            int whiteStones = countStones(board, Stone.WHITE);
            blackScore = blackTerritory + blackStones;
            whiteScore = whiteTerritory + whiteStones + state.getKomi();
        }

        return new Score(blackScore, whiteScore);
    }

    private int countTerritory(BoardState board, Stone color) {
        int size = board.getSize();
        boolean[][] visited = new boolean[size][size];
        int territory = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (!visited[x][y] && board.getStone(x, y) == Stone.EMPTY) {
                    List<Point> region = new ArrayList<>();
                    boolean isTerritory = floodFill(board, x, y, color, visited, region);
                    if (isTerritory) {
                        territory += region.size();
                    }
                }
            }
        }
        return territory;
    }

    private boolean floodFill(BoardState board, int x, int y, Stone color, boolean[][] visited, List<Point> region) {
        if (!board.isValidPosition(x, y) || visited[x][y]) {
            return true;
        }

        Stone stone = board.getStone(x, y);
        if (stone != Stone.EMPTY && stone != color) {
            return false; // Vùng giáp với quân đối thủ
        }

        visited[x][y] = true;
        region.add(new Point(x, y));

        boolean isTerritory = true;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            isTerritory &= floodFill(board, nx, ny, color, visited, region);
        }
        return isTerritory;
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

    private boolean hasLiberties(BoardState board, Point point, Stone color) {
        Set<Point> visited = new HashSet<>();
        List<Point> group = new ArrayList<>();
        findGroup(board, point, color, group, visited);

        for (Point p : group) {
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

    public static class CapturedResult {
        private final BoardState boardState;
        private final int capturedCount;

        public CapturedResult(BoardState boardState, int capturedCount) {
            this.boardState = boardState;
            this.capturedCount = capturedCount;
        }

        public BoardState getBoardState() {
            return boardState;
        }

        public int getCapturedCount() {
            return capturedCount;
        }
    }

    public static class Score {
        public final float blackScore;
        public final float whiteScore;

        public Score(float blackScore, float whiteScore) {
            this.blackScore = blackScore;
            this.whiteScore = whiteScore;
        }
    }
}