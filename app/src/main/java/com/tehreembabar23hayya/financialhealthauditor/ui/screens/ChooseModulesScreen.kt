package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val BrassColor = Color(0xFFC5A059)

private data class ModuleInfo(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val modulesList = listOf(
    ModuleInfo(
        id = 1,
        title = "Fraud Detection",
        description = "Unusual transactions that break your normal pattern",
        icon = Icons.Default.Security
    ),
    ModuleInfo(
        id = 2,
        title = "Duplicate Subscriptions",
        description = "Overlapping or forgotten recurring charges",
        icon = Icons.Default.Refresh
    ),
    ModuleInfo(
        id = 3,
        title = "Bill Errors",
        description = "Charges that run higher than your usual rate",
        icon = Icons.Default.Receipt
    ),
    ModuleInfo(
        id = 4,
        title = "Spending Anomalies",
        description = "Shifts in your everyday spending behavior",
        icon = Icons.Default.TrendingUp
    ),
    ModuleInfo(
        id = 5,
        title = "Cash Flow Risk",
        description = "Predicted shortfalls before they happen",
        icon = Icons.Default.AccountBalanceWallet
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseModulesScreen(
    onBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChooseModulesViewModel = viewModel()
) {
    val selectedModuleIds by viewModel.selectedModuleIds.collectAsState()
    val selectedCount = selectedModuleIds.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Modules") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveAndContinue(onNavigateToAddTransaction)
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Continue with $selectedCount selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select active detection modules",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            modulesList.forEach { module ->
                val isSelected = selectedModuleIds.contains(module.id)
                ModuleCard(
                    module = module,
                    isSelected = isSelected,
                    onToggle = {
                        viewModel.toggleModule(module.id)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: ModuleInfo,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = module.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = module.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox indicator: brass filled + checkmark when ON, brass outlined square when OFF
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(BrassColor, RoundedCornerShape(4.dp))
                        } else {
                            Modifier.border(2.dp, BrassColor, RoundedCornerShape(4.dp))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


