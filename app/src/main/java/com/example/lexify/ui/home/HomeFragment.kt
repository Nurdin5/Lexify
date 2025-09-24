package com.example.lexify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Observer
import com.example.lexify.adapter.CalendarAdapter
import com.example.lexify.data.ProfitDao
import com.example.lexify.ui.task.TaskAdapter
import com.example.lexify.data.AppDatabase
import com.example.lexify.databinding.FragmentHomeBinding
import com.example.lexify.model.CalendarDay
import com.example.lexify.ui.task.TaskViewModel
import com.example.lexify.ui.task.TaskViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class HomeFragment : Fragment() {

    private val binding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tasksAdapter: TaskAdapter
    // Initialize database and DAO lazily
    private val db: AppDatabase by lazy {
        AppDatabase.getDatabase(requireContext().applicationContext)
    }
    
    private val profitDao: ProfitDao by lazy {
        db.profitDao()
    }
    
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(db.taskDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCalendar()
        setupTasksRecyclerView()
        setupClickListeners()
        loadTodayTasks()
        loadTodayIncome()
        loadWeeklyIncome()
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { calendarDay ->
            // Navigate to AddTaskFragment with the selected date
            val action =
                HomeFragmentDirections.actionHomeFragmentToAddTaskFragment(calendarDay.toDate())
            findNavController().navigate(action)
        }

        binding.calendarRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = calendarAdapter
            
            // Generate calendar data asynchronously
            generateCalendarData()
            
            // Add a layout listener to scroll after the view is laid out
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    // We'll handle scrolling after the data is loaded in generateCalendarData()
                }
            })
        }
    }

    private fun setupTasksRecyclerView() {
        tasksAdapter = TaskAdapter(
            onTaskChecked = { task, isChecked ->
                viewModel.toggleTaskCompletion(task, isChecked)
            },
            onTaskDeleted = { task ->
                viewModel.deleteTask(task) { success ->
                    // Task deleted, list will update automatically
                }
            },
            isReadOnly = false
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.addTaskButton.setOnClickListener {
            // Navigate to AddTaskFragment with today's date
            val today = Calendar.getInstance().time
            val action = HomeFragmentDirections.actionHomeFragmentToAddTaskFragment(today.time)
            findNavController().navigate(action)
        }
    }

    private fun loadTodayTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (startOfDay, endOfDay) = getTodayDateRange()

            viewModel.getTasksForDateRange(startOfDay, endOfDay).collectLatest { tasks ->
                if (tasks.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.tasksRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.tasksRecyclerView.visibility = View.VISIBLE
                    tasksAdapter.submitList(tasks)
                }
            }
        }
    }

    private fun loadTodayIncome() {
        val (startOfDay, endOfDay) = getTodayDateRange()

        profitDao.getProfitsBetweenDates(
            Date(startOfDay),
            Date(endOfDay)
        ).observe(viewLifecycleOwner, Observer { profits ->
            val totalIncome = profits.sumOf { it.amount.toDouble() }.toInt()
            binding.todayIncomeText.text = "$totalIncome ₽"
        })
    }

    private fun loadWeeklyIncome() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Set to Monday of current week
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val startOfWeek = calendar.timeInMillis
        val endOfDay = getTodayDateRange().second

        profitDao.getProfitsBetweenDates(
            Date(startOfWeek),
            Date(endOfDay)
        ).observe(viewLifecycleOwner, Observer { profits ->
            val totalIncome = profits.sumOf { it.amount.toDouble() }.toInt()
            binding.weeklyIncomeText.text = "$totalIncome ₽"
        })
    }

    private fun getDateRangeForDay(calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = cal.timeInMillis - 1

        return Pair(startOfDay, endOfDay)
    }
    
    private fun getTodayDateRange(): Pair<Long, Long> {
        return getDateRangeForDay(Calendar.getInstance())
    }

    private suspend fun loadDailyIncomeForDay(startOfDay: Long, endOfDay: Long): Int {
        return try {
            profitDao.getProfitsBetweenDatesSync(
                Date(startOfDay),
                Date(endOfDay)
            ).sumOf { it.amount.toDouble() }.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    private fun generateCalendarData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val days = mutableListOf<CalendarDay>()
            val today = Calendar.getInstance()
            var todayPosition = -1
            
            // Start from the previous Monday (2 weeks before current week's Monday)
            val calendar = today.clone() as Calendar
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.add(Calendar.DAY_OF_MONTH, -14) // Go back 2 weeks to show previous Mondays

            // Generate days for 6 weeks (42 days) to cover a month and a half
            repeat(42) {
            val dayOfWeekValue = calendar.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = when (dayOfWeekValue) {
                Calendar.MONDAY -> "Пн"
                Calendar.TUESDAY -> "Вт"
                Calendar.WEDNESDAY -> "Ср"
                Calendar.THURSDAY -> "Чт"
                Calendar.FRIDAY -> "Пт"
                Calendar.SATURDAY -> "Сб"
                Calendar.SUNDAY -> "Вс"
                else -> ""
            }

            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)
            val isWeekend =
                dayOfWeekValue == Calendar.SATURDAY || dayOfWeekValue == Calendar.SUNDAY
            val isEndOfWeek = dayOfWeekValue == Calendar.SUNDAY

            val isCurrentDay = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

            // Create a new Calendar instance for this specific day
            val dayCalendar = calendar.clone() as Calendar
            
            // Get start and end of the current day for income calculation
            val (startOfDay, endOfDay) = getDateRangeForDay(dayCalendar)
            
            // Get income for this day
            val dailyIncome = loadDailyIncomeForDay(startOfDay, endOfDay)

            days.add(
                CalendarDay(
                    dayOfWeek = dayOfWeek,
                    date = dayOfMonth,
                    year = year,
                    month = month,
                    calendar = dayCalendar,
                    isCurrentDay = isCurrentDay,
                    isWeekend = isWeekend,
                    isEndOfWeek = isEndOfWeek,
                    dailyIncome = dailyIncome
                )
            )

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                calendarAdapter?.submitList(days)
                
                // Scroll to today's position after the data is loaded
                binding.calendarRecyclerView.post {
                    val layoutManager = binding.calendarRecyclerView.layoutManager as? LinearLayoutManager
                    val todayPosition = days.indexOfFirst { it.isCurrentDay }
                    if (todayPosition != -1) {
                        layoutManager?.scrollToPositionWithOffset(todayPosition, 0)
                    }
                }
            }
            }
    }
}
