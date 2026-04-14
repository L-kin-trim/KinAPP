package com.example.kin.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.MainActivity;
import com.example.kin.data.KinRepository;

public class LaunchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KinRepository repository = new KinRepository(this);
        Class<?> target = repository.getSessionManager().isLoggedIn() ? MainActivity.class : AuthActivity.class;
        startActivity(new Intent(this, target));
        finish();
    }
}
