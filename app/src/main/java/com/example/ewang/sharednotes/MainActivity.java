package com.example.ewang.sharednotes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public class Note {
        private String noteContents;
        private String creator;
        private long lastUpdate;

        public Note() {}

        public Note(String contents) {
            lastUpdate = System.currentTimeMillis();
            noteContents = contents;
            creator = "Anonymous";
        }

        public Note(String contents, String owner) {
            lastUpdate = System.currentTimeMillis();
            noteContents = contents;
            creator = owner;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public String getNoteBody() {
            return noteContents;
        }

        public String getCreator() {
            return creator;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent i = getIntent();
        final String login_username = i.getStringExtra("username");

        Firebase.setAndroidContext(this);

        final Firebase db = new Firebase("https://glowing-fire-9880.firebaseio.com/sharednotes");

        this.findViewById(R.id.btnLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String noteID = ((EditText) MainActivity.this.findViewById(R.id.txtName)).getText().toString();
                if (noteID.length() == 0) {
                    Toast.makeText(MainActivity.this, "Enter a name!", Toast.LENGTH_SHORT).show();
                }
                // TODO
                db.child("notes").child(noteID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            HashMap<String, ?> data = (HashMap<String, ?>)dataSnapshot.getValue();
                            if (data == null)
                            {
                                Toast.makeText(MainActivity.this, "Note does not exist!", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                ((EditText) MainActivity.this.findViewById(R.id.txtBody)).setText(data.get("noteBody").toString());
                                ((TextView) MainActivity.this.findViewById(R.id.txtNoteInfo)).setText(noteID + " - " + data.get("creator").toString() + " - " + new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date(Long.parseLong(data.get("lastUpdate").toString()))));
                                Toast.makeText(MainActivity.this, "Note loaded!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(MainActivity.this, "Firebase error!", Toast.LENGTH_SHORT).show();
                            Log.e("SharedNotes", "Error reading note", ex);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        Toast.makeText(MainActivity.this, "Firebase error!", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Error loading note", firebaseError.toException());
                    }
                });
            }
        });
        this.findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final String noteID = ((EditText) MainActivity.this.findViewById(R.id.txtName)).getText().toString();
                    if (noteID.length() == 0) {
                        Toast.makeText(MainActivity.this, "Please enter a name for the note!", Toast.LENGTH_SHORT).show();
                    }
                    String noteBody = ((EditText) MainActivity.this.findViewById(R.id.txtBody)).getText().toString();
                    if (noteBody.length() == 0) {
                        db.child("notes").child(noteID).removeValue();
                        Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final Note n = new Note(noteBody, login_username);
                    db.child("notes").child(noteID).setValue(n, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                Toast.makeText(MainActivity.this, "Firebase failed to save note!", Toast.LENGTH_SHORT).show();
                                Log.e("SharedNotes", firebaseError.getMessage());
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                                ((TextView) MainActivity.this.findViewById(R.id.txtNoteInfo)).setText(noteID + " - " + n.getCreator().toString() + " - " + new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date(n.getLastUpdate())));
                            }
                        }
                    });
                }
                catch (Exception ex)
                {
                    Log.e("SharedNotes", "Error when saving note", ex);
                    Toast.makeText(MainActivity.this, "Failed to save note!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
