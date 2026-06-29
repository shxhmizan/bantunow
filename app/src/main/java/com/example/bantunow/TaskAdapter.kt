package com.example.bantunow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bantunow.data.model.Task
import com.example.bantunow.databinding.ItemUserJobBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onCancelClick: (Task) -> Unit,
    private val onEditClick: ((Task) -> Unit)? = null,
    private var userLocation: android.location.Location? = null
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val userCache = mutableMapOf<String, Pair<String, String?>>() // ID -> (Name, Avatar)
    private val ratingCache = mutableMapOf<String, Double>() // ID -> Rating

    class TaskViewHolder(val binding: ItemUserJobBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemUserJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.tvJobTitle.text = task.title
        holder.binding.tvJobDesc.text = task.desc
        holder.binding.tvJobPay.text = "RM ${String.format("%.2f", (task.paymentAmount?.toDouble() ?: 0.0) / 100.0)}"
        holder.binding.tvStatusChip.text = task.status.uppercase()

        // Distance logic
        if (userLocation != null && task.latitude != null && task.longitude != null) {
            val taskLoc = android.location.Location("task").apply {
                latitude = task.latitude!!
                longitude = task.longitude!!
            }
            val distanceKm = userLocation!!.distanceTo(taskLoc) / 1000.0
            holder.binding.tvJobDistance.text = String.format(java.util.Locale.US, "%.1f km", distanceKm)
            holder.binding.llDistance.visibility = View.VISIBLE
        } else {
            holder.binding.llDistance.visibility = View.GONE
        }

        if (!task.imageUrl.isNullOrEmpty()) {
            holder.binding.cardImageContainer.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(task.imageUrl)
                .placeholder(R.drawable.bg_form_input)
                .error(R.drawable.bg_form_input)
                .into(holder.binding.ivJobImage)
        } else {
            holder.binding.cardImageContainer.visibility = View.GONE
        }
        
        // Fetch Assigner Name
        task.ownerID?.let { ownerId ->
            if (userCache.containsKey(ownerId)) {
                holder.binding.tvAssignerName.text = userCache[ownerId]?.first
            } else {
                holder.binding.tvAssignerName.text = "Loading..."
                db.collection("users").document(ownerId).get().addOnSuccessListener { doc ->
                    val name = doc.getString("displayName") ?: "Unknown"
                    val avatar = doc.getString("profileImageUrl")
                    userCache[ownerId] = name to avatar
                    notifyItemChanged(position)
                }
            }
        }

        // Role and Accepter Info Logic
        if (task.ownerID == currentUserId) {
            holder.binding.tvRoleChip.text = "YOU POSTED THIS"
            holder.binding.tvRoleChip.visibility = View.VISIBLE
            
            val isActive = task.status != "completed" && task.status != "done"
            holder.binding.btnDeleteJob.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.binding.btnEditJob.visibility = if (isActive) View.VISIBLE else View.GONE

            // If job is accepted, show performer info to owner
            if (!task.workerID.isNullOrEmpty()) {
                holder.binding.llAccepter.visibility = View.VISIBLE
                val workerId = task.workerID!!
                
                if (userCache.containsKey(workerId) && ratingCache.containsKey(workerId)) {
                    val (name, avatar) = userCache[workerId]!!
                    holder.binding.tvAccepterName.text = "Performer: $name"
                    holder.binding.tvAccepterRating.text = "Rating: ${String.format("%.1f", ratingCache[workerId])} ★"
                    
                    Glide.with(holder.itemView.context)
                        .load(avatar)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(holder.binding.ivAccepterAvatar)
                } else {
                    db.collection("users").document(workerId).get().addOnSuccessListener { doc ->
                        val name = doc.getString("displayName") ?: "User"
                        val avatar = doc.getString("profileImageUrl")
                        val rating = doc.getDouble("rating") ?: 5.0
                        userCache[workerId] = name to avatar
                        ratingCache[workerId] = rating
                        notifyItemChanged(position)
                    }
                }
            } else {
                holder.binding.llAccepter.visibility = View.GONE
            }

        } else if (task.workerID == currentUserId) {
            holder.binding.tvRoleChip.text = "YOU ARE PERFORMER"
            holder.binding.tvRoleChip.visibility = View.VISIBLE
            holder.binding.btnDeleteJob.visibility = View.GONE
            holder.binding.btnEditJob.visibility = View.GONE
            holder.binding.llAccepter.visibility = View.GONE
        } else {
            holder.binding.tvRoleChip.visibility = View.GONE
            holder.binding.btnDeleteJob.visibility = View.GONE
            holder.binding.btnEditJob.visibility = View.GONE
            holder.binding.llAccepter.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener { onTaskClick(task) }
        holder.binding.btnDeleteJob.setOnClickListener { onCancelClick(task) }
        holder.binding.btnEditJob.setOnClickListener { onEditClick?.invoke(task) }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun updateUserLocation(location: android.location.Location?) {
        userLocation = location
        notifyDataSetChanged()
    }
}
