package com.example.mybarber;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.mybarber.home.HomeFragment;
import com.example.mybarber.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class bottomnav extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottomnav);
        bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        loadfragment(new HomeFragment());

    }
    private void loadfragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        if(item.getItemId()==R.id.homenav){
            fragment = new HomeFragment();
        }
        if(item.getItemId()==R.id.profilenav){
            fragment = new ProfileFragment();
        }
        if(fragment!=null){
            loadfragment(fragment);
        }
        return true;
    }
}