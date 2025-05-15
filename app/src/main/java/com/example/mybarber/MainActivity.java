package com.example.mybarber;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button login,signup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        login = findViewById(R.id.loginbtn);
        signup = findViewById(R.id.signupbtn);
        login.setOnClickListener(this);
        signup.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v==login){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
        if (v==signup){
            Intent intent = new Intent(getApplicationContext(), SignUp.class);
            startActivity(intent);
            finish();
        }
    }
}