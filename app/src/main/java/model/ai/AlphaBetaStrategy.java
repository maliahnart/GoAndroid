package model.ai;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import model.*;
import java.util.*;

/**
 * AI sử dụng Minimax với Alpha-Beta Pruning cho cờ vây, dùng trong chế độ PVB_MEDIUM.
 * Tích hợp AIStrategy với AIMoveCallback, chạy trong worker thread, tối ưu để tránh Android Runtime errors.
 */
public class AlphaBetaStrategy implements AIStrategy {
    private static final String TAG = "AlphaBetaStrategy";
    private static final int DEFAULT_MAX_DEPTH = 2; // Giảm để tránh stack overflow
    private static final int PASS_THRESHOLD = 100;
    private static final int CAPTURE_WEIGHT = 15;
    private static final int LIBERTY_WEIGHT = 1;
    private static final int ATARI_PENALTY = 50;
    private static final int ATARI_BONUS = 40;
    private static final int TERRITORY_WEIGHT = 10;
    private static final int ALIVE_BONUS = 100;
    private static final int FILL_EYE_PENALTY = -1000;
    private static final int MAX_CANDIDATE_POINTS = 30; // Giới hạn nước đi xét

    private final int maxDepth;
    private final Random random = new Random();
    private final GameLogic logic = new GameLogic();
    private Stone aiColor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public AlphaBetaStrategy() {
        this.maxDepth = DEFAULT_MAX_DEPTH;
    }

    public AlphaBetaStrategy(int depth) {
        this.maxDepth = depth;
    }

    @Override
    public Move generateMove(GameState gameState, Stone color, AIMoveCallback callback) {
        if (gameState == null || color == null || callback == null) {
            Log.e(TAG, "Invalid input: gameState=" + gameState + ", color=" + color + ", callback=" + callback);
            Move fallbackMove = new Move(null, color != null ? color : Stone.BLACK, true, false);
            mainThreadHandler.post(() -> callback.onMoveGenerated(fallbackMove));
            return fallbackMove;
        }

        AIMoveTask task = new AIMoveTask(callback);
        task.execute(gameState, color);
        // Trả về null vì nước đi sẽ được xử lý bất đồng bộ qua callback
        // Caller nên đợi callback thay vì dùng giá trị trả về trực tiếp
        return null;
    }

    private class AIMoveTask extends AsyncTask<Object, Void, Move> {
        private final AIMoveCallback callback;
        private Exception error;

