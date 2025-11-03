package com.example.anchornotes.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.anchornotes.R;
import com.example.anchornotes.databinding.FragmentHomeBinding;
import com.example.anchornotes.viewmodel.NoteListViewModel;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private NotesAdapter adapter;

    public static HomeFragment newInstance() { return new HomeFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new NotesAdapter(note -> {
            FragmentTransaction ft = requireActivity()
                    .getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, NoteEditorFragment.newInstance(note.id));
            ft.addToBackStack(null);
            ft.commit();
        });

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotes.setAdapter(adapter);

        binding.fabNew.setOnClickListener(v -> {
            FragmentTransaction ft = requireActivity()
                    .getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, NoteEditorFragment.newInstance(null));
            ft.addToBackStack(null);
            ft.commit();
        });

        // Initial load
        NoteListViewModel vm = new ViewModelProvider(this).get(NoteListViewModel.class);
        adapter.submit(vm.loadNotes());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh list when returning from editor (sorted by last edited)
        NoteListViewModel vm = new ViewModelProvider(this).get(NoteListViewModel.class);
        adapter.submit(vm.loadNotes());
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
