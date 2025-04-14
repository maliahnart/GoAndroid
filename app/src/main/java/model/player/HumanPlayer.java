package model.player;
import model.GameState;
import model.Move;
import model.Stone;
public class HumanPlayer implements Player {
    private final Stone color;

    public HumanPlayer(Stone color) {
        this.color = color;
    }

    @Override
    public Stone getColor() {
        return color;
    }

    @Override
    public Move generateMove(GameState gameState) {
        // TODO: Sẽ được gọi từ GameController khi nhận input từ BoardView
        throw new UnsupportedOperationException("Human player move will be handled by UI");
    }
}
