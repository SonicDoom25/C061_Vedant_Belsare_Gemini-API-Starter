package com.c061_vedant_belsare.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_RECORD_AUDIO = 100;
    private static final int REQ_SPEECH = 101;

    private EditText messageEditText;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;

    private ChatDatabase chatDatabase;
    private ChatMessageDao chatMessageDao;
    private ExecutorService executorService;

    private List<ChatMessage> chatList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        messageEditText = findViewById(R.id.messageEditText);
        Button sendButton = findViewById(R.id.sendButton);
        ImageButton micButton = findViewById(R.id.micButton);
        Button darkModeButton = findViewById(R.id.darkModeButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        // RecyclerView setup
        chatAdapter = new ChatAdapter(chatList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Room DB
        chatDatabase = ChatDatabase.getInstance(this);
        chatMessageDao = chatDatabase.chatMessageDao();
        executorService = Executors.newSingleThreadExecutor();

        // Load existing messages
        executorService.execute(() -> {
            List<ChatMessage> messages = chatMessageDao.getAllMessages();
            runOnUiThread(() -> {
                chatList.addAll(messages);
                chatAdapter.notifyDataSetChanged();
            });
        });

        // Gemini Model
        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-2.0-flash",
                BuildConfig.API_KEY
        );

        // Send Button
        sendButton.setOnClickListener(v -> {
            String prompt = messageEditText.getText().toString().trim();
            messageEditText.setText("");
            if (prompt.isEmpty()) {
                messageEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            ChatMessage userMessage = new ChatMessage("user", prompt);
            addMessageToUI(userMessage);
            saveMessageToDb(userMessage);

            progressBar.setVisibility(VISIBLE);

            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String botReply = response.getText() != null ? response.getText() : "Error";

                    ChatMessage botMessage = new ChatMessage("bot", botReply);
                    runOnUiThread(() -> {
                        addMessageToUI(botMessage);
                        progressBar.setVisibility(GONE);
                    });
                    saveMessageToDb(botMessage);
                }
            });
        });

        // Mic Button → starts speech recognition
        micButton.setOnClickListener(v -> checkAudioPermission());

        // Dark Mode toggle
        darkModeButton.setOnClickListener(v -> {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeButton.setText("Dark Mode");
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                darkModeButton.setText("Light Mode");
            }
        });
    }

    private void addMessageToUI(ChatMessage message) {
        chatList.add(message);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        chatRecyclerView.scrollToPosition(chatList.size() - 1);
    }

    private void saveMessageToDb(ChatMessage message) {
        executorService.execute(() -> chatMessageDao.insert(message));
    }

    // --- Speech-to-text helpers ---

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
        } else {
            startSpeechInput();
        }
    }

    private void startSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        startActivityForResult(intent, REQ_SPEECH);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechInput();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                messageEditText.setText(result.get(0)); // place recognized text into EditText
            }
        }
    }
}
