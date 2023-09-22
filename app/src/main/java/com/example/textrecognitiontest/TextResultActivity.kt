package com.example.textrecognitiontest

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.example.textrecognitiontest.databinding.ActivityTestResultBinding
import java.util.*

class TextResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestResultBinding
    private var textResult: String? = null
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                // Set the language for text-to-speech
                textToSpeech.language = Locale.US
            }
        }

        textResult = intent.getStringExtra("result")

        if (textResult != null)
            binding.textResult.text = textResult

        binding.buttonSpeaker.setOnClickListener {
            if (textResult != null) {
                textToSpeech.speak(textResult, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown the Text-to-Speech engine
        textToSpeech.shutdown()
    }
}