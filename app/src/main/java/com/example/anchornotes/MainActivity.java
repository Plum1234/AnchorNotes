package com.example.anchornotes;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.anchornotes.context.NotificationHelper;
import com.example.anchornotes.context.RelevantCleanup;
import com.example.anchornotes.ui.HomeFragment;
import com.example.anchornotes.worker.RelevantCleanupWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // must contain a View with id fragment_container

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize notification channel
        new NotificationHelper(this);

        // Cleanup expired relevant notes on startup
        RelevantCleanup.runNow(this);

        // Schedule periodic cleanup worker (runs every 15 minutes)
        // This ensures time-based reminders are automatically retired even when app is closed
        PeriodicWorkRequest cleanupWork = new PeriodicWorkRequest.Builder(
                RelevantCleanupWorker.class,
                15, // Repeat interval
                TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "relevant_cleanup",
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                cleanupWork);

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

    @Override
    protected void onStart() {
        super.onStart();
        // Aggressive cleanup for demo - runs when activity becomes visible
        RelevantCleanup.runNow(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Aggressive cleanup for demo - runs when returning to activity
        RelevantCleanup.runNow(this);
    }
}
