package view;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gogameproject.R;
import config.ConfigLoader;
import config.GameConfig;
import controller.GameController;
import model.GameState;
import model.GameLogic;
import model.Move;
import model.Point;
import model.Stone;
import view.assets.ImageLoader;

/**
 * Activity chính để chơi game cờ vây.
 */
public class GameActivity extends AppCompatActivity {
    private BoardView boardView;
    private GameController gameController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundResource(R.drawable.bamboo);

        // Tải cấu hình từ ConfigLoader
        ConfigLoader configLoader = new ConfigLoader(this);
        GameConfig config = configLoader.loadGameConfig();

        // Thiết lập BoardView
        boardView = findViewById(R.id.boardView);
        ImageLoader loader = new ImageLoader(this);
        Bitmap blackStone = loader.loadBitmap(R.drawable.black_stones);
        Bitmap whiteStone = loader.loadBitmap(R.drawable.white_stones);

        BoardView boardView = findViewById(R.id.boardView);
        boardView.setStoneBitmaps(blackStone, whiteStone);


        // Thêm GameInfoFragment
        GameInfoFragment fragment = new GameInfoFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }

        // Khởi tạo GameController
        gameController = new GameController(this, boardView, fragment, config);
    }

    public void onPass() {
        gameController.handlePass();
    }

    public void onResign() {
        gameController.handleResign();
    }
}