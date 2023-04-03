package ru.startandroid.develop.krestiki_noliki;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PlayerName extends AppCompatActivity {
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_name);

        final EditText playerNameEt = findViewById(R.id.playerNameEt);
        final AppCompatButton startGameBtn = findViewById(R.id.startGameBtn);
        final AppCompatButton startGameNick = findViewById(R.id.startGameNick);
        final TextView email = findViewById(R.id.emailFI);
        final TextView nick = findViewById(R.id.nick);
        final String getPlayerEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        email.setText(getPlayerEmail);
        String FIO[] = getPlayerEmail.split("\\.");
        nick.setText(FIO[0]);

        startGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String getPlayerName = playerNameEt.getText().toString();


                if(getPlayerName.isEmpty()){
                    Toast.makeText(PlayerName.this, "Пожалуйста, введите свое имя", Toast.LENGTH_SHORT).show();
                }
                else{
                    if(firebaseUser != null && firebaseUser.isEmailVerified()) {
                        Intent intent = new Intent(PlayerName.this, MainActivity.class);
                        intent.putExtra("playerName",getPlayerName);
                        startActivity(intent);
                        finish();

                    }
                }
            }
        });

        startGameNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String getPlayerName = FIO[0];
                if(firebaseUser != null && firebaseUser.isEmailVerified()) {
                    Intent intent = new Intent(PlayerName.this, MainActivity.class);
                    intent.putExtra("playerName",getPlayerName);
                    startActivity(intent);
                    finish();

                }
            }
        });
    }
}