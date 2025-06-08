package com.example.mybarber;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mybarber.home.HomeFragment;
import com.example.mybarber.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class bottomnav extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isBarber = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottomnav);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Check user type and setup navigation
        checkUserTypeAndSetupNavigation();

        // Load default fragment
        loadfragment(new HomeFragment());
    }

    private void checkUserTypeAndSetupNavigation() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Boolean userIsBarber = document.getBoolean("isBarber");
                                isBarber = userIsBarber != null ? userIsBarber : false;
                                setupNavigationMenu();
                            }
                        }
                    });
        }
    }

    private void setupNavigationMenu() {
        if (isBarber) {
            bottomNavigationView.getMenu().findItem(R.id.showCalender).setVisible(true);
        } else {
            bottomNavigationView.getMenu().findItem(R.id.showCalender).setVisible(false);
        }
    }

    private void loadfragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        if (item.getItemId() == R.id.homenav) {
            fragment = new HomeFragment();
        } else if (item.getItemId() == R.id.profilenav) {
            fragment = new ProfileFragment();
        } else if (item.getItemId() == R.id.showCalender) {
            // Navigate to Calendar Activity instead of fragment
            if (isBarber) {
                Intent intent = new Intent(this, BarberCalendarActivity.class);
                startActivity(intent);
                return true;
            }
        }

        if (fragment != null) {
            loadfragment(fragment);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset to home fragment when returning from calendar activity
        if (bottomNavigationView.getSelectedItemId() == R.id.showCalender) {
            bottomNavigationView.setSelectedItemId(R.id.homenav);
        }
    }
}