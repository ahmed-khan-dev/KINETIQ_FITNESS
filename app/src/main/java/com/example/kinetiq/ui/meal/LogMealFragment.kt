package com.example.kinetiq.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.kinetiq.R
import com.example.kinetiq.data.local.entity.MealLogEntity
import com.example.kinetiq.databinding.FragmentLogMealBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

class LogMealFragment : Fragment() {

    private var _binding: FragmentLogMealBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MealViewModel by viewModels()
    private var cameraController: PhotoCameraController? = null
    private var pendingMealForm: MealForm? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openCameraPreview()
        } else {
            viewModel.showMessage("Camera permission is needed to capture a meal photo.")
        }
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
        pendingMealForm?.let(viewModel::saveMeal)
        pendingMealForm = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraController = PhotoCameraController(requireContext(), binding.mealCameraPreview, viewLifecycleOwner)

        binding.spinnerMealSlot.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Breakfast", "Lunch", "Dinner", "Snack")
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.btnCaptureMealPhoto.setOnClickListener {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCameraPermission) openCameraPreview()
            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnChooseMealGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnRetakeMealPhoto.setOnClickListener {
            binding.ivMealPhoto.visibility = View.GONE
            viewModel.clearCapturedPhoto()
            openCameraPreview()
        }

        binding.btnSaveMeal.setOnClickListener {
            val form = currentMealForm()
            if (!validateMealForm(form)) return@setOnClickListener
            val hasLocationPermission = hasLocationPermission()
            if (hasLocationPermission) {
                viewModel.saveMeal(form)
            } else {
                pendingMealForm = form
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }

        listOf(binding.etMealName, binding.etMealCalories, binding.etMealProtein, binding.etMealCarbs, binding.etMealFat).forEach { field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { field.error = null }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest(::render)
        }
    }

    private fun currentMealForm() = MealForm(
        name = binding.etMealName.text.toString(),
        slot = binding.spinnerMealSlot.selectedItem?.toString().orEmpty(),
        calories = binding.etMealCalories.text.toString(),
        proteinG = binding.etMealProtein.text.toString(),
        carbsG = binding.etMealCarbs.text.toString(),
        fatG = binding.etMealFat.text.toString()
    )

    private fun validateMealForm(form: MealForm): Boolean {
        val name = form.name.trim()
        val calories = form.calories.trim().toIntOrNull()
        val protein = optionalMacro(form.proteinG)
        val carbs = optionalMacro(form.carbsG)
        val fat = optionalMacro(form.fatG)

        binding.etMealName.error = when {
            name.isEmpty() -> "Enter a meal name."
            name.length > 100 -> "Use 100 characters or fewer."
            else -> null
        }
        binding.etMealCalories.error = when {
            calories == null -> "Enter calories as a whole number."
            calories !in 0..10000 -> "Calories must be from 0 to 10,000."
            else -> null
        }
        binding.etMealProtein.error = macroError(form.proteinG, protein, "Protein")
        binding.etMealCarbs.error = macroError(form.carbsG, carbs, "Carbohydrates")
        binding.etMealFat.error = macroError(form.fatG, fat, "Fat")

        val invalidField = listOf(
            binding.etMealName, binding.etMealCalories, binding.etMealProtein, binding.etMealCarbs, binding.etMealFat
        ).firstOrNull { it.error != null }
        invalidField?.requestFocus()
        return invalidField == null
    }

    private fun optionalMacro(value: String): Double? = if (value.isBlank()) 0.0 else value.trim().toDoubleOrNull()

    private fun macroError(raw: String, value: Double?, label: String): String? {
        if (raw.isBlank()) return null
        return if (value == null || !value.isFinite() || value < 0.0 || value > 1000.0) {
            "$label must be from 0 to 1,000 g, or leave it blank."
        } else null
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun openCameraPreview() {
        val currentBinding = _binding ?: return
        currentBinding.tvCameraPrompt.visibility = View.GONE
        currentBinding.ivMealPhoto.visibility = View.GONE
        currentBinding.mealCameraPreview.visibility = View.VISIBLE
        currentBinding.btnRetakeMealPhoto.visibility = View.GONE
        currentBinding.btnCaptureMealPhoto.text = "Capture Photo"
        cameraController?.start { message -> viewModel.showMessage(message) }
        currentBinding.btnCaptureMealPhoto.setOnClickListener {
            captureMealPhoto()
        }
    }

    private fun captureMealPhoto() {
        val outputFile = try {
            viewModel.createPhotoFile()
        } catch (error: Exception) {
            viewModel.showMessage(error.localizedMessage ?: "Could not create a meal photo file.")
            return
        }
        cameraController?.capture(
            outputFile = outputFile,
            onSaved = viewModel::setCapturedPhoto,
            onFailure = viewModel::showMessage
        )
    }

    private fun render(state: MealUiState) {
        binding.pbSavingMeal.visibility = if (state.isSaving) View.VISIBLE else View.GONE
        binding.btnSaveMeal.isEnabled = !state.isSaving
        binding.tvMealMessage.text = state.message.orEmpty()
        binding.tvMealMessage.visibility = if (state.message.isNullOrBlank()) View.GONE else View.VISIBLE

        state.capturedPhotoPath?.let { path ->
            val imageFile = File(path)
            if (imageFile.isFile) {
                binding.ivMealPhoto.setImageBitmap(loadPreviewBitmap(imageFile))
                binding.ivMealPhoto.visibility = View.VISIBLE
                binding.mealCameraPreview.visibility = View.GONE
                binding.tvCameraPrompt.visibility = View.GONE
                binding.btnRetakeMealPhoto.visibility = View.VISIBLE
                binding.btnCaptureMealPhoto.text = "Capture Another Photo"
                binding.btnCaptureMealPhoto.setOnClickListener {
                    if (hasCameraPermission()) captureMealPhoto()
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
        renderRecentMeals(state.recentMeals)
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun loadPreviewBitmap(file: File) = BitmapFactory.decodeFile(file.absolutePath)

    private fun renderRecentMeals(meals: List<MealLogEntity>) {
        binding.recentMealsContainer.removeAllViews()
        if (meals.isEmpty()) {
            binding.recentMealsContainer.addView(TextView(requireContext()).apply {
                text = "No meals logged yet."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_muted))
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }

        meals.forEach { meal ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, dp(10))
                gravity = Gravity.CENTER_VERTICAL
            }
            val photo = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
                contentDescription = "${meal.mealName} photo"
                scaleType = ImageView.ScaleType.CENTER_CROP
                meal.photoUrl?.let { path ->
                    val file = File(path)
                    if (file.isFile) setImageBitmap(loadThumbnail(file))
                }
            }
            val details = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12)
                }
                text = "${meal.mealName} · ${meal.mealSlot}\n${meal.calories} kcal · P ${meal.proteinG.toInt()}g · C ${meal.carbsG.toInt()}g · F ${meal.fatG.toInt()}g\n${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(meal.loggedAt))}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_primary))
                textSize = 13f
            }

            val overflowBtn = TextView(requireContext()).apply {
                text = "⋮"
                textSize = 22f
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setTextColor(ContextCompat.getColor(requireContext(), R.color.kinetic_text_muted))
                setOnClickListener { anchor ->
                    val popup = PopupMenu(requireContext(), anchor)
                    popup.menu.add("Delete Meal")
                    popup.setOnMenuItemClickListener { item ->
                        if (item.title == "Delete Meal") {
                            viewModel.deleteMeal(meal)
                        }
                        true
                    }
                    popup.show()
                }
            }

            row.addView(photo)
            row.addView(details)
            row.addView(overflowBtn)
            binding.recentMealsContainer.addView(row)
        }
    }

    private fun loadThumbnail(file: File) : android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = if (bounds.outWidth > 512 || bounds.outHeight > 512) 4 else 1
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        cameraController?.stop()
        cameraController = null
        super.onDestroyView()
        _binding = null
    }
}