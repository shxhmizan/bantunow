package com.example.bantunow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bantunow.databinding.FragmentChatbotBinding

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val greetings = arrayOf(
        "Any new ideas to explore?",
        "How can I help you today?",
        "Ready to find some jobs?",
        "Need help posting a task?",
        "How about checking your rewards?"
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

        setupGreetingSwitcher()

        binding.btnBackIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSuggest1.setOnClickListener {
            binding.etChatInput.setText(binding.btnSuggest1.text)
        }

        binding.btnSuggest2.setOnClickListener {
            binding.etChatInput.setText(binding.btnSuggest2.text)
        }

        binding.btnSuggest3.setOnClickListener {
            binding.etChatInput.setText(binding.btnSuggest3.text)
        }

        binding.btnSendFloating.setOnClickListener {
            val text = binding.etChatInput.text.toString()
            if (text.isNotEmpty()) {
                startChatMode()
                binding.etChatInput.text.clear()
            }
        }
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

    private fun startChatMode() {
        binding.llGreeting.visibility = View.GONE
        binding.scrollChat.visibility = View.VISIBLE
        // Logic to add the user's message and bot response would go here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(greetingRunnable)
        _binding = null
    }
}