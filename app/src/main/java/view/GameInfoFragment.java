package view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.gogameproject.R;
import model.GameState;
import model.Stone;

public class GameInfoFragment extends Fragment {
    private TextView turnTextView;
    private Button passButton;
    private Button resignButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_info, container, false);

        turnTextView = view.findViewById(R.id.turnTextView);
        passButton = view.findViewById(R.id.passButton);
        resignButton = view.findViewById(R.id.resignButton);

        passButton.setOnClickListener(v -> {
            if (getActivity() instanceof GameActivity) {
                ((GameActivity) getActivity()).onPass();
            }
        });
        resignButton.setOnClickListener(v -> {
            if (getActivity() instanceof GameActivity) {
                ((GameActivity) getActivity()).onResign();
            }
        });

        return view;
    }

    public void updateGameInfo(GameState gameState) {
        if (turnTextView != null) {
            String turn = gameState.getCurrentPlayer() == Stone.BLACK ? "Black's Turn" : "White's Turn";
            turnTextView.setText(turn);
        }
    }
}