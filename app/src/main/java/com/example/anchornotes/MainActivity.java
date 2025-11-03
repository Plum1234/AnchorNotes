package com.example.anchornotes;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.anchornotes.databinding.ActivityMainBinding;
import com.example.anchornotes.ui.HomeFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment.newInstance())
                    .commit();
        }
    }
}
