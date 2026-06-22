package com.example.bantunow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bantunow.databinding.ItemUserJobBinding
import com.google.firebase.auth.FirebaseAuth

class JobAdapter(
    private var jobs: List<Job>,
    private val onJobClick: (Job) -> Unit,
    private val onCancelClick: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(val binding: ItemUserJobBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemUserJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        val context = holder.itemView.context
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.tvJobTitle.text = job.title
        holder.binding.tvJobDesc.text = job.description
        holder.binding.tvJobPay.text = "RM ${job.pay}"
        holder.binding.tvStatusChip.text = job.status

        // Set role chip based on user relation to job
        if (job.ownerId == currentUserId) {
            holder.binding.tvRoleChip.text = "YOU POSTED THIS"
            holder.binding.tvRoleChip.visibility = View.VISIBLE
            holder.binding.btnDeleteJob.visibility = View.VISIBLE
        } else if (job.performerId == currentUserId) {
            holder.binding.tvRoleChip.text = "YOU ARE PERFORMER"
            holder.binding.tvRoleChip.visibility = View.VISIBLE
            holder.binding.btnDeleteJob.visibility = View.VISIBLE
        } else {
            holder.binding.tvRoleChip.visibility = View.GONE
            holder.binding.btnDeleteJob.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener { onJobClick(job) }
        holder.binding.btnDeleteJob.setOnClickListener { onCancelClick(job) }
    }

    override fun getItemCount() = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
}
