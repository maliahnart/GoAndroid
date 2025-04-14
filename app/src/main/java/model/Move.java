package model;

public class Move {
    private final Point point; // null nếu là pass hoặc resign
    private final Stone color;
    private final boolean isPass;
    private final boolean isResign;

    /**
     * Tạo một nước đi đặt quân.
     * @param point Tọa độ đặt quân.
     * @param color Màu quân (BLACK hoặc WHITE).
     */
    public Move(Point point, Stone color) {
        this.point = point;
        this.color = color;
        this.isPass = false;
        this.isResign = false;
    }

    /**
     * Tạo một nước đi đặc biệt (pass hoặc resign).
     * @param point null
     * @param color Màu quân.
     * @param isPass true nếu là pass.
     * @param isResign true nếu là resign.
     */
    public Move(Point point, Stone color, boolean isPass, boolean isResign) {
        this.point = point;
        this.color = color;
        this.isPass = isPass;
        this.isResign = isResign;
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
}