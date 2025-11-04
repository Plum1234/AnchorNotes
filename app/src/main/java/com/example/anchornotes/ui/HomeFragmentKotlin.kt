package com.example.anchornotes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anchornotes.R
import com.example.anchornotes.data.NoteSearchFilter
import com.example.anchornotes.databinding.FragmentHomeBinding
import com.example.anchornotes.viewmodel.NoteViewModel

/**
 * HomeFragment with search and filter functionality.
 * This is the Kotlin version that replaces HomeFragment.java.
 */
class HomeFragmentKotlin : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: NotesAdapter
    private lateinit var viewModel: NoteViewModel
    
    companion object {
        @JvmStatic
        fun newInstance(): HomeFragmentKotlin {
            return HomeFragmentKotlin()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up ViewModel
        viewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]
        
        // Set up adapter
        adapter = NotesAdapter(object : NotesAdapter.OnClick {
            override fun onNote(note: com.example.anchornotes.data.db.NoteEntity) {
                val ft = requireActivity().supportFragmentManager.beginTransaction()
                ft.replace(R.id.fragment_container, NoteEditorFragment.newInstance(note.id))
                ft.addToBackStack(null)
                ft.commit()
            }
        })
        
        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.adapter = adapter
        
        // Set up FAB
        binding.fabNew.setOnClickListener {
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, NoteEditorFragment.newInstance(null))
            ft.addToBackStack(null)
            ft.commit()
        }
        
        // Set up SearchView
        setupSearchView()
        
        // Set up Filter button in menu
        setupMenu()
        
        // Listen for filter results from FilterDialogFragment
        setFragmentResultListener(FilterDialogFragment.RESULT_KEY) { _, bundle ->
            // Use Bundle.getParcelable with type parameter for newer API
            @Suppress("DEPRECATION")
            val filter = bundle.getParcelable<NoteSearchFilter>(FilterDialogFragment.FILTER_EXTRA)
            if (filter != null) {
                viewModel.updateFilters(filter)
            }
        }
        
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { notes ->
            adapter.submit(notes ?: emptyList())
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.updateSearchQuery(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupMenu() {
        // Set menu provider on the activity so it shows in the toolbar
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }
            
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter -> {
                        // Open filter dialog
                        val filterDialog = FilterDialogFragment.newInstance()
                        filterDialog.show(parentFragmentManager, FilterDialogFragment.TAG)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh results when returning
        // Results are already observed via LiveData, so this is optional
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

