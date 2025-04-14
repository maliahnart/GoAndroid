package model;

import config.GameConfig;
import config.GameMode;
/**
 * Lớp biểu diễn trạng thái bàn cờ trong trò chơi cờ vây.
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
            for (int j = 0; j < size; j++) {
                copy.board[i][j] = this.board[i][j];
            }
        }
        return copy;
    }
    /**
     * Lấy trạng thái ô tại vị trí (x, y).
     * @param x Tọa độ hàng.
     * @param y Tọa độ cột.
     * @return Stone tại vị trí (x, y).
     * @throws IllegalArgumentException nếu vị trí không hợp lệ.
     */
    public Stone getStone(int x, int y) {
        if (isValidPosition(x, y)) {
            return board[x][y];
        }
        throw new IllegalArgumentException("Invalid position: (" + x + ", " + y + ")");
    }

    /**
     * Đặt quân tại vị trí (x, y).
     * @param x Tọa độ hàng.
     * @param y Tọa độ cột.
     * @param stone Màu quân (BLACK, WHITE, hoặc EMPTY).
     * @throws IllegalArgumentException nếu vị trí không hợp lệ.
     */
    public void setStone(int x, int y, Stone stone) {
        if (isValidPosition(x, y)) {
            board[x][y] = stone;
        } else {
            throw new IllegalArgumentException("Invalid position: (" + x + ", " + y + ")");
        }
    }

    /**
     * Lấy kích thước bàn cờ.
     * @return Kích thước (số ô mỗi chiều).
     */
    public int getSize() {
        return size;
    }

    /**
     * Kiểm tra vị trí (x, y) có hợp lệ không.
     * @param x Tọa độ hàng.
     * @param y Tọa độ cột.
     * @return True nếu vị trí hợp lệ, false nếu không.
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    /**
     * Sao chép trạng thái bàn cờ.
     * @return Một bản sao của BoardState với cùng kích thước và trạng thái.
     */
}