package com.example.lexify.ui.task

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lexify.R
import com.example.lexify.adapter.ExpenseAdapter
import com.example.lexify.adapter.ProfitAdapter
import com.example.lexify.data.AppDatabase
import com.example.lexify.data.ProfitDao
import com.example.lexify.data.TaskDao
import com.example.lexify.databinding.DialogAddProfitBinding
import com.example.lexify.databinding.FragmentAddTaskBinding
import com.example.lexify.model.Expense
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTaskFragment : Fragment() {
    companion object {
        private const val TAG = "AddTaskFragment"
    }
    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!
    private val args: AddTaskFragmentArgs by navArgs()

    private val viewModel: TaskViewModel by viewModels {
        val database = AppDatabase.Companion.getDatabase(requireContext())
        TaskViewModelFactory(
            database.taskDao(),
            database.expenseDao(),
            database.profitDao()
        )
    }

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var profitAdapter: ProfitAdapter
    private lateinit var expenseAdapter: ExpenseAdapter
    private var selectedDate: Date = Date()
    private lateinit var profitDao: ProfitDao
    private lateinit var taskDao: TaskDao
    // expenseDao is now obtained through the ViewModel

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
        profitDao = AppDatabase.Companion.getDatabase(requireContext()).profitDao()
        taskDao = AppDatabase.Companion.getDatabase(requireContext()).taskDao()

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Set up observers
        observeTasks()
        observeProfits()
        observeExpenses()

        // Load data for the selected date
        viewModel.loadTasksForDate(selectedDate)
        loadProfitsForDate(selectedDate)
        loadExpensesForDate(selectedDate)
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

        // Set up the add expense button
        binding.addExpenseButton.apply {
            setOnClickListener { showAddExpenseDialog() }
            setTextColor(resources.getColor(R.color.red_500, null))
            strokeColor = resources.getColorStateList(R.color.red_500, null)
            iconTint = resources.getColorStateList(R.color.red_500, null)
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
            viewModel.deleteProfit(profit) { success ->
                if (success) {
                    showMessage("Доход удален")
                    loadProfitsForDate(selectedDate) // Reload profits after deletion
                } else {
                    showError("Не удалось удалить доход")
                }
            }
        }

        binding.profitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = profitAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            visibility = View.VISIBLE
        }

        // Set up expenses RecyclerView
        expenseAdapter = ExpenseAdapter { expense ->
            viewModel.deleteExpense(expense) { success ->
                if (success) {
                    showMessage("Расход удален")
                    loadExpensesForDate(selectedDate) // Reload expenses after deletion
                } else {
                    showError("Не удалось удалить расход")
                }
            }
        }

        binding.expensesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            visibility = View.VISIBLE
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profits.collect { profits ->
                Log.d(TAG, "profits observed: count=${profits.size}")
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
    }

    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenses.collect { expenses ->
                Log.d(TAG, "expenses observed: count=${expenses.size}")
                expenseAdapter.submitList(expenses)
                updateExpenseTotal(expenses)

                // Show/hide expenses section
                if (expenses.isNotEmpty()) {
                    binding.expensesLabel.visibility = View.VISIBLE
                    binding.expensesRecyclerView.visibility = View.VISIBLE
                } else {
                    binding.expensesLabel.visibility = View.GONE
                    binding.expensesRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun updateExpenseTotal(expenses: List<Expense>) {
        val total = expenses.sumOf { it.amount.toDouble() }
        binding.expenseTotalText.text = String.format("%.0f ₽", total)
        binding.expenseTotalText.visibility = if (expenses.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadExpensesForDate(date: Date) {
        viewModel.loadExpensesForDate(date)
    }

    private fun loadProfitsForDate(date: Date) {
        viewModel.loadProfitsForDate(date)
    }

    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
        val noteInput = dialogView.findViewById<TextInputEditText>(R.id.noteInput)
        val categoryInput = dialogView.findViewById<AutoCompleteTextView>(R.id.categoryInput)

        // Set up categories for the dropdown
        val categories = arrayOf("Еда", "Транспорт", "Покупки", "Развлечения", "Другое")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        categoryInput.setAdapter(adapter)

        dialogView.findViewById<View>(R.id.addButton).setOnClickListener {
            val amountText = amountInput.text.toString()
            val note = noteInput.text.toString().trim()
            val category = categoryInput.text.toString().trim()

            if (amountText.isNotEmpty() && category.isNotEmpty()) {
                try {
                    val amount = amountText.toDouble()
                    if (amount > 0) {
                        val expense = Expense(
                            amount = amount,
                            date = selectedDate,
                            note = if (note.isNotEmpty()) "$category: $note" else category,
                            category = category
                        )

                        viewModel.addExpense(
                            amount = amount,
                            date = selectedDate,
                            note = if (note.isNotEmpty()) "$category: $note" else category,
                            category = category
                        ) { success ->
                            if (!success) {
                                showError(getString(R.string.error_adding_expense))
                            }
                        }
                        dialog.dismiss()
                    } else {
                        amountInput.error = getString(R.string.error_invalid_amount)
                    }
                } catch (e: NumberFormatException) {
                    amountInput.error = getString(R.string.error_invalid_number)
                }
            } else {
                if (amountText.isEmpty()) {
                    dialogView.findViewById<TextInputLayout>(R.id.amountInputLayout).error = getString(R.string.error_amount_required)
                }
                if (category.isEmpty()) {
                    dialogView.findViewById<TextInputLayout>(R.id.categoryInputLayout).error = getString(R.string.error_category_required)
                }
            }
        }

        // Clear errors when user starts typing
        amountInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialogView.findViewById<TextInputLayout>(R.id.amountInputLayout).error = null
            }
        }

        categoryInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialogView.findViewById<TextInputLayout>(R.id.categoryInputLayout).error = null
            }
        }

        dialogView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun showAddProfitDialog() {
        val binding = DialogAddProfitBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

        binding.addButton.setOnClickListener {
            val amountText = binding.amountInput.text.toString()
            val note = binding.noteInput.text.toString().trim()

            // Clear previous errors
            binding.amountLayout.error = null

            if (amountText.isBlank()) {
                binding.amountLayout.error = getString(R.string.error_amount_required)
                return@setOnClickListener
            }

            try {
                val amount = amountText.toDouble()
                if (amount > 0) {
                    viewModel.addProfit(
                        amount = amount,
                        date = selectedDate,
                        note = if (note.isNotBlank()) note else null,
                        onComplete = { success ->
                            if (success) {
                                dialog.dismiss()
                                loadProfitsForDate(selectedDate)
                            } else {
                                showError(getString(R.string.error_adding_profit))
                            }
                        }
                    )
                } else {
                    binding.amountLayout.error = getString(R.string.error_invalid_amount)
                }
            } catch (e: NumberFormatException) {
                binding.amountLayout.error = getString(R.string.error_invalid_number)
            }
        }

        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(binding.root)
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


    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}