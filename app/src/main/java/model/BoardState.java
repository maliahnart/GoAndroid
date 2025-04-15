package model;

import config.GameConfig;

import java.util.Arrays;
import java.util.Objects;

/**
 * Lớp biểu diễn trạng thái bàn cờ trong trò chơi Cờ Vây.
 */
public class BoardState {
    private final Stone[][] board;
    private final int size;
    private final GameConfig config;

    public BoardState(GameConfig config) {
        this.config = config;
        this.size = config.getBoardSize();
        this.board = new Stone[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = Stone.EMPTY;
            }
        }
    }

    public GameConfig getConfig() {
        return config;
    }

    public BoardState copy() {
        BoardState copy = new BoardState(config);
        for (int i = 0; i < size; i++) {
            System.arraycopy(board[i], 0, copy.board[i], 0, size);
        }
        return copy;
    }

    public Stone getStone(int x, int y) {
        if (isValidPosition(x, y)) {
            return board[x][y];
        }
        throw new IllegalArgumentException("Invalid position: (" + x + ", " + y + ")");
    }

    public void setStone(int x, int y, Stone stone) {
        if (isValidPosition(x, y)) {
            board[x][y] = stone;
        } else {
            throw new IllegalArgumentException("Invalid position: (" + x + ", " + y + ")");
        }
    }

    public int getSize() {
        return size;
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    public String hash() {
        StringBuilder sb = new StringBuilder(size * size);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                sb.append(board[i][j].ordinal());
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardState that = (BoardState) o;
        return size == that.size && Arrays.deepEquals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, Arrays.deepHashCode(board));
    }
}