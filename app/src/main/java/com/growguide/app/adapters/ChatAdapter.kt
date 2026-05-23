package com.growguide.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import com.growguide.app.R
import com.growguide.app.models.ChatMessage

/**
 * RecyclerView adapter for AI chat conversation.
 * Uses two view types to display user bubbles (green, right) and AI bubbles (grey, left).
 * AI messages are rendered as Markdown so bold, lists, and formatting display correctly.
 */
class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ASSISTANT = 1
    }

    inner class ChatViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    // Determine which bubble layout to use based on the message role
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].role == "user") VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_chat_user    // Green bubble, right-aligned
        } else {
            R.layout.item_chat_ai      // Grey bubble, left-aligned
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.role == "assistant") {
            // Render AI replies as Markdown (bold, bullets, etc.)
            val markwon = Markwon.create(holder.messageText.context)
            markwon.setMarkdown(holder.messageText, message.content)
        } else {
            holder.messageText.text = message.content
        }
    }

    override fun getItemCount(): Int = messages.size
}
