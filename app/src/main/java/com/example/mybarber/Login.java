package com.example.mybarber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;

    EditText emailInput, passwordInput;
    Button loginbtn;
    TextView signuptext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.loginemail);
        passwordInput = findViewById(R.id.loginpassword);
        loginbtn = findViewById(R.id.loginbtn);
        signuptext = findViewById(R.id.Signuptext);
        signuptext.setOnClickListener(this);

        loginbtn.setOnClickListener(this);



    }

    @Override
    public void onClick(View view) {
        if (view == loginbtn) {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(Login.this, "Authentication Succeed .",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), bottomnav.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(Login.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        if(view == signuptext){
            Intent intent = new Intent(getApplicationContext(), SignUp.class);
            startActivity(intent);
            finish();
        }
    }

}