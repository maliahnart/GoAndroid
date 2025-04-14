package com.example.gogameproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import config.GameConfig;
import config.GameMode;
import model.BoardState;
import view.BoardView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        BoardView boardView = findViewById(R.id.boardView);
        GameConfig config = new GameConfig();
        BoardState boardState = new BoardState(config);
        boardState.setStone(4, 4,model.Stone.BLACK);
        boardView.setBoardState(boardState);
    }
}