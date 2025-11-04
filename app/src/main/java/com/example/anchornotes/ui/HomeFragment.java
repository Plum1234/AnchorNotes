package com.example.anchornotes.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.anchornotes.R;
import com.example.anchornotes.data.NoteSearchFilter;
import com.example.anchornotes.data.ServiceLocator;
import com.example.anchornotes.data.db.NoteEntity;
import com.example.anchornotes.data.repo.NoteSearchRepository;
import com.example.anchornotes.databinding.FragmentHomeBinding;
import com.example.anchornotes.viewmodel.NoteListViewModel;

import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private NotesAdapter adapter;

    // --- NEW: search/filter state ---
    private String currentQuery = null;
    private NoteSearchFilter currentFilter = new NoteSearchFilter();

    public static HomeFragment newInstance() { return new HomeFragment(); }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // needed so onCreateOptionsMenu gets called to show the Filter action
        setHasOptionsMenu(true);

        // Listen for filter dialog result (Fragment Result API)
        getParentFragmentManager().setFragmentResultListener(
                FilterDialogFragment.RESULT_KEY,
                this,
                (requestKey, bundle) -> {
                    NoteSearchFilter f = bundle.getParcelable(FilterDialogFragment.FILTER_EXTRA);
                    if (f != null) {
                        // preserve the current query typed in the SearchView
                        f.query = currentQuery;
                        currentFilter = f;
                        runSearch();
                    }
                }
        );
    }

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
        // Adapter (unchanged)
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

        // --- NEW: SearchView behavior ---
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentQuery = (newText == null || newText.trim().isEmpty()) ? null : newText;
                currentFilter.query = currentQuery;
                runSearch();
                return true;
            }
        });

        // Initial load (kept): show all notes before any search/filter is applied
        NoteListViewModel vm = new ViewModelProvider(this).get(NoteListViewModel.class);
        adapter.submit(vm.loadNotes());
    }

    // --- NEW: inflate the Filter action in the toolbar ---
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.home_menu, menu);
    }

    // --- NEW: open the filter dialog when the menu item is tapped ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            new FilterDialogFragment().show(getParentFragmentManager(), FilterDialogFragment.TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        // If there is an active search/filter, keep it; else refresh the full list
        if (hasActiveFilter(currentFilter)) {
            runSearch();
        } else {
            NoteListViewModel vm = new ViewModelProvider(this).get(NoteListViewModel.class);
            adapter.submit(vm.loadNotes());
        }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }

    // --- NEW: runSearch helper that calls the Java NoteSearchRepository or falls back to "all" ---
    private void runSearch() {
        if (!hasActiveFilter(currentFilter)) {
            // nothing active => show all
            NoteListViewModel vm = new ViewModelProvider(this).get(NoteListViewModel.class);
            adapter.submit(vm.loadNotes());
            return;
        }

        NoteSearchRepository repo = ServiceLocator.noteSearchRepository(requireContext());
        List<NoteEntity> results = repo.search(
                currentFilter.query,
                currentFilter.tagIds,
                currentFilter.fromDate,
                currentFilter.toDate,
                currentFilter.hasPhoto,
                currentFilter.hasVoice,
                currentFilter.hasLocation
        );
        adapter.submit(results);
    }

    private boolean hasActiveFilter(NoteSearchFilter f) {
        return (f != null) && (
                (f.query != null && !f.query.trim().isEmpty()) ||
                        (f.tagIds != null && !f.tagIds.isEmpty()) ||
                        f.fromDate != null ||
                        f.toDate != null ||
                        f.hasPhoto != null ||
                        f.hasVoice != null ||
                        f.hasLocation != null
        );
    }
}
