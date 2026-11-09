package com.nova.ai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val OVERLAY_REQ_CODE = 1000
    private val VOICE_REQ_CODE = 2000

    private lateinit var chatContainer: LinearLayout
    private lateinit var inputPrompt: EditText
    private lateinit var scrollChat: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatContainer = findViewById(R.id.chatContainer)
        inputPrompt = findViewById(R.id.inputPrompt)
        scrollChat = findViewById(R.id.scrollChat)

        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnVoice = findViewById<ImageButton>(R.id.btnVoiceMain)

        btnSend.setOnClickListener {
            val text = inputPrompt.text.toString().trim()
            if (text.isNotEmpty()) sendChatMessage(text)
        }

        btnVoice.setOnClickListener {
            startVoiceRecognition()
        }

        checkPermissions()
    }

    private fun sendChatMessage(userText: String) {
        addBubble("Anda", userText, "#343541")
        inputPrompt.setText("")

        CoroutineScope(Dispatchers.Main).launch {
            val response = GeminiClient.sendMessage(userText)
            addBubble("Nova", response, "#444654")
        }
    }

    private fun addBubble(sender: String, message: String, bgColor: String) {
        val textView = TextView(this).apply {
            text = "$sender:\n$message"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setPadding(24, 18, 24, 18)
            setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 8, 0, 8) }

        chatContainer.addView(textView, params)
        scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        startActivityForResult(intent, VOICE_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode: Int, resultCode, data)
        if (requestCode == VOICE_REQ_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                sendChatMessage(spokenText)
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_REQ_CODE)
        } else {
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingNovaService::class.java)
        startService(intent)
    }
}<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.nova.ai">

    <!-- Izin Utama -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <!-- Izin untuk mengecek apakah sedang di home screen -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Nova AI"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">
        
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service android:name=".FloatingNovaService" android:enabled="true" android:exported="false" />
    </application>
</manifest>
