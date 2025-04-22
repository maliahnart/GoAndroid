//package model.ai;
//
//import android.os.AsyncTask;
//import android.util.Log;
//import model.*;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Random;
//import java.util.Set;
//
//public class MCTSStrategy implements AIStrategy {
//    private static final String TAG = "MCTSStrategy";
//    private final Random random = new Random();
//    private final int simulations;
//    private final GameLogic logic;
//
//    public MCTSStrategy(int simulations) {
//        this.simulations = simulations;
//        this.logic = new GameLogic();
//    }
//
//    @Override
//    public Move generateMove(GameState gameState, Stone color, AIMoveCallback callback) {
//        if (gameState == null || color == null || callback == null) {
//            Log.e(TAG, "Invalid input: gameState=" + gameState + ", color=" + color + ", callback=" + callback);
//            if (callback != null) {
//                callback.onMoveGenerated(new Move(null, color != null ? color : Stone.BLACK, true, false, null, new ArrayList<>()));
//            }
//            return null;
//        }
//
//        new MCTSMoveTask(gameState, color, callback).execute();
//        return null; // Move returned via callback
//    }
//
//    private class MCTSMoveTask extends AsyncTask<Void, Void, Move> {
//        private final GameState gameState;
//        private final Stone color;
//        private final AIMoveCallback callback;
//        private Exception error;
//
//        MCTSMoveTask(GameState gameState, Stone color, AIMoveCallback callback) {
//            this.gameState = gameState;
//            this.color = color;
//            this.callback = callback;
//        }
//
//        @Override
//        protected Move doInBackground(Void... params) {
//            try {
//                long startTime = System.currentTimeMillis();
//                BoardState board = gameState.getBoardState();
//                if (board == null) {
//                    Log.e(TAG, "BoardState is null");
//                    return new Move(null, color, true, false, null, new ArrayList<>());
//                }
//
//                // Get candidate moves (near existing stones)
//                List<Point> validPoints = getValidCandidatePoints(board, gameState);
//                Log.d(TAG, "Valid candidate points: " + validPoints.size());
//
//                if (validPoints.isEmpty()) {
//                    Log.d(TAG, "No valid moves. Passing. Time: " + (System.currentTimeMillis() - startTime) + "ms");
//                    return new Move(null, color, true, false, null, new ArrayList<>());
//                }
//
//                // Select best move via MCTS
//                Move bestMove = selectBestMove(gameState, color, validPoints);
//                Log.d(TAG, "Selected move: " + bestMove + ", time: " + (System.currentTimeMillis() - startTime) + "ms");
//                return bestMove;
//            } catch (Exception e) {
//                Log.e(TAG, "Error generating move: " + e.getMessage(), e);
//                this.error = e;
//                return new Move(null, color, true, false, null, new ArrayList<>());
//            }
//        }
//
//        @Override
//        protected void onPostExecute(Move move) {
//            if (error != null) {
//                Log.e(TAG, "MCTS task failed: " + error.getMessage(), error);
//            }
//            callback.onMoveGenerated(move);
//        }
//    }
//
//    /**
//     * Get valid candidate points near existing stones (radius 2 for 9x9, 1 for larger).
//     */
//    private List<Point> getValidCandidatePoints(BoardState board, GameState gameState) {
//        Set<Point> candidates = new HashSet<>();
//        int boardSize = board.getSize();
//        int radius = boardSize > 9 ? 1 : 2;
//
//        // Find empty points near existing stones
//        for (int x = 0; x < boardSize; x++) {
//            for (int y = 0; y < boardSize; y++) {
//                if (board.getStone(x, y) != Stone.EMPTY) {
//                    for (int dx = -radius; dx <= radius; dx++) {
//                        for (int dy = -radius; dy <= radius; dy++) {
//                            int nx = x + dx;
//                            int ny = y + dy;
//                            if (board.isValidPosition(nx, ny) && board.getStone(nx, ny) == Stone.EMPTY) {
//                                candidates.add(new Point(nx, ny));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // Fallback: add center point if no candidates
//        if (candidates.isEmpty()) {
//            int center = boardSize / 2;
//            candidates.add(new Point(center, center));
//        }
//
//        // Validate candidates
//        List<Point> validPoints = new ArrayList<>();
//        for (Point point : candidates) {
//            Move move = new Move(point, gameState.getCurrentPlayer(), false, false, null, new ArrayList<>());
//            if (logic.isValidMove(move, gameState)) {
//                validPoints.add(point);
//            }
//        }
//        return validPoints;
//    }
//
//    /**
//     * Select the best move based on simulation win rates.
//     */
//    private Move selectBestMove(GameState gameState, Stone color, List<Point> validPoints) {
//        double bestWinRate = -1.0;
//        List<Move> bestMoves = new ArrayList<>();
//
//        for (Point point : validPoints) {
//            Move move = new Move(point, color, false, false, null, new ArrayList<>());
//            double winRate = evaluateMove(gameState, move);
//            Log.v(TAG, "Move " + point + ": winRate=" + winRate);
//            if (winRate > bestWinRate) {
//                bestWinRate = winRate;
//                bestMoves.clear();
//                bestMoves.add(move);
//            } else if (Math.abs(winRate - bestWinRate) < 0.0001) {
//                bestMoves.add(move);
//            }
//        }
//
//        // Randomly select among best moves
//        return bestMoves.get(random.nextInt(bestMoves.size()));
//    }
//
//    /**
//     * Evaluate a move by running random playout simulations.
//     * Returns the win rate (wins/simulations).
//     */
//    private double evaluateMove(GameState originalState, Move move) {
//        int wins = 0;
//        Stone myColor = move.getColor();
//        int maxMoves = originalState.getBoardState().getSize() * originalState.getBoardState().getSize();
//
//        for (int i = 0; i < simulations; i++) {
//            GameState simState = new GameState(originalState);
//            BoardState simBoard = simState.getBoardState();
//
//            // Apply the candidate move
//            GameLogic.CapturedResult result = logic.calculateNextBoardState(move, simBoard);
//            if (result == null || result.getBoardState() == null) {
//                continue; // Skip invalid simulation
//            }
//            simState.recordMove(
//                    new Move(move.getPoint(), move.getColor(), move.isPass(), move.isResign(), result.getBoardState(), result.getCapturedPoints()),
//                    result.getBoardState(),
//                    result.getCapturedCount()
//            );
//
//            // Simulate random playout
//            boolean gameEnded = false;
//            int moveCount = 0;
//            while (!gameEnded && moveCount < maxMoves) {
//                List<Point> simValidPoints = getValidSimulationPoints(simBoard, simState);
//                Move simMove;
//                if (simValidPoints.isEmpty()) {
//                    simMove = new Move(null, simState.getCurrentPlayer(), true, false, null, new ArrayList<>());
//                } else {
//                    Point point = simValidPoints.get(random.nextInt(simValidPoints.size()));
//                    simMove = new Move(point, simState.getCurrentPlayer(), false, false, null, new ArrayList<>());
//                }
//
//                result = logic.calculateNextBoardState(simMove, simBoard);
//                if (result == null || result.getBoardState() == null) {
//                    gameEnded = true;
//                    continue;
//                }
//                simState.recordMove(
//                        new Move(simMove.getPoint(), simMove.getColor(), simMove.isPass(), simMove.isResign(), result.getBoardState(), result.getCapturedPoints()),
//                        result.getBoardState(),
//                        result.getCapturedCount()
//                );
//                simBoard = result.getBoardState();
//
//                // Check game end (two consecutive passes)
//                List<Move> history = simState.getMoveHistory();
//                if (history.size() >= 2 && history.get(history.size() - 1).isPass() &&
//                        history.get(history.size() - 2).isPass()) {
//                    gameEnded = true;
//                }
//                moveCount++;
//            }
//
//            // Evaluate final state (stones + captured)
//            int myScore = countStones(simBoard, myColor) +
//                    (myColor == Stone.BLACK ? simState.getWhiteCaptured() : simState.getBlackCaptured());
//            int opponentScore = countStones(simBoard, myColor.opponent()) +
//                    (myColor == Stone.BLACK ? simState.getBlackCaptured() : simState.getWhiteCaptured());
//            if (myScore > opponentScore) {
//                wins++;
//            }
//        }
//
//        double winRate = simulations > 0 ? (double) wins / simulations : 0.0;
//        Log.d(TAG, "Evaluated move: point=" + move.getPoint() + ", winRate=" + winRate + ", wins=" + wins + "/" + simulations);
//        return winRate;
//    }
//
//    /**
//     * Get valid points for simulation (faster, full board scan).
//     */
//    private List<Point> getValidSimulationPoints(BoardState board, GameState gameState) {
//        List<Point> validPoints = new ArrayList<>();
//        for (int x = 0; x < board.getSize(); x++) {
//            for (int y = 0; y < board.getSize(); y++) {
//                if (board.getStone(x, y) == Stone.EMPTY) {
//                    Move move = new Move(new Point(x, y), gameState.getCurrentPlayer(), false, false, null, new ArrayList<>());
//                    if (logic.isValidMove(move, gameState)) {
//                        validPoints.add(new Point(x, y));
//                    }
//                }
//            }
//        }
//        return validPoints;
//    }
//
//    /**
//     * Count stones of a color on the board.
//     */
//    private int countStones(BoardState board, Stone color) {
//        int count = 0;
//        for (int x = 0; x < board.getSize(); x++) {
//            for (int y = 0; y < board.getSize(); y++) {
//                if (board.getStone(x, y) == color) {
//                    count++;
//                }
//            }
//        }
//        return count;
//    }
//}