package view;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gogameproject.R;

import config.ConfigLoader;
import config.GameConfig;
import config.GameMode;
import config.ScoringRule;
import config.TimeControl;
import controller.GameController;
import view.BoardView;
import view.GameInfoFragment;
import view.assets.ImageLoader;

public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GameActivity";
    private GameController gameController;
    private BoardView boardView;
    private GameInfoFragment gameInfoFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Khởi tạo views
        boardView = findViewById(R.id.boardView);
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundResource(R.drawable.bamboo);
        ImageLoader loader = new ImageLoader(this);
        Bitmap blackStone = loader.loadBitmap(R.drawable.black_stones);
        Bitmap whiteStone = loader.loadBitmap(R.drawable.white_stones);

        BoardView boardView = findViewById(R.id.boardView);
        boardView.setStoneBitmaps(blackStone, whiteStone);
        gameInfoFragment = (GameInfoFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Kiểm tra null
        if (boardView == null) {
            Log.e(TAG, "BoardView is null. Check activity_game.xml for id 'boardView'.");
            showErrorAndExit("Failed to initialize game: BoardView not found.");
            return;
        }
        if (gameInfoFragment == null) {
            Log.e(TAG, "GameInfoFragment is null. Check activity_game.xml for FragmentContainerView with id 'fragment_container'.");
            showErrorAndExit("Failed to initialize game: GameInfoFragment not found.");
            return;
        }

        // Lấy GameConfig
        ConfigLoader configLoader = new ConfigLoader(this);
        GameConfig config = configLoader.loadGameConfig();

        // Khởi tạo GameController
        try {
            gameController = new GameController(this, boardView, gameInfoFragment, config);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GameController: " + e.getMessage(), e);
            showErrorAndExit("Failed to initialize game: " + e.getMessage());
            return;
        }

        // Thiết lập các nút
        findViewById(R.id.pass_button).setOnClickListener(v -> gameController.handlePass());
        findViewById(R.id.resign_button).setOnClickListener(v -> gameController.handleResign());
        findViewById(R.id.undo_button).setOnClickListener(v -> gameController.handleUndo());
    }

    public void showGameOverDialog(String message, Runnable onPlayAgain, Runnable onExit) {
        new AlertDialog.Builder(this)
                .setTitle("End")
                .setMessage(message)
                .setPositiveButton("Play Again", (dialog, which) -> onPlayAgain.run())
                .setNegativeButton("Exit", (dialog, which) -> onExit.run())
                .setCancelable(false)
                .show();
    }



    public void restartGame(GameConfig config) {
        try {
            gameController = new GameController(this, boardView, gameInfoFragment, config);
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart game: " + e.getMessage(), e);
            showErrorAndExit("Failed to restart game: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}