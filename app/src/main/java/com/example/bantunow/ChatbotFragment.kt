package com.example.bantunow

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bantunow.data.model.Task
import com.example.bantunow.databinding.FragmentChatbotBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val db = FirebaseFirestore.getInstance()
    private var chatHistory = JsonArray()
    private val gson = Gson()

    private val greetings = mutableListOf<String>()
    private var greetingIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val greetingRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            greetingIndex = (greetingIndex + 1) % greetings.size
            binding.tsGreeting.setText(greetings[greetingIndex])
            handler.postDelayed(this, 4000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSavedHistory()
        setupGreetings()
        setupGreetingSwitcher()

        // Close keyboard when tapping on background
        binding.root.setOnClickListener { hideKeyboard() }
        binding.llChatMessages.setOnClickListener { hideKeyboard() }
        binding.scrollChat.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            v.performClick()
            false
        }

        binding.btnBackIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNewChat.setOnClickListener {
            // Reset chat history
            initChatHistory()
            binding.llChatMessages.removeAllViews()
            binding.llGreeting.visibility = View.VISIBLE
            binding.scrollChat.visibility = View.GONE
            clearSavedHistory()
        }

        binding.btnSuggestJob.setOnClickListener { onSuggestionClicked(binding.btnSuggestJob.text.toString()) }
        binding.btnSuggestPrice.setOnClickListener { onSuggestionClicked(binding.btnSuggestPrice.text.toString()) }
        binding.btnSuggestPoints.setOnClickListener { onSuggestionClicked(binding.btnSuggestPoints.text.toString()) }

        binding.btnSendFloating.setOnClickListener {
            val text = binding.etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etChatInput.text.clear()
            }
        }
    }

    private fun initChatHistory() {
        chatHistory = JsonArray()
        val systemMsg = JsonObject()
        systemMsg.addProperty("role", "system")
        systemMsg.addProperty("content", """
        You are BantuBot 🤖, the friendly AI companion for BantuNow — Empowering Communities through Hyperlocal, AI-Driven Micro-Tasking.
        
        Your personality:
        - Warm, encouraging, and casual — like a helpful senior student
        - Use light emojis occasionally (not every sentence) to feel approachable 😊
        - Celebrate wins with the user (e.g. when they accept a task or earn points)
        - If the user seems stressed or unsure, reassure them gently
        - Never sound robotic or corporate — keep it human and relatable
        
        Your capabilities:
        - Help users find available jobs/tasks on the platform
        - When a user asks for jobs or what's new, always HIGHLIGHT the latest job (the first one in the list provided in context) as "FRESH" or "NEWLY ADDED".
        - When a user wants to accept a task, respond with [ACCEPT_TASK: Task Title]
        - Give advice on pricing, BantuPoints, and how the platform works
        - Answer general questions a user might have
        
        Response style:
        - Keep responses concise and scannable
        - Use **bold** for important words or task titles
        - Use bullet points for lists
        - End with a soft follow-up question or encouragement when appropriate
        
        Example tone:
        "Hey! I found 3 tasks near you 🎯 Here's what's available right now..."
        "Great choice! Let me secure that for you right away 💪"
        "No worries, I've got you covered!"
    """.trimIndent())
        chatHistory.add(systemMsg)
    }

    private fun loadSavedHistory() {
        val prefs = requireContext().getSharedPreferences("BantuBotPrefs", 0)
        val saved = prefs.getString("chat_history_${FirebaseAuth.getInstance().currentUser?.uid}", null)
        if (saved != null) {
            try {
                chatHistory = gson.fromJson(saved, JsonArray::class.java)
                binding.llGreeting.visibility = View.GONE
                binding.scrollChat.visibility = View.VISIBLE
                
                // Redraw history
                binding.llChatMessages.removeAllViews()
                for (i in 1 until chatHistory.size()) { // Skip system message
                    val msg = chatHistory[i].asJsonObject
                    val role = msg.get("role").asString
                    val content = msg.get("content").asString
                    if (role == "user") {
                        addUserMessage(content)
                    } else if (role == "assistant") {
                        addBotMessage(content)
                    }
                }
            } catch (e: Exception) {
                initChatHistory()
            }
        } else {
            initChatHistory()
        }
    }

    private fun saveHistory() {
        val prefs = requireContext().getSharedPreferences("BantuBotPrefs", 0)
        prefs.edit().putString(
            "chat_history_${FirebaseAuth.getInstance().currentUser?.uid}",
            gson.toJson(chatHistory)
        ).apply()
    }

    private fun clearSavedHistory() {
        val prefs = requireContext().getSharedPreferences("BantuBotPrefs", 0)
        prefs.edit().remove("chat_history_${FirebaseAuth.getInstance().currentUser?.uid}").apply()
    }

    private fun setupGreetings() {
        val name = FirebaseAuth.getInstance().currentUser?.displayName
            ?.split(" ")?.firstOrNull() ?: "there"
        greetings.add("Let's jump\nin, $name")
        greetings.add("How can I\nhelp today?")
        greetings.add("Ask me\nanything...")
        greetings.add("Need a hand\nwith a task?")
    }

    private fun setupGreetingSwitcher() {
        binding.tsGreeting.setFactory {
            TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 32f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setLineSpacing(0f, 1.1f)
                try {
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(
                        requireContext(), R.font.playfair_display
                    )
                } catch (e: Exception) {
                    // Fallback to default font
                }
            }
        }

        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
        binding.tsGreeting.inAnimation = fadeIn
        binding.tsGreeting.outAnimation = fadeOut

        binding.tsGreeting.setCurrentText(greetings[0])
        handler.postDelayed(greetingRunnable, 4000)
    }

    private fun sendMessage(userText: String) {
        if (binding.llGreeting.visibility == View.VISIBLE) {
            binding.llGreeting.visibility = View.GONE
            binding.scrollChat.visibility = View.VISIBLE
        }

        addUserMessage(userText)
        showTypingIndicator()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Agentic logic: Fetch jobs if user asks
                var finalUserText = userText
                if (userText.contains("job", ignoreCase = true) || userText.contains("task", ignoreCase = true)) {
                    val isNearby = userText.contains("near me", ignoreCase = true) || 
                                     userText.contains("nearby", ignoreCase = true) ||
                                     userText.contains("around", ignoreCase = true)
                    
                    var userLocation: Location? = null
                    if (isNearby) {
                        try {
                            userLocation = (activity as? MainActivity)?.requestLocation()?.await()
                        } catch (e: Exception) {
                            Log.e("BantuBot", "Failed to get location", e)
                        }
                    }

                    val jobs = fetchAvailableJobs(userLocation)
                    if (jobs.isNotEmpty()) {
                        val jobsText = jobs.mapIndexed { index, task ->
                            val distStr = if (userLocation != null && task.latitude != null && task.longitude != null) {
                                val taskLoc = Location("task").apply {
                                    latitude = task.latitude!!
                                    longitude = task.longitude!!
                                }
                                " (%.2f km away)".format(userLocation.distanceTo(taskLoc) / 1000.0)
                            } else ""
                            val tag = if (index == 0) "[LATEST/NEWLY ADDED] " else ""
                            "- $tag**${task.title}**: RM${task.paymentAmount?.div(100)}$distStr (${task.category})"
                        }.joinToString("\n")
                        finalUserText = "Context: The following jobs are currently available in the database (sorted by latest):\n$jobsText\n\nUser Message: $userText"
                    }
                }

                // Add to history (after potential context enrichment)
                val userMsg = JsonObject()
                userMsg.addProperty("role", "user")
                userMsg.addProperty("content", finalUserText)
                chatHistory.add(userMsg)
                saveHistory()

                val responseText = callOpenRouter()
                removeTypingIndicator()
                
                // Check for agentic triggers
                if (responseText.contains("[ACCEPT_TASK:", ignoreCase = true)) {
                    val taskTitle = responseText.substringAfter("[ACCEPT_TASK:").substringBefore("]").trim()
                    handleAcceptTask(taskTitle)
                    
                    val cleanResponse = responseText.replace(Regex("\\[ACCEPT_TASK:.*?]"), "").trim()
                    if (cleanResponse.isNotEmpty()) addBotMessage(cleanResponse)
                } else {
                    addBotMessage(responseText)
                }
                
                // Add to history
                val assistantMsg = JsonObject()
                assistantMsg.addProperty("role", "assistant")
                assistantMsg.addProperty("content", responseText)
                chatHistory.add(assistantMsg)
                saveHistory()
            } catch (e: Exception) {
                Log.e("BantuBot", "Error: ${e.message}", e)
                removeTypingIndicator()
                addBotMessage("Sorry, I'm having trouble connecting. Please try again.")
            }
        }
    }

    private suspend fun fetchAvailableJobs(userLocation: Location? = null): List<Task> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("tasks")
                .whereEqualTo("status", "open")
                .get()
                .await()
            val allTasks = snapshot.toObjects(Task::class.java)

            if (userLocation != null) {
                allTasks.sortedBy { task ->
                    if (task.latitude != null && task.longitude != null) {
                        val taskLoc = Location("task").apply {
                            latitude = task.latitude!!
                            longitude = task.longitude!!
                        }
                        userLocation.distanceTo(taskLoc)
                    } else {
                        Float.MAX_VALUE
                    }
                }.take(5)
            } else {
                allTasks.sortedByDescending { it.createdAt }.take(5)
            }
        } catch (e: Exception) {
            Log.e("BantuBot", "Error fetching jobs", e)
            emptyList()
        }
    }

    private fun handleAcceptTask(title: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val snapshot = db.collection("tasks")
                    .whereEqualTo("title", title)
                    .whereEqualTo("status", "open")
                    .limit(1)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val taskRef = doc.reference
                    val userRef = db.collection("users").document(currentUserId)
                    
                    db.runTransaction { transaction ->
                        val userDoc = transaction.get(userRef)
                        val currentPoints = userDoc.getLong("bantuPoints") ?: 0L
                        
                        transaction.update(taskRef, "status", "in_progress")
                        transaction.update(taskRef, "workerID", currentUserId)
                        transaction.update(userRef, "bantuPoints", currentPoints + 5L)
                    }.await()
                    
                    addBotMessage("✅ Done! I've accepted the task '**$title**' for you. You've earned **5 BantuPoints**! Check your tasks for details.")
                } else {
                    addBotMessage("I couldn't find an open task with the title '**$title**'. It might have been taken already.")
                }
            } catch (e: Exception) {
                Log.e("BantuBot", "Error accepting task", e)
                addBotMessage("I encountered an error while trying to accept that task. Please try accepting it manually.")
            }
        }
    }

    private suspend fun callOpenRouter(): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.OPENROUTER_API_KEY
        } catch (e: Exception) {
            ""
        }
        
        if (apiKey.isEmpty()) return@withContext "API Key is missing. Please configure OPENROUTER_API_KEY in local.properties."

        val jsonBody = JsonObject()
        // Using Gemini 2.0 Flash Lite for better performance and cost.
        jsonBody.addProperty("model", "google/gemini-2.5-flash-lite")
        jsonBody.add("messages", chatHistory)

        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://bantunow.app")
            .header("X-Title", "BantuNow")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("BantuBot", "API Error ${response.code}: $errorBody")
                throw Exception("Unexpected code ${response.code}")
            }

            val result = response.body?.string() ?: throw Exception("Empty response")
            val jsonResponse = gson.fromJson(result, JsonObject::class.java)
            val choices = jsonResponse.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                choices[0].asJsonObject.getAsJsonObject("message").get("content").asString
            } else {
                "I'm not sure how to respond to that."
            }
        }
    }

    private fun onSuggestionClicked(text: String) {
        sendMessage(text)
    }

    private fun addUserMessage(message: String) {
        val tv = TextView(requireContext()).apply {
            text = parseMarkdown(message)
            setBackgroundResource(R.drawable.bg_input_rounded)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.brand_dark)
            setPadding(40, 24, 40, 24)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                setMargins(100, 12, 0, 12)
            }
        }
        binding.llChatMessages.addView(tv)
        scrollToBottom()
    }

    private fun addBotMessage(message: String) {
        val tv = TextView(requireContext()).apply {
            text = parseMarkdown(message)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setPadding(20, 24, 40, 24)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                setMargins(0, 12, 0, 12)
            }
        }
        binding.llChatMessages.addView(tv)
        scrollToBottom()
    }

    private fun parseMarkdown(text: String): Spanned {
        // Simple markdown parser for bold **text**
        val html = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            .replace("\n", "<br>")
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    }

    // Typing indicator tag for easy removal
    private val TYPING_TAG = "typing_indicator"

    private fun showTypingIndicator() {
        val tv = TextView(requireContext()).apply {
            text = "BantuBot is typing..."
            tag = TYPING_TAG
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            alpha = 0.6f
            setPadding(20, 12, 40, 12)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                setMargins(0, 8, 0, 8)
            }
        }
        binding.llChatMessages.addView(tv)
        scrollToBottom()
    }

    private fun removeTypingIndicator() {
        val tv = binding.llChatMessages.findViewWithTag<TextView>(TYPING_TAG)
        if (tv != null) binding.llChatMessages.removeView(tv)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun scrollToBottom() {
        binding.scrollChat.post {
            binding.scrollChat.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(greetingRunnable)
        _binding = null
    }
}
