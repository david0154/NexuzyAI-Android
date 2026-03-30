package ai.nexuzy.assistant.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
        const val VIEW_AI   = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].isUser) VIEW_USER else VIEW_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_USER) {
            UserVH(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            AiVH(inflater.inflate(R.layout.item_message_ai, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg  = messages[position]
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))

        when (holder) {
            is UserVH -> {
                holder.text.text = msg.text
                holder.time.text = time
                holder.bubble.setOnLongClickListener {
                    copyToClipboard(it.context, msg.text)
                    true
                }
            }
            is AiVH -> {
                holder.text.text = msg.text
                holder.time.text = time
                holder.bubble.setOnLongClickListener {
                    copyToClipboard(it.context, msg.text)
                    true
                }
            }
        }
    }

    override fun getItemCount() = messages.size

    /** Copies [text] to system clipboard and shows a Toast confirmation */
    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NexuzyAI Message", text))
        // Android 13+ shows its own system copy notification; show Toast for older versions
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, "\uD83D\uDCCB Copied!", Toast.LENGTH_SHORT).show()
        }
    }

    class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        val text:   TextView = v.findViewById(R.id.msgText)
        val time:   TextView = v.findViewById(R.id.msgTime)
        val bubble: View     = v.findViewById(R.id.bubbleCard)  // CardView root for long-press
    }

    class AiVH(v: View) : RecyclerView.ViewHolder(v) {
        val text:   TextView = v.findViewById(R.id.msgText)
        val time:   TextView = v.findViewById(R.id.msgTime)
        val bubble: View     = v.findViewById(R.id.bubbleCard)  // CardView root for long-press
    }
}
