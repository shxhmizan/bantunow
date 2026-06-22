package com.example.bantunow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bantunow.databinding.FragmentChatbotBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var generativeModel: GenerativeModel

    private val greetings = arrayOf(
        "How can I help you today?",
        "Ready to find some jobs?",
        "Need help posting a task?",
        "Ask me about job pricing!"
    )
    private var greetingIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val greetingRunnable = object : Runnable {
        override fun run() {
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

        initGemini()
        setupGreetingSwitcher()

        binding.btnBackIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.etChatInput.setOnClickListener {
            binding.etChatInput.requestFocus()
        }

        binding.btnSuggest1.setOnClickListener { 
            binding.etChatInput.setText(binding.btnSuggest1.text.toString())
            binding.etChatInput.requestFocus()
        }
        binding.btnSuggest2.setOnClickListener { 
            binding.etChatInput.setText(binding.btnSuggest2.text.toString())
            binding.etChatInput.requestFocus()
        }
        binding.btnSuggest3.setOnClickListener { 
            binding.etChatInput.setText(binding.btnSuggest3.text.toString())
            binding.etChatInput.requestFocus()
        }

        binding.btnSendFloating.setOnClickListener {
            val text = binding.etChatInput.text.toString()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etChatInput.text.clear()
            }
        }
    }

    private fun initGemini() {
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private fun setupGreetingSwitcher() {
        binding.tsGreeting.setFactory {
            TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 28f
                setTextColor(resources.getColor(R.color.white, null))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
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
        
        lifecycleScope.launch {
            try {
                // System instructions for the "Smart Negotiator" persona
                val response = generativeModel.generateContent(
                    content {
                        text("You are BantuBot, a Smart Negotiator for BantuNow, a campus micro-job app. " +
                             "Your goal is to help users find fair prices for jobs and describe tasks better. " +
                             "Be helpful, concise, and professional. Context: UiTM Tapah campus. " +
                             "User says: $userText")
                    }
                )
                addBotMessage(response.text ?: "Sorry, I couldn't process that.")
            } catch (e: Exception) {
                addBotMessage("Error: ${e.message}")
            }
        }
    }

    private fun addUserMessage(message: String) {
        val tv = TextView(context).apply {
            text = message
            setBackgroundResource(R.drawable.bg_chip_status_mint) // Simplified bubble
            setPadding(32, 16, 32, 16)
            setTextColor(resources.getColor(R.color.bg_dark_forest, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                setMargins(0, 8, 0, 8)
            }
        }
        binding.llChatMessages.addView(tv)
        binding.scrollChat.fullScroll(View.FOCUS_DOWN)
    }

    private fun addBotMessage(message: String) {
        val tv = TextView(context).apply {
            text = message
            setBackgroundResource(R.drawable.bg_input_rounded)
            setPadding(32, 16, 32, 16)
            setTextColor(resources.getColor(R.color.white, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                setMargins(0, 8, 0, 8)
            }
        }
        binding.llChatMessages.addView(tv)
        binding.scrollChat.fullScroll(View.FOCUS_DOWN)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(greetingRunnable)
        _binding = null
    }
}