        AIMoveTask(AIMoveCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Move doInBackground(Object... params) {
            GameState gameState = (GameState) params[0];
            Stone color = (Stone) params[1];
            try {
                long startTime = System.currentTimeMillis();
                aiColor = color;

                // Kiểm tra đối phương Pass
                boolean opponentPassed = false;
                List<Move> moveHistory = gameState.getMoveHistory();
                if (moveHistory != null && !moveHistory.isEmpty() && moveHistory.get(moveHistory.size() - 1).isPass()) {
                    opponentPassed = true;
                    Log.d(TAG, "Opponent passed. Prioritizing Pass unless strong move found.");
                }

                // Lấy nước đi hợp lệ
                List<Move> validMoves = getValidMoves(gameState, color);
                if (validMoves.isEmpty()) {
                    Log.d(TAG, "No valid moves. Passing. Time: " +
                            (System.currentTimeMillis() - startTime) + "ms");
                    return new Move(null, color, true, false);
                }
                validMoves.add(new Move(null, color, true, false));

                // Sắp xếp nước đi
// Cách sửa:
                validMoves.sort((Move m1, Move m2) -> {
                    // Tính điểm heuristic cho cả hai nước đi
                    int score1 = evaluateMoveBasicHeuristic(gameState, m1);
                    int score2 = evaluateMoveBasicHeuristic(gameState, m2);

                    // So sánh để sắp xếp giảm dần (điểm cao hơn đứng trước)
                    // Integer.compare(y, x) cho giảm dần nếu y là score2, x là score1
                    return Integer.compare(score2, score1);
                });
                Move bestMove = null;
                int bestScore = Integer.MIN_VALUE;
                int alpha = Integer.MIN_VALUE;
                int beta = Integer.MAX_VALUE;

                Log.d(TAG, "Evaluating " + validMoves.size() + " top-level moves at depth " + maxDepth);

                for (Move move : validMoves) {
                    GameState nextState = applyMoveToNewState(gameState, move);
                    if (nextState == null) {
                        Log.w(TAG, "Skipping invalid move: " + move);
                        continue;
                    }

                    int score = alphaBetaSearch(nextState, maxDepth - 1, alpha, beta, false);
                    Log.v(TAG, "Move: " + move + ", Score: " + score);

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = move;
                        Log.d(TAG, "New best move: " + bestMove + " with score: " + bestScore);
                    }

                    alpha = Math.max(alpha, bestScore);
                }

                // Ưu tiên Pass nếu đối phương Pass và không có nước tốt
                if (opponentPassed && bestScore < PASS_THRESHOLD) {
                    Log.d(TAG, "Opponent passed and best score (" + bestScore +
                            ") below threshold. Passing. Time: " +
                            (System.currentTimeMillis() - startTime) + "ms");
                    return new Move(null, color, true, false);
                }

                if (bestMove == null) {
                    Log.w(TAG, "No best move found. Passing as fallback.");
                    bestMove = new Move(null, color, true, false);
                }

                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Selected move: " + bestMove + ", Score: " + bestScore +
                        ", Time: " + (endTime - startTime) + "ms");
                return bestMove;
            } catch (Exception e) {
                Log.e(TAG, "Error in AIMoveTask: " + e.getMessage(), e);
                this.error = e;
                return new Move(null, color, true, false);
            }
        }

