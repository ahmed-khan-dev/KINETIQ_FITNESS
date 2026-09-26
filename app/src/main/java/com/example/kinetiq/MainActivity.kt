package com.example.kinetiq

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.kinetiq.databinding.ActivityMainBinding
import com.example.kinetiq.utils.SecurityUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.workoutPlanFragment,
        R.id.logMealFragment,
        R.id.progressFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.lockFragment || destination.id == R.id.onboardingFragment || destination.id == R.id.workoutSessionFragment) {
                binding.bottomNav.visibility = View.GONE
            } else {
                binding.bottomNav.visibility = View.VISIBLE
            }
        }

        setupOnBackPressedHandler()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentId = navController.currentDestination?.id
            if (item.itemId == currentId) {
                return@setOnItemSelectedListener true
            }

            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.homeFragment, inclusive = false, saveState = true)
                .build()

            try {
                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun setupOnBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentId = navController.currentDestination?.id
                if (currentId == R.id.homeFragment || currentId == R.id.lockFragment || currentId == R.id.onboardingFragment) {
                    finish()
                } else if (currentId in topLevelDestinations) {
                    val navOptions = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.homeFragment, inclusive = false)
                        .build()
                    navController.navigate(R.id.homeFragment, null, navOptions)
                    binding.bottomNav.selectedItemId = R.id.homeFragment
                } else {
                    if (!navController.popBackStack()) {
                        finish()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        checkForegroundSessionExpiry()
    }

    private fun checkForegroundSessionExpiry() {
        val app = application as KinetiqApplication
        lifecycleScope.launch {
            val session = app.repository.getAppSession()
            if (session != null && session.isAuthenticated) {
                if (SecurityUtils.isSessionExpired(session.lastActiveTimestamp, session.sessionExpiryTimestamp)) {
                    app.repository.invalidateSession()
                    if (navController.currentDestination?.id != R.id.lockFragment) {
                        navController.navigate(R.id.lockFragment)
                    }
                } else {
                    app.repository.updateSessionSuccess()
                }
            }
        }
    }
}