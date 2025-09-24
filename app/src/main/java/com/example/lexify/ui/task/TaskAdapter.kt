package com.example.lexify.ui.task

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lexify.databinding.ItemTaskBinding
import com.example.lexify.model.Task

class TaskAdapter(
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskDeleted: (Task) -> Unit,
    private val isReadOnly: Boolean = false
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                taskTitle.text = task.title
                taskCheckbox.isChecked = task.isCompleted
                
                // Strike through text if task is completed
                taskTitle.paint.isStrikeThruText = task.isCompleted
                
                // Disable checkbox and change appearance for read-only mode
                taskCheckbox.isEnabled = !isReadOnly
                taskCheckbox.alpha = if (isReadOnly) 0.6f else 1.0f
                
                // Handle checkbox click if not in read-only mode
                taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (!isReadOnly) {
                        onTaskChecked(task, isChecked)
                        taskTitle.paint.isStrikeThruText = isChecked
                    }
                }
                
                // Handle task deletion (e.g., on long click) if not in read-only mode
                root.setOnLongClickListener {
                    if (!isReadOnly) {
                        onTaskDeleted(task)
                    } else {
                        // Optionally show a message that editing is not allowed
                        // Toast.makeText(root.context, "Редактирование прошедших задач недоступно", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                
                // Change appearance for read-only mode
                root.alpha = if (isReadOnly) 0.8f else 1.0f
                taskTitle.alpha = if (isReadOnly) 0.7f else 1.0f
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
