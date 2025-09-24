package com.example.lexify.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lexify.R
import com.example.lexify.adapter.ProfitAdapter
import com.example.lexify.data.AppDatabase
import com.example.lexify.data.ProfitDao
import com.example.lexify.databinding.FragmentAddTaskBinding
import com.example.lexify.model.DailyProfit
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTaskFragment : Fragment() {
    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!
    private val args: AddTaskFragmentArgs by navArgs()
    
    private val viewModel: TaskViewModel by viewModels {
        val taskDao = AppDatabase.getDatabase(requireContext()).taskDao()
        TaskViewModelFactory(taskDao)
    }
    
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var profitAdapter: ProfitAdapter
    private var selectedDate: Date = Date()
    private lateinit var profitDao: ProfitDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get the selected date from arguments
        selectedDate = Date(args.selectedDate)
        profitDao = AppDatabase.getDatabase(requireContext()).profitDao()
        
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeTasks()
        observeProfits()
        
        // Load tasks and profits for the selected date
        viewModel.loadTasksForDate(selectedDate)
        loadProfitsForDate(selectedDate)
    }
    
    private fun setupUI() {
        // Format and display the selected date
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.selectedDateText.text = dateFormat.format(selectedDate)
        
        // Set up the add profit button
        binding.addProfitButton.apply {
            setOnClickListener { showAddProfitDialog() }
            setTextColor(resources.getColor(R.color.yellow_500, null))
            strokeColor = resources.getColorStateList(R.color.yellow_500, null)
            iconTint = resources.getColorStateList(R.color.yellow_500, null)
        }
        
        // Check if the selected date is in the past
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val isPastDate = selectedDate.before(today)
        
        // If the date is in the past, disable the input fields and button
        if (isPastDate) {
            binding.titleInputLayout.isEnabled = false
            binding.descriptionInputLayout.isEnabled = false
            binding.saveButton.visibility = View.GONE
            
            // Show a message that this is a past date
            binding.dateInfoText.visibility = View.VISIBLE
            binding.dateInfoText.text = getString(R.string.past_date_message)
        } else {
            binding.titleInputLayout.isEnabled = true
            binding.descriptionInputLayout.isEnabled = true
            binding.saveButton.visibility = View.VISIBLE
            binding.dateInfoText.visibility = View.GONE
        }
    }
    
    private fun setupRecyclerView() {
        // Set up profits RecyclerView
        profitAdapter = ProfitAdapter { profit ->
            viewLifecycleOwner.lifecycleScope.launch {
                profitDao.delete(profit)
            }
        }
        
        binding.profitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = profitAdapter
            setHasFixedSize(true)
        }
        
        // Set up tasks RecyclerView
        val isPastDate = selectedDate.before(getTodayStart())
        
        taskAdapter = TaskAdapter(
            onTaskChecked = { task, isChecked ->
                // Only allow toggling completion for today and future dates
                if (!isPastDate) {
                    viewModel.toggleTaskCompletion(task, isChecked)
                }
            },
            onTaskDeleted = { task ->
                // Only allow deletion for today and future dates
                if (!isPastDate) {
                    viewModel.deleteTask(task) { success ->
                        if (success) {
                            showMessage("Задача удалена")
                        }
                    }
                } else {
                    showMessage("Невозможно изменить прошедшие задачи")
                }
            },
            isReadOnly = isPastDate
        )
        
        with(binding.tasksRecyclerView) {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                // Optional: Add item decoration for spacing between items if needed
                // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            adapter = taskAdapter
            setHasFixedSize(true)
            // Make sure the RecyclerView is visible
            visibility = View.VISIBLE
        }
        
        // Add a text view to show when there are no tasks
        binding.emptyStateText.visibility = View.GONE
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is TaskUiState.Success -> {
                        taskAdapter.submitList(state.tasks)
                        if (state.tasks.isEmpty()) {
                            binding.emptyStateText.visibility = View.VISIBLE
                            binding.tasksRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyStateText.visibility = View.GONE
                            binding.tasksRecyclerView.visibility = View.VISIBLE
                        }
                    }
                    is TaskUiState.Loading -> {
                        // Show loading state if needed
                    }
                    is TaskUiState.Error -> {
                        // Show error state if needed
                        showError(state.message)
                    }
                    is TaskUiState.Empty -> {
                        taskAdapter.submitList(emptyList())
                        binding.emptyStateText.visibility = View.VISIBLE
                        binding.tasksRecyclerView.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeProfits() {
        profitDao.getProfitsForDate(selectedDate).observe(viewLifecycleOwner) { profits ->
            profitAdapter.submitList(profits)

            // Show/hide profits section
            if (profits.isNotEmpty()) {
                binding.profitsLabel.visibility = View.VISIBLE
                binding.profitsRecyclerView.visibility = View.VISIBLE
            } else {
                binding.profitsLabel.visibility = View.GONE
                binding.profitsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun loadProfitsForDate(date: Date) {
        viewLifecycleOwner.lifecycleScope.launch {
            val profits = profitDao.getProfitsForDate(date).value ?: emptyList()
            profitAdapter.submitList(profits)
        }
    }

    private fun showAddProfitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_profit, null)
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
        val noteInput = dialogView.findViewById<TextInputEditText>(R.id.noteInput)

        dialogView.findViewById<View>(R.id.addButton).setOnClickListener {
            val amountText = amountInput.text.toString()
            val note = noteInput.text.toString().trim()

            if (amountText.isNotEmpty()) {
                try {
                    val amount = amountText.toDouble()
                    if (amount > 0) {
                        val profit = DailyProfit(
                            amount = amount,
                            date = selectedDate,
                            note = note
                        )

                        viewLifecycleOwner.lifecycleScope.launch {
                            profitDao.insert(profit)
                            dialog.dismiss()
                        }
                    } else {
                        amountInput.error = getString(R.string.error_invalid_amount)
                    }
                } catch (e: NumberFormatException) {
                    amountInput.error = getString(R.string.error_invalid_number)
                }
            } else {
                amountInput.error = getString(R.string.error_amount_required)
            }
        }

        dialogView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun getTodayStart(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val title = binding.titleEditText.text.toString().trim()
            val description = binding.descriptionEditText.text.toString().trim()

            if (title.isNotEmpty()) {
                viewModel.addTask(title, description, selectedDate) { success ->
                    if (success) {
                        binding.titleEditText.text?.clear()
                        binding.descriptionEditText.text?.clear()
                    }
                }
            } else {
                binding.titleInputLayout.error = getString(R.string.error_amount_required)
            }
            binding.emptyStateText.visibility = View.GONE
        }
    }
    
    
    private fun clearInputs() {
        binding.titleEditText.text?.clear()
        binding.descriptionEditText.text?.clear()
        binding.titleInputLayout.error = null
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
