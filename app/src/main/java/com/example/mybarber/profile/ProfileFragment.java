package com.example.mybarber.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.mybarber.R;
import com.example.mybarber.SignUp;
import com.example.mybarber.bottomnav;
import com.example.mybarber.intent;
import com.example.mybarber.settings.SettingsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;




public class ProfileFragment extends Fragment implements View.OnClickListener {

    TextView username;
    ImageButton settings;
    private FirebaseAuth mAuth;

    public ProfileFragment() {

    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        username = view.findViewById(R.id.username);
        TextView emailText = view.findViewById(R.id.email);
        TextView roleText = view.findViewById(R.id.role);
        settings = view.findViewById(R.id.settings);

        settings.setOnClickListener(this);

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String displayName = documentSnapshot.getString("displayName");
                            String email = documentSnapshot.getString("email");
                            Boolean isBarber = documentSnapshot.getBoolean("IsBarber");

                            if (displayName != null) username.setText(displayName);
                            if (email != null) emailText.setText(email);
                            roleText.setText(isBarber != null && isBarber ? "Barber" : "Customer");
                        }
                    })
                    .addOnFailureListener(e -> {
                        emailText.setText("Failed to load email");
                        roleText.setText("Failed to load role");
                    });
        }

        return view;
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.settings) {
            SettingsFragment settingsFragment = new SettingsFragment();

            // Replace ProfileFragment with SettingsFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment)
                    .addToBackStack(null)  // So user can go back
                    .commit();
        }
    }

}
