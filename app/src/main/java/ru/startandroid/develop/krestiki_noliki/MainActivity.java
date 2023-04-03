package ru.startandroid.develop.krestiki_noliki;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private LinearLayout player1Layout, player2Layout;
    private ImageView image1, image2, image3, image4, image5, image6, image7, image8, image9;
    private TextView player1TV,player2TV, E1, E2;

    private final List<int[]> combinationslist = new ArrayList<>();

    private String playerUniqueId = "0";

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://tictactoe-267fa-default-rtdb.firebaseio.com/");

    private boolean opponentFound = false;

    private String opponentUniqueId = "0";

    private String status = "matching";

    private String playerTurn = " ";

    private String connectionId = "";

    ValueEventListener turnsEventListener, wonEventListener;

    private final List<String> doneBoxes = new ArrayList<>();

    private final String[] boxesSelectedBy = {" ", " ", " ", " ", " ", " ", " ", " ", " "};

    // +++ rav
    private static final String lg = "KX DEBUG:";
    private boolean playerInGame = false;
    private long gameTimeout = 3; // minutes
    private String playerName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player1Layout = findViewById(R.id.player1Layout);
        player2Layout = findViewById(R.id.player2Layout);

        image1 = findViewById(R.id.image1);
        image2 = findViewById(R.id.image2);
        image3 = findViewById(R.id.image3);
        image4 = findViewById(R.id.image4);
        image5 = findViewById(R.id.image5);
        image6 = findViewById(R.id.image6);
        image7 = findViewById(R.id.image7);
        image8 = findViewById(R.id.image8);
        image9 = findViewById(R.id.image9);

        player1TV = findViewById(R.id.player1TV);
        player2TV = findViewById(R.id.player2TV);



        final String getPlayerName = getIntent().getStringExtra("playerName");

        // +++ rav
        playerName = getIntent().getStringExtra("playerName");


        combinationslist.add(new int[]{0,1,2});
        combinationslist.add(new int[]{3,4,5});
        combinationslist.add(new int[]{6,7,8});
        combinationslist.add(new int[]{0,3,6});
        combinationslist.add(new int[]{1,4,7});
        combinationslist.add(new int[]{2,5,8});
        combinationslist.add(new int[]{2,4,6});
        combinationslist.add(new int[]{0,4,8});


        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Подождите, идет поиск оппонента");
        progressDialog.show();

        String FIO[] = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("\\.");
        String F = String.join(" ", FIO);
        playerUniqueId = F;

        player1TV.setText(getPlayerName);



        // +++ rav
        databaseReference.child("connections").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(lg, "onDataChange: started");

                if (
                        (connectionId.length() > 1) &&
                                (snapshot.child(connectionId).getChildrenCount() == 2) &&
                                (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Long.valueOf(connectionId)) < gameTimeout)
                ) {
                    // Go game
                    String firstMove = "";
                    for ( DataSnapshot players : snapshot.child(connectionId).getChildren() ) {
                        String gamePlayer = players.getKey();
                        if (firstMove.length() == 0) {
                            firstMove = gamePlayer;
                        }
                        if (!playerUniqueId.equals(gamePlayer)) {
                            opponentUniqueId = gamePlayer;
                        }
                    }
                    Log.d(lg, "onDataChange: game " + connectionId + " between " + playerUniqueId + " and " + opponentUniqueId);


                    player2TV.setText(snapshot.child(connectionId).child(opponentUniqueId).child("player_name").getValue(String.class));

                    databaseReference.child("turns").child(connectionId).addValueEventListener(turnsEventListener);
                    databaseReference.child("won").child(connectionId).addValueEventListener(wonEventListener);

                    playerTurn = firstMove;
                    applyPlayerTurn(firstMove);

                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    databaseReference.child("connections").removeEventListener(this);
                    playerInGame = true;
                }
                if (playerInGame) { return; }

                boolean newGame = false;
                if ( snapshot.hasChildren() ) {
                    Log.d(lg, "onDataChange: connections founded");

                    // looking for game
                    for (DataSnapshot connections : snapshot.getChildren() ) {
                        String gameID = connections.getKey();
                        long playerTime = System.currentTimeMillis();

                        long playersCount = connections.getChildrenCount();
                        long connectionTime = Long.valueOf(gameID);
                        long connectionTimeout = TimeUnit.MILLISECONDS.toMinutes(playerTime - connectionTime);

                        Log.d(lg, "onDataChange: Connection " + connections.getKey() + " has " + playersCount + " players, timeout " + connectionTimeout + " mins");


                        if (
                                (playersCount == 1) &&
                                        (connectionTimeout <= gameTimeout)
                        ) {
                            Log.d(lg, "onDataChange: is place in game " + gameID);
                            // Check players
                            boolean playerExists = false;
                            for ( DataSnapshot players : connections.getChildren() ) {
                                String gamePlayer = players.getKey();
                                Log.d(lg, "onDataChange: players " + gamePlayer + " == " + playerUniqueId);
                                if (playerUniqueId.equals(gamePlayer)) {
                                    playerExists = true;
                                    playerInGame = true;
                                    newGame = false;
                                    connectionId = gameID;
                                    Log.d(lg, "onDataChange: player " + playerUniqueId + " in game " + gameID);
                                    break; // for ( DataSnapshot players
                                }
                            }

                            if (!playerExists) {
                                // Jump to game
                                Log.d(lg, "onDataChange: player " + playerUniqueId + " jump to game " + gameID);
                                snapshot.child(gameID).child(playerUniqueId).child("player_name").getRef().setValue(playerName);
                                playerExists = true;
                                playerInGame = true;
                                newGame = false;
                                connectionId = gameID;
                                break; // for (DataSnapshot connections
                            }
                        } else {
                            Log.d(lg, "onDataChange: game " + gameID + " abandoned");
                        }
                    }

                    if (!playerInGame) {
                        Log.d(lg, "onDataChange: player " + playerUniqueId + " didn't found in all games");
                        newGame = true;
                    }
                } else {
                    // snapshot hasn't Children()
                    Log.d(lg, "onDataChange: no connections");
                    newGame = true;
                }

                if (newGame) {
                    // Create a game
                    String connectionUniqueId = String.valueOf(System.currentTimeMillis());
                    Log.d(lg, "onDataChange: new game " + String.valueOf(connectionUniqueId));

                    snapshot.child(connectionUniqueId).child(playerUniqueId).child("player_name").getRef().setValue(playerName);
                    playerInGame = true;
                    connectionId = connectionUniqueId;
                }

                Log.d(lg, "onDataChange: ended");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(lg, "onCancelled: started");
                // TODO Something
                Log.d(lg, "onCancelled: ended");
            }
        });

        turnsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    if(dataSnapshot.getChildrenCount() == 2){
                        final int getBoxPosition = Integer.parseInt(dataSnapshot.child("box_position").getValue(String.class));
                        final String getPlayerId = dataSnapshot.child("player_id").getValue(String.class);
                        if(!doneBoxes.contains(String.valueOf(getBoxPosition))){
                            doneBoxes.add(String.valueOf(getBoxPosition));
                            if(getBoxPosition == 1) { selectBox(image1, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 2) { selectBox(image2, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 3) { selectBox(image3, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 4) { selectBox(image4, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 5) { selectBox(image5, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 6) { selectBox(image6, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 7) { selectBox(image7, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 8) { selectBox(image8, getBoxPosition, getPlayerId); }
                            else if(getBoxPosition == 9) { selectBox(image9, getBoxPosition, getPlayerId); }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };




        wonEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChild("player_id")){
                    String getWinPlayerId = snapshot.child("player_id").getValue(String.class);
                    final WinDialog winDialog;
                    if(getWinPlayerId.equals(playerUniqueId)){
                        winDialog = new WinDialog(MainActivity.this, "Ты победил");
                    }
                    else{
                        winDialog = new WinDialog(MainActivity.this, "Ты проиграл");
                    }
                    winDialog.setCancelable(false);
                    winDialog.show();

                    databaseReference.child("turns").child(connectionId).removeEventListener(turnsEventListener);
                    databaseReference.child("won").child(connectionId).removeEventListener(wonEventListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };




        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(lg, "onClick 1: " + playerTurn);
                if(!doneBoxes.contains("1") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("1");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("2") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("2");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("3") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("3");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("4") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("4");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("5") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("5");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("6") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("6");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("7") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("7");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("8") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("8");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });

        image9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!doneBoxes.contains("9") && playerTurn.equals(playerUniqueId)){
                    ((ImageView)v).setImageResource(R.drawable.cross_icon);

                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("9");
                    databaseReference.child("turns").child(connectionId).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);

                    playerTurn = opponentUniqueId;
                }
            }
        });
    }

    private void applyPlayerTurn(String playerUniqueId2){
        if(playerUniqueId2.equals(playerUniqueId)){
            //player1Layout.setBackgroundResource(R.drawable.round_back_dark_blue_stroke);
            player1Layout.setBackgroundResource(R.drawable.main_grad);
            AnimationDrawable animationDrawable = (AnimationDrawable) player1Layout.getBackground();
            animationDrawable.setEnterFadeDuration(2000);
            animationDrawable.setExitFadeDuration(4000);
            animationDrawable.start();

            player2Layout.setBackgroundResource(R.drawable.round_back_dark_blue_20);
        }
        else{
            player1Layout.setBackgroundResource(R.drawable.round_back_dark_blue_20);
            //player2Layout.setBackgroundResource(R.drawable.round_back_dark_blue_stroke);
            player2Layout.setBackgroundResource(R.drawable.main_grad);
            AnimationDrawable animationDrawable = (AnimationDrawable) player2Layout.getBackground();
            animationDrawable.setEnterFadeDuration(2000);
            animationDrawable.setExitFadeDuration(4000);
            animationDrawable.start();
        }
    }

    private void selectBox(ImageView imageView, int selectedBoxPosition, String selectedByPlayer){
        boxesSelectedBy[selectedBoxPosition - 1] = selectedByPlayer;

        if(selectedByPlayer.equals(playerUniqueId)){
            imageView.setImageResource(R.drawable.cross_icon);
            playerTurn = opponentUniqueId;
        }
        else{
            imageView.setImageResource(R.drawable.zero_icon);
            playerTurn = playerUniqueId;
        }
        applyPlayerTurn(playerTurn);
        if(checkPlayerWin(selectedByPlayer)){
            databaseReference.child("won").child(connectionId).child("player_id").setValue(selectedByPlayer);
        }

        if((doneBoxes.size() == 9) && (!checkPlayerWin(selectedByPlayer))){
            final WinDialog winDialog = new WinDialog(MainActivity.this, "Ничья!");
            winDialog.setCancelable(false);
            winDialog.show();
        }
    }

    private boolean checkPlayerWin(String playerId){
        boolean isPlayerWon = false;

        for(int i=0; i < combinationslist.size(); i++){
            final int[] combination = combinationslist.get(i);

            if(boxesSelectedBy[combination[0]].equals(playerId) &&
                    (boxesSelectedBy[combination[1]].equals(playerId) &&
                            (boxesSelectedBy[combination[2]].equals(playerId)))){
                isPlayerWon = true;
            }
        }
        return isPlayerWon;
    }
}
