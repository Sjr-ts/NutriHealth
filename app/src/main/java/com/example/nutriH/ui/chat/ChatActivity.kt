package com.example.nutrih.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrih.R
import com.example.nutrih.domain.ChatMessage
import com.example.nutrih.domain.IChatRepository
import com.example.nutrih.di.ServiceLocator
import com.example.nutrih.databinding.ActivityChatBinding
import com.example.nutrih.utils.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        const val EXTRA_SPECIALIST_NAME = "extra_specialist_name"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private var appointmentId: Long = -1
    private var specialistName: String = "Especialista"

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(
            applicationContext,
            appointmentId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentId = intent.getLongExtra(EXTRA_APPOINTMENT_ID, -1)
        specialistName = intent.getStringExtra(EXTRA_SPECIALIST_NAME) ?: "Especialista"

        if (appointmentId == -1L) {
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.toolbarChat.title = getString(R.string.chat_title_prefix) + " " + specialistName
        binding.toolbarChat.setNavigationOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString()
            if (messageText.isNotBlank()) {
                viewModel.sendMessage(messageText)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.recyclerViewChat.adapter = chatAdapter
        binding.recyclerViewChat.layoutManager = layoutManager
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerViewChat.smoothScrollToPosition(messages.size - 1)
            }
        }
    }
}

class ChatViewModel(
    private val repository: IChatRepository,
    private val appointmentId: Long
) : ViewModel() {

    val messages: LiveData<List<ChatMessage>> = repository.getMessages(appointmentId)

    fun sendMessage(messageText: String) {
        val encryptedText = SecurityUtils.encrypt(messageText)

        val userMessage = ChatMessage(
            appointmentId = appointmentId,
            text = encryptedText,
            timestamp = System.currentTimeMillis(),
            isUserSender = true
        )

        viewModelScope.launch {
            repository.sendMessage(userMessage)
            simulateSpecialistResponse(messageText)
        }
    }

    private fun simulateSpecialistResponse(userMessageOriginal: String) {
        viewModelScope.launch {
            delay(2000)
            val responseText = when {
                userMessageOriginal.lowercase().contains("olá") -> "Olá! Como posso ajudar?"
                userMessageOriginal.lowercase().contains("dieta") -> "Sobre sua dieta, me diga mais..."
                else -> "Recebido. Estou analisando."
            }
            val encryptedResponse = SecurityUtils.encrypt(responseText)
            val specialistMessage = ChatMessage(
                appointmentId = appointmentId,
                text = encryptedResponse,
                timestamp = System.currentTimeMillis(),
                isUserSender = false
            )
            repository.sendMessage(specialistMessage)
        }
    }
}

class ChatViewModelFactory(
    private val context: android.content.Context,
    private val appointmentId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(ServiceLocator.provideChatRepository(context), appointmentId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatComparator()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUserSender) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentMessageViewHolder) holder.bind(message)
        else if (holder is ReceivedMessageViewHolder) holder.bind(message)
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_message_text)
        fun bind(message: ChatMessage) {
            messageText.text = SecurityUtils.decrypt(message.text)
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_message_text)
        fun bind(message: ChatMessage) {
            messageText.text = SecurityUtils.decrypt(message.text)
        }
    }

    class ChatComparator : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}