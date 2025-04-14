package model.ai;


import model.GameState;
import model.Move;
import model.Stone;

public abstract class AbstractAIStrategy implements AIStrategy{
    protected boolean isValidMove(Move move, GameState gameState) {
        // TODO: Kết nối với GameLogic sau
        return true;
    }
}
