package view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import model.BoardState;
import model.Point;
import model.Stone;

public class BoardView extends View {
    private BoardState boardState;
    private int boardSize;
    private float cellSize;

    private Paint gridPaint, shadowPaint;

    private Bitmap blackStoneBitmap, whiteStoneBitmap;

    private static final int WOOD_LIGHT = Color.rgb(245, 222, 179);
    private static final int WOOD_DARK = Color.rgb(210, 180, 140);
    private static final int BORDER_COLOR = Color.rgb(139, 69, 19);
    private static final int SHADOW_COLOR = Color.argb(150, 50, 50, 50);

    private OnStonePlacedListener listener;

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public BoardView(Context context) {
        super(context);
        initPaints();
    }

    private void initPaints() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setAntiAlias(true);

        shadowPaint = new Paint();
        shadowPaint.setColor(SHADOW_COLOR);
        shadowPaint.setAntiAlias(true);
    }

    public void setStoneBitmaps(Bitmap blackStone, Bitmap whiteStone) {
        this.blackStoneBitmap = blackStone;
        this.whiteStoneBitmap = whiteStone;
        invalidate();
    }

    public void setBoardState(BoardState boardState) {
        this.boardState = boardState;
        this.boardSize = boardState.getSize();
        requestLayout();
        invalidate();
    }

    public void setOnStonePlacedListener(OnStonePlacedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
        if (boardSize > 0) {
            cellSize = (float) size / boardSize;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (boardState == null) return;

        Paint bgPaint = new Paint();
        for (int i = 0; i < boardSize; i++) {
            bgPaint.setColor(i % 2 == 0 ? WOOD_LIGHT : WOOD_DARK);
            canvas.drawRect(0, i * cellSize, getWidth(), (i + 1) * cellSize, bgPaint);
        }
        Paint borderPaint = new Paint();
        borderPaint.setColor(BORDER_COLOR);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8f);
        canvas.drawRect(new RectF(cellSize / 2, cellSize / 2,
                getWidth() - cellSize / 2, getHeight() - cellSize / 2), borderPaint);

        for (int i = 0; i < boardSize; i++) {
            float pos = i * cellSize + cellSize / 2;
            canvas.drawLine(cellSize / 2, pos, getWidth() - cellSize / 2, pos, gridPaint);
            canvas.drawLine(pos, cellSize / 2, pos, getHeight() - cellSize / 2, gridPaint);
        }

        // Vẽ điểm sao
        Paint starPaint = new Paint();
        starPaint.setColor(Color.BLACK);
        float radius = cellSize / 10;

        int[][] starPoints = getStarPoints(boardSize);
        for (int[] point : starPoints) {
            float cx = (point[0] + 0.5f) * cellSize;
            float cy = (point[1] + 0.5f) * cellSize;
            canvas.drawCircle(cx, cy, radius, starPaint);
        }

        // Vẽ quân cờ
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                Stone stone = boardState.getStone(x, y);
                if (stone != Stone.EMPTY) {
                    float cx = (x + 0.5f) * cellSize;
                    float cy = (y + 0.5f) * cellSize;
                    float radius_shadow = cellSize * 0.45f;

                    // Bóng đổ
                    canvas.drawCircle(cx + 3, cy + 3, radius_shadow, shadowPaint);

                    Bitmap stoneBitmap = (stone == Stone.BLACK) ? blackStoneBitmap : whiteStoneBitmap;

                    if (stoneBitmap != null) {
                        RectF dest = new RectF(
                                cx - radius_shadow,
                                cy - radius_shadow,
                                cx + radius_shadow,
                                cy + radius_shadow
                        );
                        canvas.drawBitmap(stoneBitmap, null, dest, null);
                    } else {
                        // Fallback nếu không có ảnh
                        Paint fallbackPaint = new Paint();
                        fallbackPaint.setAntiAlias(true);
                        fallbackPaint.setColor(stone == Stone.BLACK ? Color.BLACK : Color.WHITE);
                        canvas.drawCircle(cx, cy, radius, fallbackPaint);
                    }
                }
            }
        }
    }

    private int[][] getStarPoints(int boardSize) {
        switch (boardSize) {
            case 4:
                // Không có điểm sao cho 4x4
                return new int[][] {};
            case 9:
                // 5 điểm: 4 góc (cách mép 2 đường) và tâm
                return new int[][] {
                        {2, 2}, {2, 6}, {6, 2}, {6, 6}, // Góc
                        {4, 4} // Tâm
                };
            case 13:
                // 9 điểm: 4 góc (cách mép 3 đường), 4 cạnh, tâm
                return new int[][] {
                        {3, 3}, {3, 9}, {9, 3}, {9, 9}, // Góc
                        {3, 6}, {6, 3}, {9, 6}, {6, 9}, // Cạnh
                        {6, 6} // Tâm
                };
            case 19:
                // 9 điểm: 4 góc (cách mép 3 đường), 4 cạnh, tâm
                return new int[][] {
                        {3, 3}, {3, 15}, {15, 3}, {15, 15}, // Góc
                        {3, 9}, {9, 3}, {15, 9}, {9, 15}, // Cạnh
                        {9, 9} // Tâm
                };
            default:
                return new int[][] {};
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && listener != null) {
            float x = event.getX();
            float y = event.getY();
            int boardX = Math.round((x - cellSize / 2) / cellSize);
            int boardY = Math.round((y - cellSize / 2) / cellSize);
            if (boardX >= 0 && boardX < boardSize && boardY >= 0 && boardY < boardSize) {
                listener.onStonePlaced(new Point(boardX, boardY));
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public interface OnStonePlacedListener {
        void onStonePlaced(Point point);
    }
}