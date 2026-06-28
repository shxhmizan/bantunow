package com.example.bantunow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.bantunow.databinding.FragmentTransactionHistoryBinding
import com.example.bantunow.databinding.ItemTransactionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

data class TransactionItem(
    val type: String = "",
    val amount: Long = 0,
    val description: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

class TransactionHistoryFragment : Fragment() {

    private var _binding: FragmentTransactionHistoryBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        fetchTransactions()
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("transactions")
            .whereEqualTo("walletID", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val transactions = documents.toObjects(TransactionItem::class.java)
                binding.rvTransactions.adapter = TransactionAdapter(transactions, requireContext())
            }
            .addOnFailureListener { e ->
                Log.e("TransHistory", "Error fetching transactions", e)
                if (e.message?.contains("index") == true) {
                    Toast.makeText(context, "Search index is being created. Please wait a few minutes.", Toast.LENGTH_LONG).show()
                }
            }
    }

    class TransactionAdapter(private val items: List<TransactionItem>, private val context: android.content.Context) :
        RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvDescription.text = item.description
            
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.binding.tvDate.text = item.timestamp?.toDate()?.let { sdf.format(it) } ?: "-"

            val amountFormatted = String.format(Locale.US, "%.2f", item.amount.toDouble() / 100.0)
            
            val tealColor = ContextCompat.getColor(context, R.color.accent_teal)
            val coralColor = ContextCompat.getColor(context, R.color.accent_coral_faded)
            val mintBg = android.graphics.Color.parseColor("#E0F2F1")
            val coralBg = android.graphics.Color.parseColor("#FFEBEE")

            when (item.type) {
                "topup", "received" -> {
                    holder.binding.tvAmount.text = context.getString(R.string.positive_amount, amountFormatted)
                    holder.binding.tvAmount.setTextColor(tealColor)
                    holder.binding.ivType.setImageResource(R.drawable.ic_history)
                    holder.binding.ivType.setColorFilter(tealColor)
                    holder.binding.cardIcon.setCardBackgroundColor(mintBg)
                }
                "payment", "received_payment" -> {
                    holder.binding.tvAmount.text = context.getString(R.string.positive_amount, amountFormatted)
                    holder.binding.tvAmount.setTextColor(tealColor)
                    holder.binding.ivType.setImageResource(R.drawable.ic_list)
                    holder.binding.ivType.setColorFilter(tealColor)
                    holder.binding.cardIcon.setCardBackgroundColor(mintBg)
                }
                "withdrawal", "deduction", "posted_task" -> {
                    holder.binding.tvAmount.text = context.getString(R.string.negative_amount, amountFormatted)
                    holder.binding.tvAmount.setTextColor(coralColor)
                    holder.binding.ivType.setImageResource(R.drawable.ic_logout)
                    holder.binding.ivType.setColorFilter(coralColor)
                    holder.binding.cardIcon.setCardBackgroundColor(coralBg)
                }
                else -> {
                    holder.binding.tvAmount.text = "RM $amountFormatted"
                    holder.binding.cardIcon.setCardBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                }
            }
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}