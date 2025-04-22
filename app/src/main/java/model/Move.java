package model;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private final Point point; // null if pass or resign
    private final Stone color; // BLACK or WHITE
    private final boolean isPass; // True if pass
    private final boolean isResign; // True if resign
    private final BoardState boardState; // Board state before the move
    private final List<Point> capturedStones; // Positions of captured stones

    /**
     * Create a move for placing a stone.
     * @param point Coordinates of the move.
     * @param color Stone color (BLACK or WHITE).
     * @param boardState Board state before the move.
     * @param capturedStones List of captured stone positions.
     */
    public Move(Point point, Stone color, BoardState boardState, List<Point> capturedStones) {
        this.point = point;
        this.color = color;
        this.isPass = false;
        this.isResign = false;
        this.boardState = boardState != null ? boardState.copy() : null;
        this.capturedStones = capturedStones != null ? new ArrayList<>(capturedStones) : new ArrayList<>();
    }

    /**
     * Create a special move (pass or resign).
     * @param point null
     * @param color Stone color.
     * @param isPass True if pass.
     * @param isResign True if resign.
     * @param boardState Board state before the move.
     * @param capturedStones List of captured stone positions (empty for pass/resign).
     */
    public Move(Point point, Stone color, boolean isPass, boolean isResign, BoardState boardState, List<Point> capturedStones) {
        this.point = point;
        this.color = color;
        this.isPass = isPass;
        this.isResign = isResign;
        this.boardState = boardState != null ? boardState.copy() : null;
        this.capturedStones = capturedStones != null ? new ArrayList<>(capturedStones) : new ArrayList<>();
    }

    public Point getPoint() {
        return point;
    }

    public Stone getColor() {
        return color;
    }

    public boolean isPass() {
        return isPass;
    }

    public boolean isResign() {
        return isResign;
    }

    public BoardState getBoardState() {
        return boardState != null ? boardState.copy() : null;
    }

    public List<Point> getCapturedStones() {
        return new ArrayList<>(capturedStones);
    }
}