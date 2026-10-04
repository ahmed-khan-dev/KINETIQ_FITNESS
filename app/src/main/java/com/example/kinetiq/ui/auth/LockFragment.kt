package com.example.kinetiq.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kinetiq.R
import com.example.kinetiq.databinding.FragmentLockBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LockViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBiometricUnlock.setOnClickListener {
            showBiometricPrompt()
        }

        binding.btnSubmitPin.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            if (pin.length != 4 || pin.any { it !in '0'..'9' }) {
                binding.tilPin.error = "Enter exactly 4 digits."
                binding.etPin.requestFocus()
                return@setOnClickListener
            }
            binding.tilPin.error = null
            viewModel.submitPin(pin)
        }

        setupKeypadListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lockState.collectLatest { state ->
                handleLockState(state)
            }
        }

        viewModel.checkSession()
    }

    private fun setupKeypadListeners() {
        val numberButtons = mapOf(
            binding.btnKey0 to "0",
            binding.btnKey1 to "1",
            binding.btnKey2 to "2",
            binding.btnKey3 to "3",
            binding.btnKey4 to "4",
            binding.btnKey5 to "5",
            binding.btnKey6 to "6",
            binding.btnKey7 to "7",
            binding.btnKey8 to "8",
            binding.btnKey9 to "9"
        )

        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                val currentPin = binding.etPin.text.toString()
                if (currentPin.length < 4) {
                    val updatedPin = currentPin + digit
                    binding.etPin.setText(updatedPin)
                    updatePinDots(updatedPin.length)
                }
            }
        }

        binding.btnKeyBackspace.setOnClickListener {
            val currentPin = binding.etPin.text.toString()
            if (currentPin.isNotEmpty()) {
                val updatedPin = currentPin.dropLast(1)
                binding.etPin.setText(updatedPin)
                updatePinDots(updatedPin.length)
            }
        }

        binding.etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                updatePinDots(len)
                binding.tilPin.error = null
                binding.tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun updatePinDots(pinLength: Int) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.kinetic_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.kinetic_card_highest)

        binding.pinDot1.backgroundTintList = android.content.res.ColorStateList.valueOf(if (pinLength >= 1) activeColor else inactiveColor)
        binding.pinDot2.backgroundTintList = android.content.res.ColorStateList.valueOf(if (pinLength >= 2) activeColor else inactiveColor)
        binding.pinDot3.backgroundTintList = android.content.res.ColorStateList.valueOf(if (pinLength >= 3) activeColor else inactiveColor)
        binding.pinDot4.backgroundTintList = android.content.res.ColorStateList.valueOf(if (pinLength >= 4) activeColor else inactiveColor)
    }

    private fun handleLockState(state: LockState) {
        when (state) {
            LockState.Idle, LockState.CheckingSession -> {
                binding.pbCheckingSession.visibility = View.VISIBLE
                binding.authContainer.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            }
            LockState.AutoBypassed, LockState.Authenticated -> {
                binding.pbCheckingSession.visibility = View.GONE
                binding.authContainer.visibility = View.GONE
                navigateToHome()
            }
            is LockState.PinRequired -> {
                binding.pbCheckingSession.visibility = View.GONE
                binding.authContainer.visibility = View.VISIBLE
                if (!state.hasPin) {
                    binding.tilPin.hint = "Set New 4-Digit PIN"
                } else {
                    binding.tilPin.hint = "Enter 4-Digit PIN"
                }
                if (isBiometricAvailable()) {
                    showBiometricPrompt()
                }
            }
            is LockState.Error -> {
                binding.pbCheckingSession.visibility = View.GONE
                binding.authContainer.visibility = View.VISIBLE
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(requireContext())
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        if (!isBiometricAvailable()) {
            Toast.makeText(requireContext(), "Biometric unlock not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.onBiometricSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    binding.tvError.text = errString.toString()
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Kinetiq Unlock")
            .setSubtitle("Authenticate using your fingerprint or face")
            .setNegativeButtonText("Use PIN")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToHome() {
        if (findNavController().currentDestination?.id == R.id.lockFragment) {
            lifecycleScope.launch {
                val app = requireActivity().application as com.example.kinetiq.KinetiqApplication
                val profile = app.repository.getProfile()
                if (profile == null) {
                    findNavController().navigate(R.id.action_lockFragment_to_onboardingFragment)
                } else {
                    findNavController().navigate(R.id.action_lockFragment_to_homeFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}