package com.example.ewang.sharednotes;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
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

    private String loadLocalJSON(int id) throws UnsupportedEncodingException, IOException {
        InputStream is = getResources().openRawResource(id);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            is.close();
        }

        return writer.toString();
    }

    class Question {
        String question;
        int answer;
        String[] choices;

        public Question(String q, String a, String b, String c, String d, int ans) {
            question = q;
            choices = new String[] { a, b, c, d };
            answer = ans;
        }
    }

    ArrayList<Question> questionList = new ArrayList<Question>();

    Firebase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        username = getIntent().getStringExtra("username");
        is_guest = username.equals("Anonymous") || username.equals("Guest");

        Firebase.setAndroidContext(this);

        db = new Firebase("https://glowing-fire-9880.firebaseio.com/sharednotes");

        final Activity m = this;

        try {
            JSONArray json = new JSONArray(loadLocalJSON(R.raw.questions));
            for (int i = 0; i < json.length(); i++) {
                JSONObject qObj = json.getJSONObject(i);
                String question = qObj.getString("question");
                String answer = qObj.getString("answer");
                int ansindex = -1;
                if (answer.equals("A"))
                    ansindex = 0;
                if (answer.equals("B"))
                    ansindex = 1;
                if (answer.equals("C"))
                    ansindex = 2;
                if (answer.equals("D"))
                    ansindex = 3;
                if (question == null || question.isEmpty()) {
                    Log.w("QuizGame", "Question is null or empty! (#" + (i + 1) + ")");
                }
                else if (ansindex == -1) {
                    Log.w("QuizGame", "Could not find correct answer choice: " + answer);
                }
                else {
                    questionList.add(new Question(question, qObj.getString("A"), qObj.getString("B"), qObj.getString("C"), qObj.getString("D"), ansindex));
                }
            }
        }
        catch (Exception ex) {
            Log.e("QuizGame", "Error loading JSON", ex);
        }

        Collections.shuffle(questionList);

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
                        updateScore();
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
                                        if (gameTime <= 10) {
                                            t.setTextColor(Color.RED);
                                        }
                                    }
                                });
                                if (gameTime <= 0) {
                                    gameTimer.cancel();
                                    state = GameState.ANIMATION;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            state = GameState.ANIMATION;
                                            ((TextView) m.findViewById(R.id.txtTime)).setTextColor(Color.BLACK);
                                            setButtonColor(Color.BLUE);
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
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setButtonColor(-1);
                                                    state = GameState.MENU;
                                                }
                                            });
                                        }
                                    }, 3000);
                                    return;
                                }
                                gameTime--;
                            }
                        }, 0, 1000);
                    } else if (state == GameState.PLAYING) {
                        state = GameState.ANIMATION;
                        // player pressed answer
                        if (answerID == -1) {
                            Log.w("QuizGame", "User pressed button before answer loaded!");
                            return;
                        }
                        TextView qTxt = (TextView)m.findViewById(R.id.txtQuestion);
                        boolean right = false;
                        if (v.getId() == answerID) {
                            qTxt.setText("Correct!");
                            gameScore += 1;
                            updateScore();
                            v.setBackgroundColor(Color.GREEN);
                            right = true;
                        } else {
                            qTxt.setText("Wrong!");
                            v.setBackgroundColor(Color.RED);
                            Button correct = (Button)m.findViewById(answerID);
                            correct.setBackgroundColor(Color.GREEN);
                        }
                        answerID = -1;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (gameTime <= 0) {
                                    return;
                                }
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setButtonColor(-1);
                                        loadQuestion();
                                    }
                                });
                               state = GameState.PLAYING;
                            }
                        }, (right ? 500 : 1000));
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
        Question ques = questionList.remove(0);
        TextView qTxt = (TextView)this.findViewById(R.id.txtQuestion);
        qTxt.setText(ques.question);
        for (int i = 0; i < btns.length; i++) {
            Button b = (Button)this.findViewById(btns[i]);
            b.setText(ques.choices[i]);
        }
        answerID = btns[ques.answer];
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

        if (id == R.id.reset_settings) {
            maxScore = 0;
            updateScore();
            if (!is_guest) {
                db.child("users").child(username).child("highscore").setValue(maxScore);
            }
            Toast.makeText(QuizActivity.this, "High score reset!", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.about_settings) {
            new AlertDialog.Builder(QuizActivity.this).setTitle("About Quiz Game").setMessage("This app was created by Eric Wang.").setPositiveButton("OK", null).setIcon(R.drawable.icon).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
