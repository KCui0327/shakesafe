package com.example.wander
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wander.R
import com.google.android.gms.common.api.ApiException
import com.google.gson.annotations.SerializedName
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import retrofit2.Callback
import java.io.IOException
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.io.File
import java.util.*

data class ChatMessage(val sender: String, val messageText: String)

class MessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageTextView.text = message.messageText
        holder.messageSender.text = message.sender
    }


    override fun getItemCount(): Int {
        return messages.size
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageText)
        val messageSender: TextView = itemView.findViewById(R.id.messageSender)
    }
}

// Define the request object
data class CompletionRequest(
    val prompt: String,
    val max_tokens: Int,
    val temperature: Double = 0.5,
    val top_p: Double = 1.0,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0,
    val stop: List<String>? = null,
    val model: String = "text-davinci-003"
)

// Define the response object
data class CompletionResponse(
    val id: String,
    @SerializedName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<CompletionChoice>
)

data class CompletionChoice(
    val text: String,
    val index: Int,
    val logprobs: Map<String, Double>?,
    val finishReason: String?
)


interface OpenAIInterface {
    @POST("completions")
    fun getCompletions(
        @Header("Authorization") apiKey: String,
        @Body completionRequest: CompletionRequest
    ): Call<CompletionResponse>
}

const val BASE_URL = "https://api.openai.com/v1/"

const val CHAT_BOT_PREFIX = "Please act as a AI replier to someone trapped under a wreckage, tell them help is on the way. Human: "

class ChatActivity : AppCompatActivity() {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var closeButton: ImageView

    private val messages = mutableListOf<ChatMessage>()
    private val adapter = MessageAdapter(messages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val localProperties = Properties()
        val localPropertiesFile = File("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val apiKey = "Bearer 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"

        messageRecyclerView = findViewById(R.id.recycler_view)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_button)
        closeButton = findViewById<ImageView>(R.id.close_button)

        messageRecyclerView.adapter = adapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)

        closeButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        val openAIInterface = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIInterface::class.java)


        sendButton.setOnClickListener {
            val messageString = messageEditText.text.toString()
            val message = ChatMessage("User", messageString)
            if (messageString.isNotEmpty()) {
                messages.add(message)
                adapter.notifyItemInserted(messages.size - 1)
                messageRecyclerView.smoothScrollToPosition(messages.size - 1)
                messageEditText.text.clear()

                val completionRequest = CompletionRequest(
                    prompt = CHAT_BOT_PREFIX + messageString,
                    temperature=0.9,
                    max_tokens=150,
                    top_p=1.0,
                    frequency_penalty=0.0,
                    presence_penalty=0.6,
                    stop=listOf(" Human:", " AI:")
                );
                getCompletions(completionRequest, apiKey, openAIInterface) { result, error ->
                    if (error != null) {
                        // Handle the error
                        return@getCompletions
                    }
                    println("got ")

                    if (result != null) {
                        println(result)
                        val message = ChatMessage("Helper", result.trim())
                        messages.add(message)
                        adapter.notifyItemInserted(messages.size - 1)
                        messageRecyclerView.smoothScrollToPosition(messages.size - 1)

                    } else {
                        // Handle the case where no completion was found
                        val message = ChatMessage("Helper", "Please wait a moment while we try to assist your request..")
                        messages.add(message)
                        adapter.notifyItemInserted(messages.size - 1)
                        messageRecyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                }

            }
        }
    }

    fun getCompletions(completionRequest: CompletionRequest, apiKey: String, openAIInterface : OpenAIInterface, callback: (String?, Throwable?) -> Unit) {
//        val openAIInterface = Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(OpenAIInterface::class.java)

        openAIInterface.getCompletions(apiKey, completionRequest).enqueue(object: Callback<CompletionResponse> {
            override fun onResponse(call: Call<CompletionResponse>, response: Response<CompletionResponse>) {
                if (response.isSuccessful) {
                    println("eeeeee")
                    val completionResponse = response.body()
                    if (completionResponse != null) {
                        val choices = completionResponse.choices
                        if (choices.isNotEmpty()) {
                            callback(choices[0].text, null)
                            return
                        }
                    }
                    callback(null, Exception("No completions found"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when (response.code()) {
                        400 -> "Bad request: $errorBody"
                        401 -> "Unauthorized: $errorBody"
                        403 -> "Forbidden: $errorBody"
                        404 -> "Not found: $errorBody"
                        429 -> "Too many requests: $errorBody"
                        500 -> "Internal server error: $errorBody"
                        503 -> "Service unavailable: $errorBody"
                        else -> "Unknown error: $errorBody"
                    }
                    println(errorMessage)
                    callback(null, Exception(errorMessage))
                }
            }

            override fun onFailure(call: Call<CompletionResponse>, t: Throwable) {
                println("error bitch")
                callback(null, t)
            }
        })
    }
}