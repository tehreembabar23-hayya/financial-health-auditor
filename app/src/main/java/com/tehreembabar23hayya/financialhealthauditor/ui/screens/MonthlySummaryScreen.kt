package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehreembabar23hayya.financialhealthauditor.BuildConfig
import com.tehreembabar23hayya.financialhealthauditor.data.AppDatabase
import com.tehreembabar23hayya.financialhealthauditor.utils.SyntheticDataGenerator
import com.tehreembabar23hayya.financialhealthauditor.detection.DuplicateSubscriptionDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Lightweight ViewModel — only loaded in the Debug flavour guard below
// ---------------------------------------------------------------------------
class MonthlySummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    fun generateSyntheticData() {
        viewModelScope.launch {
            _isGenerating.value = true
            SyntheticDataGenerator.generate(dao)
            DuplicateSubscriptionDetector.detectAndFlag(dao, getApplication())
            _isGenerating.value = false
            _isDone.value = true
        }
    }

    fun resetDone() { _isDone.value = false }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Monthly Financial Audit Report",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "View health scores, risk forecasts, and monthly stats.",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(16.dp)
            )

            // ----------------------------------------------------------------
            // DEBUG-ONLY: Generate Synthetic Data button
            // Stripped entirely from release builds via BuildConfig.DEBUG guard.
            // ----------------------------------------------------------------
            if (BuildConfig.DEBUG) {
                val vm: MonthlySummaryViewModel = viewModel()
                val isGenerating by vm.isGenerating.collectAsState()
                val isDone by vm.isDone.collectAsState()

                Spacer(modifier = Modifier.height(24.dp))

                if (isDone) {
                    Text(
                        text = "✓ Synthetic data inserted!",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { vm.resetDone() }) { Text("Dismiss") }
                }

                Button(
                    onClick = { vm.generateSyntheticData() },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("⚙ Generate Synthetic Data [DEBUG]")
                }
            }
        }
    }
}

