package com.example.ewang.sharednotes;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class QuizActivity extends AppCompatActivity {

    static String username;
    static boolean is_guest;

    static GameState state = GameState.MENU;

    final DecimalFormat timeFormat = new DecimalFormat("00");

    Timer gameTimer;
    int gameTime;

    int gameScore = 0;
    int maxScore = 0;

    private Handler handler = new Handler();

    final int[] btns = new int[] { R.id.button, R.id.button2, R.id.button3, R.id.button4 };

    enum GameState {
        PLAYING,
        MENU,
        ANIMATION
    }

    Drawable originalButtonTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        username = getIntent().getStringExtra("username");
        is_guest = username.equals("Anonymous") || username.equals("Guest");

        Firebase.setAndroidContext(this);

        final Firebase db = new Firebase("https://glowing-fire-9880.firebaseio.com/sharednotes");

        final Activity m = this;

        originalButtonTexture = this.findViewById(R.id.button).getBackground();

        for (int i = 0; i < btns.length; i++)
        {
            Button b = (Button)this.findViewById(btns[i]);
            b.setText("Play!");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (state == GameState.MENU) {
                        // load game
                        gameScore = 0;
                        loadQuestion();
                        state = GameState.PLAYING;
                        gameTimer = new Timer();
                        gameTime = 60;
                        gameTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        TextView t = (TextView) m.findViewById(R.id.txtTime);
                                        t.setText(timeFormat.format(gameTime / 60) + ":" + timeFormat.format(gameTime % 60));
                                    }
                                });
                                if (gameTime <= 0) {
                                    gameTimer.cancel();
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (gameScore > maxScore) {
                                                maxScore = gameScore;
                                                updateScore();
                                                if (!is_guest) {
                                                    db.child("users").child(username).child("highscore").setValue(maxScore);
                                                }
                                            }
                                            resetMenu();
                                        }
                                    });
                                    state = GameState.MENU;
                                    return;
                                }
                                gameTime--;
                            }
                        }, 0, 1000);
                    } else if (state == GameState.PLAYING) {
                        state = GameState.ANIMATION;
                        // player pressed answer
                        TextView qTxt = (TextView)m.findViewById(R.id.txtQuestion);
                        if (v.getId() == answerID) {
                            qTxt.setText("Correct!");
                            gameScore += 1;
                            updateScore();
                            v.setBackgroundColor(Color.GREEN);
                        } else {
                            qTxt.setText("Wrong!");
                            v.setBackgroundColor(Color.RED);
                            Button correct = (Button)m.findViewById(answerID);
                            correct.setBackgroundColor(Color.GREEN);
                        }
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setButtonColor(-1);
                                        loadQuestion();
                                    }
                                });
                               state = GameState.PLAYING;
                            }
                        }, 1000);
                    }
                }
            });
        }

        if (!is_guest) {
            db.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        HashMap<String, ?> data = (HashMap<String, ?>) dataSnapshot.getValue();
                        Object score = data.get("highscore");
                        if (score != null) {
                            maxScore = (int)((long)score);
                        }
                        updateScore();
                    } catch (Exception ex) {
                        Log.e("SharedNotes", "Error loading score", ex);
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.e("Firebase", "Error loading score", firebaseError.toException());
                }
            });
        }
    }

    void resetMenu() {
        TextView qTxt = (TextView)this.findViewById(R.id.txtQuestion);
        qTxt.setText("Game Over! Press any button to play again!");
        for (int i = 0; i < btns.length; i++) {
            Button b = (Button)this.findViewById(btns[i]);
            b.setText("Play!");
        }
    }

    void updateScore() {
        TextView t = (TextView)this.findViewById(R.id.txtScore);
        t.setText("Points: " + gameScore + " - High Score: " + maxScore);
    }

    int answerID = -1;

    void setButtonColor(int color) {
        for (int i = 0; i < btns.length; i++) {
            Button b = (Button)this.findViewById(btns[i]);
            if (color == -1) {
                b.setBackground(originalButtonTexture);
            }
            else {
                b.setBackgroundColor(color);
            }
        }
    }

    void loadQuestion() {
        TextView qTxt = (TextView)this.findViewById(R.id.txtQuestion);
        qTxt.setText("This is a sample question!");
        for (int i = 0; i < btns.length; i++) {
            Button b = (Button)this.findViewById(btns[i]);
            b.setText("Answer");
        }
        answerID = btns[(int)(Math.random() * btns.length)];
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_quiz, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
