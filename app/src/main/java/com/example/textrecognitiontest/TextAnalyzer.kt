package com.example.textrecognitiontest

import android.content.Context
import android.content.Intent
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
//                        val text = getText(listTextBlock)
                        val textLines = mutableListOf<String>()
                        println("rrrr")
                        for (block in listTextBlock) {
                            for (line in block.lines) {
                                textLines.add(line.text)
                            }
                        }
//                        val intent = Intent(context, TextResultActivity::class.java)
//                        intent.putExtra("result", text)
                        validateLines(textLines)
//                        context?.startActivity(intent)
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

    companion object {
        fun getText(listBlock: List<Text.TextBlock>): String {
            var textResult = ""
            for (block in listBlock) {
                textResult += "\t\t"
                for (line in block.lines) {
                    textResult += line.text
                }
                textResult += "\n\n"
            }

            return textResult
        }
    }

    fun validateLines(textLines: List<String>) {
        MainScope().launch(Dispatchers.IO) {
            textLines.forEach { textLine ->
                print("rrrr $textLine")
                if (isInputValid(textLine.removeSpace())) {
                    _homeUiState.update { uiState ->
                        uiState.copy(
                            textResult = TextResult.Success(
                                input = textLine,
                                result = getOperationResult(textLine)
                            ),
                        )
                    }
                    print("rrrr x ${getOperationResult(textLine)}")
                    return@launch
                }
                print("rrrr asdasd")
            }
            _homeUiState.update { uiState ->
                uiState.copy(textResult = TextResult.NoResultFound)
            }
        }
    }

    private fun isInputValid(input: String): Boolean {
        val splitInput = input.split('/', '*', '+', '-')
        return splitInput.size == 2 && splitInput.all { it.isDigit() && it.isNotEmpty() }
    }

    private fun getOperationResult(equation: String): String {
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