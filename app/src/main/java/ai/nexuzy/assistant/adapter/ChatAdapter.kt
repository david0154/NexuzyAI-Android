package ai.nexuzy.assistant.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ai.nexuzy.assistant.R
import ai.nexuzy.assistant.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_USER = 0
        const val VIEW_AI = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].isUser) VIEW_USER else VIEW_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_USER) {
            val v = inflater.inflate(R.layout.item_message_user, parent, false)
            UserVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_ai, parent, false)
            AiVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
        when (holder) {
            is UserVH -> { holder.text.text = msg.text; holder.time.text = time }
            is AiVH  -> { holder.text.text = msg.text; holder.time.text = time }
        }
    }

    override fun getItemCount() = messages.size

    class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.msgText)
        val time: TextView = v.findViewById(R.id.msgTime)
    }
    class AiVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.msgText)
        val time: TextView = v.findViewById(R.id.msgTime)
    }
}
