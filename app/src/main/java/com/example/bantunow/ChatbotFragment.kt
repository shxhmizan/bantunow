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
        binding.btnSuggestLeaderboard.setOnClickListener { onSuggestionClicked(binding.btnSuggestLeaderboard.text.toString()) }
        binding.btnSuggestBalance.setOnClickListener { onSuggestionClicked(binding.btnSuggestBalance.text.toString()) }

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
        
        STRICT GUARDRAILS:
        - ONLY answer questions related to the BantuNow app, community micro-tasking, local help, or your capabilities.
        - If a user asks something completely unrelated (e.g., world history, coding in C++, celebrity gossip), politely decline and say: "I'm here to help with BantuNow related things! Let's get back to tasks or points 😊"
        
        Your personality:
        - Warm, encouraging, and casual — like a helpful senior student.
        - Use light emojis occasionally.
        
        Your capabilities & rules:
        - Help users find available jobs. IF THERE ARE NO JOBS IN THE CONTEXT provided, say: "There are no jobs available at the moment."
        - Provide leaderboard info and wallet balance when asked (context will be provided).
        - When a user asks for jobs or what's new, HIGHLIGHT the latest job as "FRESH".
        - When a user wants to accept a task, respond with [ACCEPT_TASK: Task Title].
        - Give advice on pricing (Cleaning: RM15-30, Delivery: RM10-25, Repair: RM30+).
        
        Response style:
        - Concise and scannable with **bold** highlights.
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
                // Agentic logic: Fetch jobs/leaderboard/wallet if relevant
                var contextInjected = ""
                
                // 1. Job context
                if (userText.contains("job", true) || userText.contains("task", true)) {
                    val isNearby = userText.contains("near me", true) || userText.contains("nearby", true)
                    var loc: Location? = null
                    if (isNearby) loc = (activity as? MainActivity)?.requestLocation()?.await()
                    
                    val jobs = fetchAvailableJobs(loc)
                    if (jobs.isNotEmpty()) {
                        val jobsText = jobs.mapIndexed { i, t ->
                            "- ${if(i==0)"[LATEST] " else ""}**${t.title}**: RM${t.paymentAmount?.div(100)} (${t.category})"
                        }.joinToString("\n")
                        contextInjected += "Available Jobs:\n$jobsText\n\n"
                    } else {
                        contextInjected += "Context: No jobs are currently available in the database.\n\n"
                    }
                }

                // 2. Leaderboard context
                if (userText.contains("leaderboard", true) || userText.contains("leading", true) || userText.contains("top", true)) {
                    val topUsers = fetchTopUsers()
                    if (topUsers.isNotEmpty()) {
                        val lbText = topUsers.mapIndexed { i, u -> "${i+1}. ${u.first}: ${u.second} pts" }.joinToString("\n")
                        contextInjected += "Leaderboard (Top Users):\n$lbText\n\n"
                    }
                }

                // 3. Wallet context
                if (userText.contains("balance", true) || userText.contains("wallet", true) || userText.contains("money", true)) {
                    val balance = fetchUserBalance()
                    contextInjected += "Current User Wallet Balance: RM $balance\n\n"
                }

                val finalUserText = if (contextInjected.isNotEmpty()) {
                    "$contextInjected User Message: $userText"
                } else userText

                // Add to history
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

    private suspend fun fetchUserBalance(): Double = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext 0.0
        try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getDouble("walletBalance") ?: 0.0
        } catch (e: Exception) { 0.0 }
    }

    private suspend fun fetchTopUsers(): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("users")
                .orderBy("bantuPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .await()
            snapshot.documents.map { 
                (it.getString("displayName") ?: "User") to (it.getLong("bantuPoints") ?: 0L)
            }
        } catch (e: Exception) { emptyList() }
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
