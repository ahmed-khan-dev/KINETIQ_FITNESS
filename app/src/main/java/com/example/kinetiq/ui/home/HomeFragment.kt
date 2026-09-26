package com.example.kinetiq.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kinetiq.R
import com.example.kinetiq.data.local.entity.ProgressPhotoEntity
import com.example.kinetiq.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnHomeWorkout.setOnClickListener { findNavController().navigate(R.id.workoutPlanFragment) }
        binding.btnHomeMeal.setOnClickListener { findNavController().navigate(R.id.logMealFragment) }
        binding.btnHomeProgress.setOnClickListener { findNavController().navigate(R.id.progressFragment) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest(::render)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) viewModel.refresh()
    }

    private fun render(state: HomeUiState) {
        binding.tvHomeDate.text = state.dateLabel.ifBlank { "Today's summary" }
        binding.pbHomeLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.tvHomeError.text = state.errorMessage.orEmpty()
        binding.tvHomeError.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.tvHomeWorkoutTitle.text = state.workoutTitle
        binding.tvHomeWorkoutMeta.text = state.workoutMeta
        binding.tvHomeWorkoutStatus.text = state.workoutStatus
        binding.tvHomeWorkoutExercises.text = state.workoutExercises
        binding.tvHomeWorkoutExercises.visibility = if (state.workoutExercises.isBlank()) View.GONE else View.VISIBLE
        binding.btnHomeWorkout.text = if (state.hasWorkout) "Open Workout" else "Create Workout Plan"

        binding.tvHomeMealSummary.text = state.mealSummary
        binding.tvHomeMealDetails.text = state.mealDetails
        binding.tvHomeMealDetails.visibility = if (state.mealDetails.isBlank()) View.GONE else View.VISIBLE

        binding.tvHomeStreak.text = "${state.currentStreak} ${if (state.currentStreak == 1) "day" else "days"}"
        binding.tvHomeLongestStreak.text = "Longest: ${state.longestStreak} ${if (state.longestStreak == 1) "day" else "days"}"
        binding.tvHomeGps.text = state.gpsSummary
        renderLatestPhoto(state.latestPhoto)
    }

    private fun renderLatestPhoto(photo: ProgressPhotoEntity?) {
        if (photo == null) {
            binding.ivHomeProgressPhoto.setImageDrawable(null)
            binding.ivHomeProgressPhoto.visibility = View.GONE
            binding.tvHomePhotoEmpty.text = "No progress photo saved yet."
            binding.tvHomePhotoEmpty.visibility = View.VISIBLE
            binding.tvHomePhotoMeta.text = ""
            return
        }

        val file = File(photo.photoUrl)
        if (file.isFile) {
            binding.ivHomeProgressPhoto.setImageBitmap(loadPreview(file))
            binding.ivHomeProgressPhoto.visibility = View.VISIBLE
            binding.tvHomePhotoEmpty.visibility = View.GONE
        } else {
            binding.ivHomeProgressPhoto.setImageDrawable(null)
            binding.ivHomeProgressPhoto.visibility = View.GONE
            binding.tvHomePhotoEmpty.text = "The saved photo file is unavailable."
            binding.tvHomePhotoEmpty.visibility = View.VISIBLE
        }
        val takenAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(photo.takenAt))
        binding.tvHomePhotoMeta.text = "${photo.angle} view · $takenAt"
    }

    private fun loadPreview(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
