package com.example.lexify.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.lexify.model.DailyProfit
import java.util.*

@Dao
interface ProfitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profit: DailyProfit): Long

    @Update
    suspend fun update(profit: DailyProfit)

    @Delete
    suspend fun delete(profit: DailyProfit)

    @Query("SELECT * FROM daily_profits WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getProfitsBetweenDates(startDate: Date, endDate: Date): LiveData<List<DailyProfit>>
    
    @Query("SELECT * FROM daily_profits WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getProfitsBetweenDatesSync(startDate: Date, endDate: Date): List<DailyProfit>

    @Query("SELECT * FROM daily_profits WHERE id = :profitId")
    suspend fun getProfitById(profitId: Long): DailyProfit?

    @Query("SELECT * FROM daily_profits WHERE strftime('%Y-%m-%d', date/1000, 'unixepoch') = strftime('%Y-%m-%d', :date/1000, 'unixepoch')")
    fun getProfitsForDate(date: Date): LiveData<List<DailyProfit>>
}
