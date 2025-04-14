package model.player;
import model.GameState;
import model.Move;
import model.Stone;
import model.ai.AIStrategy;

public class BotPlayer implements Player {
    private final Stone color;
    private final AIStrategy strategy;

    public BotPlayer(Stone color, AIStrategy strategy) {
        this.color = color;
        this.strategy = strategy;
    }

    @Override
    public Stone getColor() {
        return color;
    }

    @Override
    public Move generateMove(GameState gameState) {
        return strategy.generateMove(gameState, color);
    }
}
