package com.example.anchornotes.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.anchornotes.R
import com.example.anchornotes.data.NoteSearchFilter
import com.example.anchornotes.data.db.TagEntity
import com.example.anchornotes.databinding.DialogFilterBinding
import com.example.anchornotes.viewmodel.NoteViewModel
import java.util.Calendar

/**
 * Dialog fragment for filtering notes.
 * Allows selection of tags, date range, and flags (photo, voice, location).
 */
class FilterDialogFragment : DialogFragment() {
    
    private var _binding: DialogFilterBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: NoteViewModel
    private val selectedTagIds = mutableSetOf<Long>()
    private var fromDate: Long? = null
    private var toDate: Long? = null
    
    companion object {
        const val TAG = "FilterDialogFragment"
        const val RESULT_KEY = "filter_result"
        const val FILTER_EXTRA = "filter"
        
        fun newInstance(): FilterDialogFragment {
            return FilterDialogFragment()
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.filter_notes)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]
        
        // Load tags and populate checkboxes
        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            populateTags(tags)
        }
        
        // Set up date pickers
        binding.btnFromDate.setOnClickListener {
            showDatePicker(true)
        }
        
        binding.btnToDate.setOnClickListener {
            showDatePicker(false)
        }
        
        // Set up Apply button
        binding.btnApply.setOnClickListener {
            applyFilters()
        }
        
        // Set up Clear button
        binding.btnClear.setOnClickListener {
            clearFilters()
        }
        
        // Set up Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    private fun populateTags(tags: List<TagEntity>) {
        binding.layoutTags.removeAllViews()
        
        tags.forEach { tag ->
            val checkBox = CheckBox(requireContext()).apply {
                text = tag.name
                id = View.generateViewId()
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTagIds.add(tag.id)
                    } else {
                        selectedTagIds.remove(tag.id)
                    }
                }
            }
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            
            binding.layoutTags.addView(checkBox, layoutParams)
        }
        
        if (tags.isEmpty()) {
            binding.layoutTags.addView(android.widget.TextView(requireContext()).apply {
                text = getString(R.string.no_tags_available)
                setPadding(16, 16, 16, 16)
            })
        }
    }
    
    private fun showDatePicker(isFromDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val cal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val timestamp = cal.timeInMillis
                
                if (isFromDate) {
                    fromDate = timestamp
                    binding.btnFromDate.text = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        .format(cal.time)
                } else {
                    toDate = timestamp
                    binding.btnToDate.text = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        .format(cal.time)
                }
            },
            year,
            month,
            day
        ).show()
    }
    
    private fun applyFilters() {
        val filter = NoteSearchFilter(
            query = null, // Query is managed by HomeFragment
            tagIds = selectedTagIds.toList(),
            fromDate = fromDate,
            toDate = toDate,
            hasPhoto = if (binding.checkboxHasPhoto.isChecked) true else null,
            hasVoice = if (binding.checkboxHasVoice.isChecked) true else null,
            hasLocation = if (binding.checkboxHasLocation.isChecked) true else null
        )
        
        // Send result back to HomeFragment
        val result = Bundle().apply {
            putParcelable(FILTER_EXTRA, filter)
        }
        // Use parentFragmentManager to send result to parent fragment
        parentFragmentManager.setFragmentResult(RESULT_KEY, result)
        
        dismiss()
    }
    
    private fun clearFilters() {
        selectedTagIds.clear()
        fromDate = null
        toDate = null
        binding.btnFromDate.text = getString(R.string.select_from_date)
        binding.btnToDate.text = getString(R.string.select_to_date)
        binding.checkboxHasPhoto.isChecked = false
        binding.checkboxHasVoice.isChecked = false
        binding.checkboxHasLocation.isChecked = false
        
        // Clear tag checkboxes
        for (i in 0 until binding.layoutTags.childCount) {
            val view = binding.layoutTags.getChildAt(i)
            if (view is CheckBox) {
                view.isChecked = false
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

