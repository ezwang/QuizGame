package com.example.ewang.sharednotes;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Firebase.setAndroidContext(this);

        final Firebase db = new Firebase("https://glowing-fire-9880.firebaseio.com/sharednotes");

        setContentView(R.layout.activity_login);

                ((Button) this.findViewById(R.id.btnLogin)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String username = ((EditText)LoginActivity.this.findViewById(R.id.txtUsername)).getText().toString();
                        final String password = ((EditText)LoginActivity.this.findViewById(R.id.txtPassword)).getText().toString();
                        if (username.isEmpty() || password.isEmpty()) {
                            new AlertDialog.Builder(LoginActivity.this).setTitle("Shared Notes").setMessage("Please fill out all the fields!").setPositiveButton("OK", null).show();
                            return;
                        }
                        db.child("users").child(username).child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                try {
                                    String data = (String)dataSnapshot.getValue();
                                    if (data == null)
                                    {
                                        new AlertDialog.Builder(LoginActivity.this).setTitle("Shared Notes").setMessage("No account exists with that username!").setPositiveButton("OK", null).show();
                                    }
                                    else if (data.equals(password)) {
                                        gotoMain(username);
                                    }
                                    else {
                                        new AlertDialog.Builder(LoginActivity.this).setTitle("Shared Notes").setMessage("Wrong password!").setPositiveButton("OK", null).show();
                                    }
                                } catch (Exception ex) {
                                    Toast.makeText(LoginActivity.this, "Firebase error!", Toast.LENGTH_SHORT).show();
                                    Log.e("SharedNotes", "Error reading note", ex);
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Toast.makeText(LoginActivity.this, "Firebase error!", Toast.LENGTH_SHORT).show();
                                Log.e("Firebase", "Error loading note", firebaseError.toException());
                            }
                        });
                    }
                });

        ((Button)this.findViewById(R.id.btnRegister)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = ((EditText)LoginActivity.this.findViewById(R.id.txtUsername)).getText().toString();
                final String password = ((EditText)LoginActivity.this.findViewById(R.id.txtPassword)).getText().toString();
                if (username.isEmpty() || password.isEmpty()) {
                    new AlertDialog.Builder(LoginActivity.this).setTitle("Shared Notes").setMessage("Please fill out all the fields!").setPositiveButton("OK", null).show();
                    return;
                }
                if (username.equals("Anonymous") || username.equals("Guest"))
                {
                    new AlertDialog.Builder(LoginActivity.this).setTitle("Shared Notes").setMessage("The username you selected is reserved. Please pick another username!").setPositiveButton("OK", null).show();
                    return;
                }
                db.child("users").child(username).child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            HashMap<String, ?> data = (HashMap<String, ?>) dataSnapshot.getValue();
                            if (data != null) {
                                new AlertDialog.Builder(LoginActivity.this).setTitle("Shared Notes").setMessage("An account with that username already exists!").setPositiveButton("OK", null).show();
                            }
                            else {
                                db.child("users").child(username).setValue(password);
                                gotoMain(username);
                            }
                        } catch (Exception ex) {
                            Toast.makeText(LoginActivity.this, "Firebase error!", Toast.LENGTH_SHORT).show();
                            Log.e("SharedNotes", "Error reading note", ex);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        Toast.makeText(LoginActivity.this, "Firebase error!", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Error loading note", firebaseError.toException());
                    }
                });
            }
        });

        ((Button)this.findViewById(R.id.btnAnonymous)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoMain("Guest");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    public void gotoMain(String username) {
        Intent main = new Intent(this, QuizActivity.class);
        main.putExtra("username", username);
        startActivity(main);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }
}
