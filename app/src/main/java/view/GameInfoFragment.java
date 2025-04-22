package view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.gogameproject.R;

import config.TimeControl;
import model.GameState;
import model.Stone;

public class GameInfoFragment extends Fragment {
    private TextView currentPlayerText;
    private TextView blackCapturedText;
    private TextView whiteCapturedText;
    private TextView blackTimeText;
    private TextView whiteTimeText;
    private LinearLayout blackCanadianMovesContainer;
    private TextView blackCanadianMovesText;
    private LinearLayout whiteCanadianMovesContainer;
    private TextView whiteCanadianMovesText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_info, container, false);

        // Khởi tạo views
        currentPlayerText = view.findViewById(R.id.current_player_text);
        blackCapturedText = view.findViewById(R.id.black_captured_text);
        whiteCapturedText = view.findViewById(R.id.white_captured_text);
        blackTimeText = view.findViewById(R.id.black_time_text);
        whiteTimeText = view.findViewById(R.id.white_time_text);
        blackCanadianMovesContainer = view.findViewById(R.id.black_canadian_moves_container);
        blackCanadianMovesText = view.findViewById(R.id.black_canadian_moves_text);
        whiteCanadianMovesContainer = view.findViewById(R.id.white_canadian_moves_container);
        whiteCanadianMovesText = view.findViewById(R.id.white_canadian_moves_text);

        return view;
    }

    public void updateGameInfo(GameState gameState, int blackMovesLeft, int whiteMovesLeft, long currentTimeLeft) {
        if (getView() == null || gameState == null) return;

        try {
            // Cập nhật lượt hiện tại
            Stone currentPlayer = gameState.getCurrentPlayer();
            currentPlayerText.setText(getString(R.string.current_player, currentPlayer.toString()));

            // Cập nhật số quân bị bắt
            blackCapturedText.setText(getString(R.string.captured, gameState.getBlackCaptured()));
            whiteCapturedText.setText(getString(R.string.captured, gameState.getWhiteCaptured()));

            // Cập nhật thời gian
            long blackDisplayTime = (currentPlayer == Stone.BLACK) ? currentTimeLeft : gameState.getBlackTimeLeft();
            long whiteDisplayTime = (currentPlayer == Stone.WHITE) ? currentTimeLeft : gameState.getWhiteTimeLeft();
            blackTimeText.setText(getString(R.string.time, formatTime(blackDisplayTime)));
            whiteTimeText.setText(getString(R.string.time, formatTime(whiteDisplayTime)));

            // Cập nhật Canadian Timing
            if (gameState.getTimeControl() == TimeControl.CANADIAN) {
                blackCanadianMovesContainer.setVisibility(View.VISIBLE);
                whiteCanadianMovesContainer.setVisibility(View.VISIBLE);
                blackCanadianMovesText.setText(getString(R.string.moves, blackMovesLeft));
                whiteCanadianMovesText.setText(getString(R.string.moves, whiteMovesLeft));
            } else {
                blackCanadianMovesContainer.setVisibility(View.GONE);
                whiteCanadianMovesContainer.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "Error updating game info: " + e.getMessage(), e);
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}