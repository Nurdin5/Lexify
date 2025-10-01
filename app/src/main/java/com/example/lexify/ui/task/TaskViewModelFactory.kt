package com.example.lexify.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lexify.data.ExpenseDao
import com.example.lexify.data.ProfitDao
import com.example.lexify.data.TaskDao

class TaskViewModelFactory(
    private val taskDao: TaskDao,
    private val expenseDao: ExpenseDao,
    private val profitDao: ProfitDao
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(taskDao, expenseDao, profitDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
