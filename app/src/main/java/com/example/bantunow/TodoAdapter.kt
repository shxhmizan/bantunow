package com.example.bantunow

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bantunow.databinding.ItemTodoBinding

class TodoAdapter : ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(TodoDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TodoViewHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TodoItem) {
            binding.tvTodoName.text = item.name
        }
    }

    private object TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem) = oldItem == newItem
    }
}
