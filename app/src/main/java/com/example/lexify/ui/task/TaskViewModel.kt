package com.example.lexify.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lexify.data.TaskDao
import com.example.lexify.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class TaskViewModel(private val taskDao: TaskDao) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    
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
    
    fun getTasksForDateRange(startDate: Long, endDate: Long) = 
        taskDao.getTasksForDateRange(startDate, endDate)
}

sealed class TaskUiState {
    object Loading : TaskUiState()
    object Empty : TaskUiState()
    data class Success(val tasks: List<Task>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}
