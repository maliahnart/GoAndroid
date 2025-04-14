package model;

public enum Stone {
    EMPTY,
    BLACK,
    WHITE;

    /**
     * Trả về màu quân đối thủ.
     * @return BLACK nếu là WHITE, WHITE nếu là BLACK, EMPTY nếu là EMPTY.
     */
    public Stone opponent() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}