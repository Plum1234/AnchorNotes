package com.example.anchornotes;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.anchornotes.context.NotificationHelper;
import com.example.anchornotes.context.RelevantCleanup;
import com.example.anchornotes.ui.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // must contain a View with id fragment_container

        // Initialize notification channel
        new NotificationHelper(this);

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, HomeFragment.newInstance());
            ft.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cleanup expired relevant notes on resume
        RelevantCleanup.runNow(this);
    }
}
