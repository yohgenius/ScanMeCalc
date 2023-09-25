package com.example.textrecognitiontest

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.textrecognitiontest.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@ExperimentalGetImage
class TextAnalyzer : ImageAnalysis.Analyzer {
    var context: Context? = null
    var binding: ActivityMainBinding? = null
    private val _homeUiState = MutableStateFlow(HomeUiState())

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = textRecognizer.process(image)
                .addOnSuccessListener {
                    val listTextBlock = it.textBlocks
                    binding?.textBoxOverlay?.setTextBlocks(
                        listTextBlock,
                        imageProxy.image!!.width.toFloat(),
                        imageProxy.image!!.height.toFloat(),
                        MainActivity.isBackCam
                    )

                    binding?.btnHitung?.setOnClickListener {
                        val textLines = mutableListOf<String>()
                        for (block in listTextBlock) {
                            for (line in block.lines) {
                                textLines.add(line.text)
                            }
                        }
                        print("rrrrx $textLines\n")
                        validateLines(textLines)
                        analyzeResult()

                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    fun analyzeResult() {
        MainScope().launch {
            binding?.inputTextView?.text = _homeUiState.collect { uiState ->
                when (uiState.textResult) {
                    TextResult.InitialState -> {
                        binding?.inputTextView?.text = ""
                        binding?.resultTextLabelView?.text = ""
                    }
                    is TextResult.Success -> {
                        binding?.inputTextView?.text = uiState.textResult.input
                        binding?.resultTextLabelView?.text =
                            uiState.textResult.result

                    }
                    TextResult.NoResultFound -> {
                        binding?.inputTextView?.text = "No result"
                        binding?.resultTextLabelView?.text = "No result"
                    }
                }
            }

        }

    }



    fun validateLines(textLines: List<String>) {
        MainScope().launch(Dispatchers.IO) {
            val finalText = textLines[0].removeSpace()
            finalText.forEach { textLine ->
                if (isInputValid(finalText)) {
                    _homeUiState.update { uiState ->
                        print("rrrr ui $uiState")
                        uiState.copy(
                            textResult = TextResult.Success(
                                input = finalText,
                                result = getOperationResult(finalText)
                            ),
                        )
                    }
                    print("rrrr x ${getOperationResult(finalText)}")
                    return@launch
                }
            }
            _homeUiState.update { uiState ->
                uiState.copy(textResult = TextResult.NoResultFound)
            }
        }
    }

    fun isInputValid(input: String): Boolean {
        val splitInput = input.split('/', '*', '+', '-')
        return splitInput.size == 2 && splitInput.all { it.isDigit() && it.isNotEmpty() }
    }

    fun getOperationResult(equation: String): String {
        val operands = equation.split('/', '*', '+', '-').map { it.toInt() }
        return when {
            equation.contains("+") -> {
                (operands[0] + operands[1]).toString()
            }
            equation.contains("-") -> {
                (operands[0] - operands[1]).toString()
            }
            equation.contains("*") -> {
                (operands[0] * operands[1]).toString()
            }
            equation.contains("/") -> {
                (operands[0] / operands[1]).toString()
            }
            else -> "Unsupported operation"
        }
    }


    data class HomeUiState(
        val textResult: TextResult = TextResult.InitialState
    )
}