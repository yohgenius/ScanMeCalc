package com.example.textrecognitiontest

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.textrecognitiontest.Constants.IMAGE_PICKER_REQUEST_CODE
import com.example.textrecognitiontest.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.theartofdev.edmodo.cropper.CropImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestAllPermissions()

        if (BuildConfig.FLAVOR_THEME == "green") {
            binding.btnHitung.setBackgroundColor(resources.getColor(R.color.green_dark, null))
            binding.buttonPickImage.setBackgroundColor(resources.getColor(R.color.green_dark, null))
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.green_dark)))
            window.statusBarColor = resources.getColor(R.color.green_dark, null)
            binding.imagePlaceholder.setBackgroundColor(resources.getColor(R.color.green_dark, null))
        } else {
            binding.btnHitung.setBackgroundColor(resources.getColor(R.color.red, null))
            binding.buttonPickImage.setBackgroundColor(resources.getColor(R.color.red, null))
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.red)))
            window.statusBarColor = resources.getColor(R.color.red, null)
            binding.imagePlaceholder.setBackgroundColor(resources.getColor(R.color.red, null))
        }

        if (BuildConfig.FLAVOR_TYPE == "camera"){
            binding.btnHitung.visibility = View.VISIBLE
            binding.buttonPickImage.visibility = View.GONE
            binding.imagePlaceholder.visibility = View.GONE
            binding.textBoxOverlay.visibility = View.VISIBLE
            binding.camPreview.visibility = View.VISIBLE
        }else{
            binding.btnHitung.visibility = View.GONE
            binding.buttonPickImage.visibility = View.VISIBLE
            binding.imagePlaceholder.visibility = View.VISIBLE
            binding.textBoxOverlay.visibility = View.GONE
            binding.camPreview.visibility = View.GONE
        }


        binding.buttonPickImage.setOnClickListener {
            if (checkPermission()) {
                openImagePicker()
            } else {
                Toast.makeText(
                    this,
                    "App does not have permission to access your phone storage!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    }

    private fun requestAllPermissions() {
        if (checkPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSION,
                Constants.CAMERA_REQUEST_CODE_PERMISSION
            )
        }
    }

    private fun checkPermission() =
        Constants.REQUIRED_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                baseContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.camPreview.surfaceProvider)
                }

            val textAnalyzer = TextAnalyzer()
            textAnalyzer.context = this@MainActivity
            textAnalyzer.binding = binding

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, textAnalyzer)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraFacing,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.CAMERA_REQUEST_CODE_PERMISSION) {
            if (checkPermission()) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show()
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "App does not have permission to access camera!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                try {
                    // start cropping activity for pre-acquired image saved on the device
                    CropImage.activity(selectedImageUri)
                        .start(this);
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode === RESULT_OK) {
                val resultUri = result.uri
                scanTextFromImage(resultUri)
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    private fun scanTextFromImage(resultUri: Uri?) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, resultUri)
        val image = InputImage.fromBitmap(bitmap, 0)
        val textAnalyzer = TextAnalyzer()
        binding.imagePlaceholder.setImageBitmap(bitmap)

        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        textRecognizer.process(image)
            .addOnSuccessListener {
                val listTextBlock = it.textBlocks
                val textLines = mutableListOf<String>()
                for (block in listTextBlock) {
                    for (line in block.lines) {
                        textLines.add(line.text)
                    }
                }
                var finalText = ""
                if (textLines[0].isNotEmpty()) {
                    finalText = textLines[0].removeSpace()
                }
                if (textAnalyzer.isInputValid(finalText)) {
                    binding.inputTextView.text = finalText
                    binding.resultTextLabelView.text = textAnalyzer.getOperationResult(finalText)
                } else {
                    binding.inputTextView.text = "No result"
                    binding.resultTextLabelView.text = "No result"
                }

            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
    }

    companion object {
        var isBackCam = true
    }
}