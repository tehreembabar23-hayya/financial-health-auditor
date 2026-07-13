package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onNavigateToChooseModules: () -> Unit,
    onNavigateToFlaggedIssues: () -> Unit,
    onNavigateToMonthlySummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Financial Health Auditor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI-powered transaction risk & audit tool",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateToChooseModules,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Choose Modules")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToFlaggedIssues,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Flagged Issues")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToMonthlySummary,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Monthly Summary")
        }
    }
}
