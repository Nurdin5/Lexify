package com.example.lexify.data

import androidx.room.*
import com.example.lexify.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date BETWEEN :startOfDay AND :endOfDay ORDER BY id DESC")
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY id DESC")
    fun getTasksForDateRange(startDate: Long, endDate: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
}
