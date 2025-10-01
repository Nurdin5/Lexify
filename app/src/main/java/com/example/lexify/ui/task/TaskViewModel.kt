package com.example.lexify.ui.task

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lexify.data.ExpenseDao
import com.example.lexify.data.ProfitDao
import com.example.lexify.data.TaskDao
import com.example.lexify.model.DailyProfit
import com.example.lexify.model.Expense
import com.example.lexify.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch
import java.util.*

class TaskViewModel(
    private val taskDao: TaskDao,
    private val expenseDao: ExpenseDao,
    private val profitDao: ProfitDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()
    
    private val _expenseTotal = MutableStateFlow(0.0)
    val expenseTotal: StateFlow<Double> = _expenseTotal.asStateFlow()
    
    private val _profits = MutableStateFlow<List<DailyProfit>>(emptyList())
    val profits: StateFlow<List<DailyProfit>> = _profits.asStateFlow()
    
    fun loadTasksForDate(selectedDate: Date) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance().apply {
                    time = selectedDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis - 1
                
                taskDao.getTasksForDate(startOfDay, endOfDay).collect { tasks ->
                    _uiState.value = if (tasks.isEmpty()) {
                        TaskUiState.Empty
                    } else {
                        TaskUiState.Success(tasks)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun addTask(title: String, description: String, date: Date, onComplete: (Boolean) -> Unit) {
        if (title.isBlank()) {
            _uiState.value = TaskUiState.Error("Название задачи не может быть пустым")
            onComplete(false)
            return
        }
        
        viewModelScope.launch {
            try {
                val task = Task(
                    title = title,
                    description = description,
                    date = date.time
                )
                taskDao.insert(task)
                // После успешного добавления обновляем список задач
                loadTasksForDate(date)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Ошибка при сохранении задачи")
                onComplete(false)
            }
        }
    }
    
    fun deleteTask(task: Task, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                taskDao.delete(task)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при удалении задачи")
                onComplete(false)
            }
        }
    }
    
    fun toggleTaskCompletion(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(isCompleted = isCompleted)
                taskDao.update(updatedTask)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при обновлении задачи")
            }
        }
    }
    
    
    fun loadExpensesForDate(date: Date) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.time
                
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.time
                
                expenseDao.getExpensesBetweenDates(startOfDay, endOfDay).observeForever { expenses ->
                    _expenses.value = expenses ?: emptyList()
                    _expenseTotal.value = expenses?.sumOf { it.amount } ?: 0.0
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при загрузке расходов: ${e.message}")
            }
        }
    }
    
    
    fun getTasksForDateRange(startDate: Long, endDate: Long) = 
        taskDao.getTasksForDateRange(startDate, endDate)
    
    private val observers = mutableListOf<Observer<List<Expense>>>()
    
    override fun onCleared() {
        super.onCleared()
        // Clear any observers when the ViewModel is cleared
        observers.clear()
    }
    
    fun addExpense(amount: Double, date: Date, note: String, category: String, onComplete: (Boolean) -> Unit) {
        if (amount <= 0) {
            _uiState.value = TaskUiState.Error("Сумма должна быть больше нуля")
            onComplete(false)
            return
        }
        
        viewModelScope.launch {
            try {
                val expense = Expense(
                    amount = amount,
                    date = date,
                    note = note,
                    category = category
                )
                expenseDao.insert(expense)
                loadExpensesForDate(date)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при сохранении расхода")
                onComplete(false)
            }
        }
    }
    
    fun deleteExpense(expense: Expense, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                expenseDao.delete(expense)
                loadExpensesForDate(expense.date)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при удалении расхода")
                onComplete(false)
            }
        }
    }
    
    fun loadProfitsForDate(date: Date) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.time

                profitDao.getProfitsBetweenDates(startOfDay, endOfDay).observeForever { profitsList ->
                    _profits.value = profitsList ?: emptyList()
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при загрузке доходов: ${e.message}")
            }
        }
    }
    
    fun addProfit(amount: Double, date: Date, note: String?, onComplete: (Boolean) -> Unit) {
        if (amount <= 0) {
            _uiState.value = TaskUiState.Error("Сумма должна быть больше нуля")
            onComplete(false)
            return
        }
        
        viewModelScope.launch {
            try {
                val profit = DailyProfit(
                    amount = amount,
                    date = date,
                    note = note ?: ""
                )
                profitDao.insert(profit)
                loadProfitsForDate(date)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при сохранении дохода")
                onComplete(false)
            }
        }
    }
    
    fun deleteProfit(profit: DailyProfit, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                profitDao.delete(profit)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Ошибка при удалении дохода")
                onComplete(false)
            }
        }
    }
}

sealed class TaskUiState {
    object Loading : TaskUiState()
    object Empty : TaskUiState()
    data class Success(val tasks: List<Task>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}
