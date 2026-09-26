package com.example.kinetiq.ui.meal

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

/** Keeps CameraX lifecycle and capture details out of the fragments. */
class PhotoCameraController(
    context: Context,
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner
) {
    private val appContext = context.applicationContext
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    fun start(onError: (String) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
                cameraProvider = provider
                imageCapture = capture
            } catch (error: Exception) {
                onError(error.localizedMessage ?: "Could not start the camera.")
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun capture(outputFile: File, onSaved: (String) -> Unit, onFailure: (String) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onFailure("Camera is still starting. Try again in a moment.")
            return
        }

        val output = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            output,
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    onSaved(outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    outputFile.delete()
                    onFailure(exception.localizedMessage ?: "Could not save the photo.")
                }
            }
        )
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
    }
}
