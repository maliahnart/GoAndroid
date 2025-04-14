package view;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gogameproject.R;
import config.GameConfig;
import config.GameMode;
import config.ScoringRule;
import config.TimeControl;
import controller.SettingsController;

public class SettingActivity extends AppCompatActivity {
    private Spinner boardSizeSpinner, gameModeSpinner, timeControlSpinner, scoringRuleSpinner;
    private EditText komiEditText, timeLimitEditText;
    private Button saveButton;
    private SettingsController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        controller = new SettingsController(this);

        // Khởi tạo các view
        boardSizeSpinner = findViewById(R.id.boardSizeSpinner);
        gameModeSpinner = findViewById(R.id.gameModeSpinner);
        timeControlSpinner = findViewById(R.id.timeControlSpinner);
        scoringRuleSpinner = findViewById(R.id.scoringRuleSpinner);
        komiEditText = findViewById(R.id.komiEditText);
        timeLimitEditText = findViewById(R.id.timeLimitEditText);
        saveButton = findViewById(R.id.saveButton);

        // Cài đặt adapter cho board size
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[]{"9x9", "13x13", "19x19"});
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boardSizeSpinner.setAdapter(sizeAdapter);

        // Cài đặt adapter cho game mode
        ArrayAdapter<GameMode> modeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, GameMode.values());
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameModeSpinner.setAdapter(modeAdapter);

        // Cài đặt adapter cho time control
        ArrayAdapter<TimeControl> timeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TimeControl.values());
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeControlSpinner.setAdapter(timeAdapter);

        // Cài đặt adapter cho scoring rule
        ArrayAdapter<ScoringRule> scoringAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ScoringRule.values());
        scoringAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scoringRuleSpinner.setAdapter(scoringAdapter);

        // Thiết lập giá trị mặc định
        GameConfig currentConfig = controller.loadSettings();
        boardSizeSpinner.setSelection(getBoardSizeIndex(currentConfig.getBoardSize()));
        gameModeSpinner.setSelection(currentConfig.getGameMode().ordinal());
        timeControlSpinner.setSelection(currentConfig.getTimeControl().ordinal());
        scoringRuleSpinner.setSelection(currentConfig.getScoringRule().ordinal());
        komiEditText.setText(String.valueOf(currentConfig.getKomi()));
        timeLimitEditText.setText(String.valueOf(currentConfig.getTimeLimit()));

        // Xử lý sự kiện lưu cài đặt
        saveButton.setOnClickListener(this::saveSettings);
    }

    private int getBoardSizeIndex(int boardSize) {
        switch (boardSize) {
            case 9:
                return 0;
            case 13:
                return 1;
            case 19:
                return 2;
            default:
                return 0;
        }
    }

    private void saveSettings(View v) {
        try {
            // Lấy kích thước bàn cờ
            int boardSize;
            String sizeText = boardSizeSpinner.getSelectedItem().toString();
            switch (sizeText) {
                case "9x9":
                    boardSize = 9;
                    break;
                case "13x13":
                    boardSize = 13;
                    break;
                default:
                    boardSize = 19;
            }

            // Lấy điểm komi
            float komi;
            try {
                komi = Float.parseFloat(komiEditText.getText().toString());
                if (komi < 0 || komi > 50) {
                    throw new NumberFormatException("Komi must be between 0 and 50");
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Komi, using default 6.5", Toast.LENGTH_SHORT).show();
                komi = 6.5f;
            }

            // Lấy thời gian giới hạn
            int timeLimit;
            try {
                timeLimit = Integer.parseInt(timeLimitEditText.getText().toString());
                if (timeLimit < 0) {
                    throw new NumberFormatException("Time limit must be non-negative");
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid time limit, using default 30 minutes", Toast.LENGTH_SHORT).show();
                timeLimit = 30 * 60; // Mặc định 30 phút (tính bằng giây)
            }

            // Lấy các giá trị khác
            GameMode mode = (GameMode) gameModeSpinner.getSelectedItem();
            TimeControl timeControl = (TimeControl) timeControlSpinner.getSelectedItem();
            ScoringRule scoringRule = (ScoringRule) scoringRuleSpinner.getSelectedItem();

            // Lưu cài đặt
            controller.saveSettings(boardSize, komi, mode, timeControl, timeLimit, scoringRule);
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}