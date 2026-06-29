package com.example.bantunow

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bantunow.databinding.ActivityTodoListBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TodoListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodoListBinding
    private val adapter = TodoAdapter()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvTodos.layoutManager = LinearLayoutManager(this)
        binding.rvTodos.adapter = adapter

        loadTodos()
    }

    private fun loadTodos() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val result = db.collection("todos").get().await()
                val todos = result.toObjects(TodoItem::class.java)
                adapter.submitList(todos)
            } catch (e: Exception) {
                Toast.makeText(this@TodoListActivity, "Failed to load todos: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
