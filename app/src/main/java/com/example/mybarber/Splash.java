package com.example.mybarber;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Splash extends AppCompatActivity {

    FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        auth = FirebaseAuth.getInstance();

        Thread t=new Thread() {
            public void run() {

                try {

                    sleep(3500);

                    FirebaseUser currentUser = auth.getCurrentUser();
/*
                    if(currentUser != null){
                        Intent intent = new Intent(getApplicationContext(), bottomnav.class);
                        startActivity(intent);
                        finish();
                    }

                    else {

 */
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };


        t.start();

    };
}