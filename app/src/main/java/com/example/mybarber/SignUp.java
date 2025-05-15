package com.example.mybarber;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mybarber.home.HomeFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;


public class SignUp extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
EditText displayName,Email,Password,ConfirmPassword;
RadioGroup UserTypeGroup;
RadioButton BarberButton,CustomerButton;
Boolean IsBarber;
FirebaseFirestore db = FirebaseFirestore.getInstance();
Button SignUp;
TextView loginTextView;
private FirebaseAuth mAuth;



    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (BarberButton.isChecked())
            IsBarber=true;
        if (CustomerButton.isChecked())
            IsBarber=false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_up);

       loginTextView = findViewById(R.id.logintext);
       loginTextView.setOnClickListener(this);
       displayName=findViewById(R.id.displayname);
       Email=findViewById(R.id.email);
       Password=findViewById(R.id.password);
       ConfirmPassword=findViewById(R.id.confirmpassword);
       UserTypeGroup=findViewById(R.id.userTypeGroup);
       BarberButton=findViewById(R.id.barberButton);
       CustomerButton=findViewById(R.id.customerButton);
       BarberButton.setOnCheckedChangeListener(this);
       CustomerButton.setOnCheckedChangeListener(this);
       SignUp=findViewById(R.id.signupbtn);
       SignUp.setOnClickListener(this);
       mAuth = FirebaseAuth.getInstance();



    }

    @Override
    public void onClick(View view) {
        if (view==SignUp){
            String username=displayName.getText().toString();
            String email=Email.getText().toString();
            String password=Password.getText().toString();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if(user!=null){
                                    try {
                                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        db.collection("users").document(uid)
                                                .update("IsBarber", IsBarber);
                                    }
                                    catch (Error e) {
                                        System.out.println(e);
                                    }
                                    UserProfileChangeRequest profileupdate = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                                    user.updateProfile(profileupdate).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(SignUp.this, "Authentication Succeed.",
                                                        Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(getApplicationContext(), bottomnav.class);
                                                startActivity(intent);
                                                finish();

                                            }
                                        }
                                    });
                                }

                            } else {
                                Toast.makeText(SignUp.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


        }

        if (view == loginTextView) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
    }

}





