package com.example.lexify.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.lexify.model.Expense
import java.util.*

@Dao
interface ExpenseDao {

    // Reads
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Long): LiveData<Expense?>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getExpensesBetweenDates(start: Date, end: Date): LiveData<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalAmount(): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :start AND :end")
    fun getTotalAmountBetweenDates(start: Date, end: Date): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category")
    fun getTotalAmountByCategory(category: String): LiveData<Double?>

    // Writes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>): List<Long>

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