        @Override
        protected void onPostExecute(Move bestMove) {
            if (error != null) {
                Log.e(TAG, "AI move error: " + error.getMessage(), error);
            }
            mainThreadHandler.post(() -> callback.onMoveGenerated(bestMove));
        }
    }

    private int alphaBetaSearch(GameState state, int depth, int alpha, int beta, boolean maximizingPlayer) {
        try {
            if (state == null || state.getBoardState() == null) {
                Log.e(TAG, "Invalid state in alphaBetaSearch");
                return 0;
            }
            if (state.isGameOver() || depth == 0) {
                return evaluateState(state, aiColor);
            }

            Stone currentPlayer = state.getCurrentPlayer();
            if (currentPlayer == null) {
                Log.e(TAG, "Current player is null");
                return 0;
            }

            List<Move> validMoves = getValidMoves(state, currentPlayer);
            validMoves.add(new Move(null, currentPlayer, true, false));

            validMoves.sort((Move m1, Move m2) -> {
                // Tính điểm heuristic cho cả hai nước đi
                int score1 = evaluateMoveBasicHeuristic(state, m1);
                int score2 = evaluateMoveBasicHeuristic(state, m2);

                // So sánh để sắp xếp giảm dần (điểm cao hơn đứng trước)
                // Integer.compare(y, x) cho giảm dần nếu y là score2, x là score1
                return Integer.compare(score2, score1);
            });
            if (maximizingPlayer) {
                int maxEval = Integer.MIN_VALUE;
                for (Move move : validMoves) {
                    GameState nextState = applyMoveToNewState(state, move);
                    if (nextState == null) continue;

                    int eval = alphaBetaSearch(nextState, depth - 1, alpha, beta, false);
                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, eval);

                    if (beta <= alpha) {
                        break;
                    }
                }
                return maxEval;
            } else {
                int minEval = Integer.MAX_VALUE;
                for (Move move : validMoves) {
                    GameState nextState = applyMoveToNewState(state, move);
                    if (nextState == null) continue;

                    int eval = alphaBetaSearch(nextState, depth - 1, alpha, beta, true);
                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, eval);

                    if (beta <= alpha) {
                        break;
                    }
                }
                return minEval;
            }
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in alphaBetaSearch: " + e.getMessage(), e);
            return 0;
        }
    }

    private List<Move> getValidMoves(GameState gameState, Stone color) {
        List<Move> validMoves = new ArrayList<>();
        try {
            if (gameState == null || gameState.getBoardState() == null || color == null) {
                Log.e(TAG, "Invalid input in getValidMoves: gameState=" + gameState + ", color=" + color);
                return validMoves;
            }

            BoardState board = gameState.getBoardState();
            Set<Point> candidates = getCandidatePoints(board);

            if (candidates.isEmpty()) {
                int center = board.getSize() / 2;
                if (board.getStone(center, center) == Stone.EMPTY) {
                    candidates.add(new Point(center, center));
                }
            }

            for (Point point : candidates) {
                Move testMove = new Move(point, color);
                if (logic.isValidMove(testMove, gameState)) {
                    validMoves.add(testMove);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in getValidMoves: " + e.getMessage(), e);
        }
        return validMoves;
    }

    private GameState applyMoveToNewState(GameState originalState, Move move) {
        try {
            if (originalState == null || originalState.getBoardState() == null || move == null) {
                Log.e(TAG, "Invalid input in applyMoveToNewState: state=" + originalState + ", move=" + move);
                return null;
            }

            GameState nextState = new GameState(originalState);
            if (move.isPass()) {
                nextState.recordMove(move, nextState.getBoardState(), 0);
            } else {
                GameLogic.CapturedResult result = logic.calculateNextBoardState(move, originalState.getBoardState());
                if (result == null || result.getBoardState() == null) {
                    Log.e(TAG, "Invalid move or null result: " + move);
                    return null;
                }
                nextState.recordMove(move, result.getBoardState(), result.getCapturedCount());
            }
            return nextState;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in applyMoveToNewState: " + e.getMessage(), e);
            return null;
        }
    }

    private int evaluateState(GameState gameState, Stone aiColor) {
        try {
            if (gameState == null || gameState.getBoardState() == null || aiColor == null) {
                Log.e(TAG, "Invalid input in evaluateState: gameState=" + gameState + ", aiColor=" + aiColor);
                return 0;
            }

            int score = 0;
            Stone opponentColor = aiColor.opponent();
            BoardState board = gameState.getBoardState();
            int boardSize = board.getSize();

            // Quân bắt
            int aiCaptured = aiColor == Stone.BLACK ? gameState.getWhiteCaptured() : gameState.getBlackCaptured();
            int opponentCaptured = aiColor == Stone.BLACK ? gameState.getBlackCaptured() : gameState.getWhiteCaptured();
            score += (aiCaptured - opponentCaptured) * CAPTURE_WEIGHT;

            // Khí và Atari
            int aiLiberties = 0, opponentLiberties = 0;
            int aiAtariGroups = 0, opponentAtariGroups = 0;
            int aiAliveGroups = 0, opponentAliveGroups = 0;
            Set<Point> visited = new HashSet<>();

            for (int x = 0; x < boardSize; x++) {
                for (int y = 0; y < boardSize; y++) {
                    Point p = new Point(x, y);
                    if (!visited.contains(p) && board.getStone(x, y) != Stone.EMPTY) {
                        Stone stone = board.getStone(x, y);
                        int liberties = countLiberties(board, p, stone, visited);
                        if (stone == aiColor) {
                            aiLiberties += liberties;
                            if (liberties == 1) aiAtariGroups++;
                            if (liberties >= 4 || hasTwoEyes(board, p, stone)) aiAliveGroups++;
                        } else if (stone == opponentColor) {
                            opponentLiberties += liberties;
                            if (liberties == 1) opponentAtariGroups++;
                            if (liberties >= 4 || hasTwoEyes(board, p, stone)) opponentAliveGroups++;
                        }
                    }
                }
            }

            score += (aiLiberties - opponentLiberties) * LIBERTY_WEIGHT;
            score -= aiAtariGroups * ATARI_PENALTY;
            score += opponentAtariGroups * ATARI_BONUS;
            score += aiAliveGroups * ALIVE_BONUS;
            score -= opponentAliveGroups * ALIVE_BONUS;

            // Vùng lãnh thổ
            int aiTerritory = countTerritory(board, aiColor);
            int opponentTerritory = countTerritory(board, opponentColor);
            score += (aiTerritory - opponentTerritory) * TERRITORY_WEIGHT;

            // Game over
            if (gameState.isGameOver()) {
                int finalScore = (aiCaptured - opponentCaptured) * CAPTURE_WEIGHT +
                        (aiTerritory - opponentTerritory) * TERRITORY_WEIGHT;
                if (gameState.getEndGameReason() != null && gameState.getEndGameReason().contains(aiColor.opponent() + " wins")) {
                    return Integer.MIN_VALUE / 2;
                } else if (gameState.getEndGameReason() != null && gameState.getEndGameReason().contains(aiColor + " wins")) {
                    return Integer.MAX_VALUE / 2;
                }
                return finalScore;
            }

            return score;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in evaluateState: " + e.getMessage(), e);
            return 0;
        }
    }

    private int evaluateMoveBasicHeuristic(GameState state, Move move) {
        try {
            if (state == null || state.getBoardState() == null || move == null) {
                Log.e(TAG, "Invalid input in evaluateMoveBasicHeuristic");
                return Integer.MIN_VALUE;
            }

            if (move.isPass()) {
                int emptyCount = countEmptyPoints(state.getBoardState());
                return -emptyCount + (state.getConsecutivePasses() * 50);
            }

            int score = 0;
            Point point = move.getPoint();
            Stone myColor = move.getColor();
            BoardState board = state.getBoardState();
            GameLogic.CapturedResult result = logic.calculateNextBoardState(move, board);

            if (result != null && result.getCapturedCount() > 0) {
                score += 100 * result.getCapturedCount();
            }

            for (Point neighbor : getNeighbors(point, board.getSize())) {
                if (board.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                        board.getStone(neighbor.getX(), neighbor.getY()) == myColor &&
                        isInAtari(board, neighbor, myColor) &&
                        result != null && result.getBoardState() != null &&
                        !isInAtari(result.getBoardState(), neighbor, myColor)) {
                    score += 90;
                    break;
                }
            }

            if (result != null && result.getBoardState() != null) {
                BoardState nextBoard = result.getBoardState();
                for (Point neighbor : getNeighbors(point, nextBoard.getSize())) {
                    if (nextBoard.isValidPosition(neighbor.getX(), neighbor.getY()) &&
                            nextBoard.getStone(neighbor.getX(), neighbor.getY()) == myColor.opponent() &&
                            isInAtari(nextBoard, neighbor, myColor.opponent())) {
                        score += 20;
                        break;
                    }
                }
            }

            if (isFillingTrueEye(board, point, myColor)) {
                score += FILL_EYE_PENALTY;
            }

            return score;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in evaluateMoveBasicHeuristic: " + e.getMessage(), e);
            return Integer.MIN_VALUE;
        }
    }

    private int countEmptyPoints(BoardState board) {
        try {
            if (board == null) {
                Log.e(TAG, "Board is null in countEmptyPoints");
                return 0;
            }
            int count = 0;
            for (int x = 0; x < board.getSize(); x++) {
                for (int y = 0; y < board.getSize(); y++) {
                    if (board.getStone(x, y) == Stone.EMPTY) count++;
                }
            }
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in countEmptyPoints: " + e.getMessage(), e);
            return 0;
        }
    }

    private Set<Point> getCandidatePoints(BoardState board) {
        Set<Point> candidates = new HashSet<>();
        try {
            if (board == null) {
                Log.e(TAG, "Board is null in getCandidatePoints");
                return candidates;
            }

            int boardSize = board.getSize();
            int radius = boardSize > 9 ? 1 : 2; // Giảm bán kính trên 19x19
            boolean stonesPresent = false;

            for (int x = 0; x < boardSize; x++) {
                for (int y = 0; y < boardSize; y++) {
                    if (board.getStone(x, y) != Stone.EMPTY) {
                        stonesPresent = true;
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dy = -radius; dy <= radius; dy++) {
                                if (dx == 0 && dy == 0) continue;
                                int nx = x + dx;
                                int ny = y + dy;
                                if (board.isValidPosition(nx, ny) && board.getStone(nx, ny) == Stone.EMPTY) {
                                    candidates.add(new Point(nx, ny));
                                    if (candidates.size() >= MAX_CANDIDATE_POINTS) {
                                        return candidates; // Giới hạn số điểm
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!stonesPresent) {
                candidates.add(new Point(boardSize / 2, boardSize / 2));
                if (boardSize >= 9) {
                    candidates.add(new Point(3, 3));
                    candidates.add(new Point(3, boardSize - 4));
                    candidates.add(new Point(boardSize - 4, 3));
                    candidates.add(new Point(boardSize - 4, boardSize - 4));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in getCandidatePoints: " + e.getMessage(), e);
        }
        return candidates;
    }

    private int countLiberties(BoardState board, Point startPoint, Stone color, Set<Point> visitedOverall) {
        try {
            if (board == null || startPoint == null || color == null) {
                Log.e(TAG, "Invalid input in countLiberties");
                return 0;
            }

            Set<Point> group = new HashSet<>();
            Set<Point> liberties = new HashSet<>();
            Stack<Point> stack = new Stack<>();
            int maxGroupSize = board.getSize() * board.getSize() / 2; // Giới hạn kích thước nhóm

            if (!board.isValidPosition(startPoint.getX(), startPoint.getY()) ||
                    board.getStone(startPoint.getX(), startPoint.getY()) != color ||
                    !visitedOverall.add(startPoint)) {
                return 0;
            }

            stack.push(startPoint);
            group.add(startPoint);

            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            while (!stack.isEmpty() && group.size() < maxGroupSize) {
                Point current = stack.pop();
                for (int[] dir : directions) {
                    int nx = current.getX() + dir[0];
                    int ny = current.getY() + dir[1];
                    Point neighbor = new Point(nx, ny);
                    if (board.isValidPosition(nx, ny)) {
                        Stone neighborStone = board.getStone(nx, ny);
                        if (neighborStone == Stone.EMPTY) {
                            liberties.add(neighbor);
                        } else if (neighborStone == color && !group.contains(neighbor)) {
                            if (visitedOverall.add(neighbor)) {
                                group.add(neighbor);
                                stack.push(neighbor);
                            }
                        }
                    }
                }
            }
            return liberties.size();
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in countLiberties: " + e.getMessage(), e);
            return 0;
        }
    }

    private boolean isInAtari(BoardState board, Point point, Stone color) {
        try {
            if (board == null || point == null || color == null) {
                Log.e(TAG, "Invalid input in isInAtari");
                return false;
            }
            if (!board.isValidPosition(point.getX(), point.getY()) ||
                    board.getStone(point.getX(), point.getY()) != color) {
                return false;
            }

            Set<Point> visited = new HashSet<>();
            return countLiberties(board, point, color, visited) == 1;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in isInAtari: " + e.getMessage(), e);
            return false;
        }
    }

    private boolean isFillingTrueEye(BoardState board, Point point, Stone color) {
        try {
            if (board == null || point == null || color == null) {
                Log.e(TAG, "Invalid input in isFillingTrueEye");
                return false;
            }
            if (board.getStone(point.getX(), point.getY()) != Stone.EMPTY) return false;

            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[][] diagonals = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

            for (int[] dir : directions) {
                int nx = point.getX() + dir[0];
                int ny = point.getY() + dir[1];
                if (board.isValidPosition(nx, ny) && board.getStone(nx, ny) != color) {
                    return false;
                }
            }

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

            for (int[] dir : directions) {
                int nx = point.getX() + dir[0];
                int ny = point.getY() + dir[1];
                if (board.isValidPosition(nx, ny)) {
                    Set<Point> visited = new HashSet<>();
                    int liberties = countLiberties(board, new Point(nx, ny), color, visited);
                    if (liberties < 2 && !hasTwoEyes(board, new Point(nx, ny), color)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in isFillingTrueEye: " + e.getMessage(), e);
            return false;
        }
    }

    private boolean hasTwoEyes(BoardState board, Point startPoint, Stone color) {
        try {
            if (board == null || startPoint == null || color == null) {
                Log.e(TAG, "Invalid input in hasTwoEyes");
                return false;
            }

            Set<Point> group = new HashSet<>();
            Stack<Point> stack = new Stack<>();
            stack.push(startPoint);
            group.add(startPoint);
            int maxGroupSize = board.getSize() * board.getSize() / 2;

            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            while (!stack.isEmpty() && group.size() < maxGroupSize) {
                Point current = stack.pop();
                for (int[] dir : directions) {
                    int nx = current.getX() + dir[0];
                    int ny = current.getY() + dir[1];
                    Point neighbor = new Point(nx, ny);
                    if (board.isValidPosition(nx, ny) && board.getStone(nx, ny) == color && !group.contains(neighbor)) {
                        group.add(neighbor);
                        stack.push(neighbor);
                    }
                }
            }

            int eyeCount = 0;
            for (int x = 0; x < board.getSize(); x++) {
                for (int y = 0; y < board.getSize(); y++) {
                    Point p = new Point(x, y);
                    if (board.getStone(x, y) == Stone.EMPTY && isFillingTrueEye(board, p, color)) {
                        boolean adjacentToGroup = false;
                        for (Point g : group) {
                            for (Point n : getNeighbors(p, board.getSize())) {
                                if (n.equals(g)) {
                                    adjacentToGroup = true;
                                    break;
                                }
                            }
                            if (adjacentToGroup) break;
                        }
                        if (adjacentToGroup) eyeCount++;
                        if (eyeCount >= 2) return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in hasTwoEyes: " + e.getMessage(), e);
            return false;
        }
    }

    private int countTerritory(BoardState board, Stone color) {
        try {
            if (board == null || color == null) {
                Log.e(TAG, "Invalid input in countTerritory");
                return 0;
            }

            int boardSize = board.getSize();
            boolean[][] visited = new boolean[boardSize][boardSize];
            int territory = 0;

            for (int x = 0; x < boardSize; x++) {
                for (int y = 0; y < boardSize; y++) {
                    if (!visited[x][y] && board.getStone(x, y) == Stone.EMPTY) {
                        Set<Point> region = new HashSet<>();
                        boolean isTerritory = floodFill(board, x, y, color, visited, region);
                        if (isTerritory) territory += region.size();
                    }
                }
            }
            return territory;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in countTerritory: " + e.getMessage(), e);
            return 0;
        }
    }

    private boolean floodFill(BoardState board, int x, int y, Stone color, boolean[][] visited, Set<Point> region) {
        try {
            if (!board.isValidPosition(x, y) || visited[x][y]) return true;
            Stone stone = board.getStone(x, y);
            if (stone != Stone.EMPTY && stone != color) return false;

            visited[x][y] = true;
            region.add(new Point(x, y));

            boolean isTerritory = true;
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!floodFill(board, nx, ny, color, visited, region)) {
                    isTerritory = false;
                }
            }
            return isTerritory;
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in floodFill: " + e.getMessage(), e);
            return false;
        }
    }

    private List<Point> getNeighbors(Point p, int boardSize) {
        List<Point> neighbors = new ArrayList<>();
        try {
            if (p == null) {
                Log.e(TAG, "Point is null in getNeighbors");
                return neighbors;
            }

            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int nx = p.getX() + dir[0];
                int ny = p.getY() + dir[1];
                if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                    neighbors.add(new Point(nx, ny));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Runtime error in getNeighbors: " + e.getMessage(), e);
        }
        return neighbors;
    }
}