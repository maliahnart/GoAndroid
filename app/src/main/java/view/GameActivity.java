package view;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
        // Tạo dialog với layout tùy chỉnh
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_game_over, null);
        builder.setView(dialogView);

        // Ánh xạ views
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button playAgainButton = dialogView.findViewById(R.id.play_again_button);
        Button exitButton = dialogView.findViewById(R.id.exit_button);

        // Thiết lập nội dung
        titleView.setText("Game End");
        messageView.setText(message);

        // Tạo dialog
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Thiết lập sự kiện nút
        playAgainButton.setOnClickListener(v -> {
            dialog.dismiss(); // Đóng dialog trước khi khởi động lại
            onPlayAgain.run();
        });
        exitButton.setOnClickListener(v -> {
            dialog.dismiss(); // Đóng dialog trước khi thoát
            onExit.run();
        });

        // Hiển thị dialog
        dialog.show();
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
        // Tạo dialog với layout tùy chỉnh
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_error, null);
        builder.setView(dialogView);

        // Ánh xạ views
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button okButton = dialogView.findViewById(R.id.ok_button);

        // Thiết lập nội dung
        titleView.setText("Error");
        messageView.setText(message);

        // Thiết lập sự kiện nút
        okButton.setOnClickListener(v -> {
            finish();
            builder.create().dismiss();
        });

        // Hiển thị dialog
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }
}