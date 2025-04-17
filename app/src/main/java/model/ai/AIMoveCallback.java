package model.ai;

import model.Move;

public interface AIMoveCallback {
    /**
     * Được gọi khi AI đã tìm ra nước đi tốt nhất.
     * Phương thức này sẽ được thực thi trên luồng UI.
     * @param bestMove Nước đi được AI chọn (có thể là Pass).
     */
    void onMoveGenerated(Move bestMove);

    /**
     * (Optional) Được gọi nếu có lỗi xảy ra trong quá trình tính toán của AI.
     * @param e Exception xảy ra.
     */
    // void onAIError(Exception e);
}