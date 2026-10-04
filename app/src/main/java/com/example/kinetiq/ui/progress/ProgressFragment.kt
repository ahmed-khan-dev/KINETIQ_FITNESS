package com.example.kinetiq.ui.progress

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.kinetiq.R
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.data.local.entity.WeightLogEntity
import com.example.kinetiq.databinding.FragmentProgressBinding
import com.example.kinetiq.ui.meal.PhotoCameraController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProgressViewModel by viewModels()
    private var cameraController: PhotoCameraController? = null
    private var pendingLocationAction: (() -> Unit)? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCameraPreview() else viewModel.showMessage("Camera permission is needed to capture a progress photo.")
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val destinationFile = viewModel.createPhotoFile()
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setCapturedPhoto(destinationFile.absolutePath)
            } catch (e: Exception) {
                viewModel.showMessage("Could not import photo from gallery.")
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pendingLocationAction?.invoke()
        pendingLocationAction = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraController = PhotoCameraController(requireContext(), binding.progressCameraPreview, viewLifecycleOwner)
        binding.spinnerProgressAngle.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, listOf("Front", "Side", "Back")
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.btnCaptureProgressPhoto.setOnClickListener {
            if (hasCameraPermission()) openCameraPreview()
            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnChooseProgressGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnRetakeProgressPhoto.setOnClickListener {
            binding.ivProgressPhoto.visibility = View.GONE
            viewModel.clearCapturedPhoto()
            openCameraPreview()
        }

        binding.btnSaveWeight.setOnClickListener {
            val weight = binding.etProgressWeight.text.toString()
            val parsedWeight = weight.trim().toDoubleOrNull()
            binding.etProgressWeight.error = if (parsedWeight == null || !parsedWeight.isFinite() || parsedWeight <= 0.0 || parsedWeight > 500.0) {
                "Enter a weight above 0 and no more than 500 kg."
            } else null
            if (binding.etProgressWeight.error != null) {
                binding.etProgressWeight.requestFocus()
                return@setOnClickListener
            }
            withLocationPermission { viewModel.saveWeight(weight) }
        }

        binding.etProgressWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { binding.etProgressWeight.error = null }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnSaveProgressPhoto.setOnClickListener {
            val angle = binding.spinnerProgressAngle.selectedItem?.toString().orEmpty()
            withLocationPermission { viewModel.saveProgressPhoto(angle) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest(::render)
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun withLocationPermission(action: () -> Unit) {
        if (hasLocationPermission()) action() else {
            pendingLocationAction = action
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun openCameraPreview() {
        val currentBinding = _binding ?: return
        currentBinding.tvProgressCameraPrompt.visibility = View.GONE
        currentBinding.ivProgressPhoto.visibility = View.GONE
        currentBinding.progressCameraPreview.visibility = View.VISIBLE
        currentBinding.btnRetakeProgressPhoto.visibility = View.GONE
        currentBinding.btnCaptureProgressPhoto.text = "Capture Photo"
        currentBinding.btnCaptureProgressPhoto.setOnClickListener { captureProgressPhoto() }
        cameraController?.start(viewModel::showMessage)
    }

    private fun captureProgressPhoto() {
        val output = try {
            viewModel.createPhotoFile()
        } catch (error: Exception) {
            viewModel.showMessage(error.localizedMessage ?: "Could not create a progress photo file.")
            return
        }
        cameraController?.capture(output, viewModel::setCapturedPhoto, viewModel::showMessage)
    }

    private fun render(state: ProgressUiState) {
        binding.pbSavingProgress.visibility = if (state.isSaving) View.VISIBLE else View.GONE
        binding.btnSaveWeight.isEnabled = !state.isSaving
        binding.btnSaveProgressPhoto.isEnabled = !state.isSaving
        binding.tvProgressMessage.text = state.message.orEmpty()
        binding.tvProgressMessage.visibility = if (state.message.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.weightTrajectoryGraph.setWeightLogs(state.weightLogs)

        state.capturedPhotoPath?.let { path ->
            val file = File(path)
            if (file.isFile) {
                binding.ivProgressPhoto.setImageBitmap(loadBitmap(file, 1024))
                binding.ivProgressPhoto.visibility = View.VISIBLE
                binding.progressCameraPreview.visibility = View.GONE
                binding.tvProgressCameraPrompt.visibility = View.GONE
                binding.btnRetakeProgressPhoto.visibility = View.VISIBLE
                binding.btnCaptureProgressPhoto.text = "Capture Another Photo"
                binding.btnCaptureProgressPhoto.setOnClickListener {
                    if (hasCameraPermission()) captureProgressPhoto()
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
        renderWeightHistory(state.weightLogs)
        renderProgressPhotos(state.progressPhotos)
    }

    private fun renderWeightHistory(logs: List<WeightLogEntity>) {
        binding.weightHistoryContainer.removeAllViews()
        if (logs.isEmpty()) {
            binding.weightHistoryContainer.addView(TextView(requireContext()).apply {
                text = "No weight entries yet."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_muted))
            })
            return
        }
        logs.forEach { log ->
            binding.weightHistoryContainer.addView(TextView(requireContext()).apply {
                text = "${formatWeight(log.weightKg)} kg  •  ${formatDate(log.loggedAt)}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_primary))
                setPadding(0, dp(8), 0, dp(8))
            })
        }
    }

    private fun renderProgressPhotos(photos: List<ProgressPhotoEntity>) {
        binding.progressPhotosContainer.removeAllViews()
        if (photos.isEmpty()) {
            binding.progressPhotosContainer.addView(TextView(requireContext()).apply {
                text = "No progress photos yet."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_muted))
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }
        photos.forEach { photo ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, dp(10))
            }
            val image = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(88), dp(88))
                contentDescription = "${photo.angle} progress photo"
                scaleType = ImageView.ScaleType.CENTER_CROP
                val file = File(photo.photoUrl)
                if (file.isFile) setImageBitmap(loadBitmap(file, 512))
            }
            val details = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
                text = "${photo.angle} view\n${formatDate(photo.takenAt)}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_primary))
            }
            row.addView(image)
            row.addView(details)
            binding.progressPhotosContainer.addView(row)
        }
    }

    private fun loadBitmap(file: File, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun formatWeight(weight: Double) = if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(weight)
    private fun formatDate(timestamp: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        cameraController?.stop()
        cameraController = null
        super.onDestroyView()
        _binding = null
    }
}