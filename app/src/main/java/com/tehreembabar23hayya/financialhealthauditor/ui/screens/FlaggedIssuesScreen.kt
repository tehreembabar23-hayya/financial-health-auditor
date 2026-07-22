package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehreembabar23hayya.financialhealthauditor.data.AppDatabase
import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlaggedIssuesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val flaggedTransactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .map { list ->
            val enabledModules = prefs.getStringSet("enabledModules", setOf("1", "2", "3", "4", "5")) ?: setOf("1", "2", "3", "4", "5")
            list.filter { tx ->
                tx.isFlagged && tx.reviewStatus == "pending" && enabledModules.contains(getModuleIdForIssueType(tx.issueType))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateReviewStatus(transaction: Transaction, status: String) {
        viewModelScope.launch {
            dao.insertTransaction(transaction.copy(reviewStatus = status))
        }
    }

    private fun getModuleIdForIssueType(issueType: String?): String {
        return when (issueType?.lowercase()) {
            "fraud" -> "1"
            "duplicate" -> "2"
            "bill", "bill_error", "error" -> "3"
            "anomaly", "spending_anomaly" -> "4"
            "cash", "cash_flow", "cash_flow_risk" -> "5"
            else -> "1"
        }
    }
}

private fun getIssueTypeColor(issueType: String?): Color {
    return when (issueType?.lowercase()) {
        "fraud" -> Color(0xFFE53935)
        "duplicate" -> Color(0xFF8E24AA)
        "bill", "bill_error", "error" -> Color(0xFFFB8C00)
        "anomaly", "spending_anomaly" -> Color(0xFFFDD835)
        "cash", "cash_flow", "cash_flow_risk" -> Color(0xFF1E88E5)
        else -> Color(0xFF757575)
    }
}

private fun getIssueTypeIcon(issueType: String?): ImageVector {
    return when (issueType?.lowercase()) {
        "fraud" -> Icons.Default.Security
        "duplicate" -> Icons.Default.Refresh
        "bill", "bill_error", "error" -> Icons.Default.Receipt
        "anomaly", "spending_anomaly" -> Icons.Default.TrendingUp
        "cash", "cash_flow", "cash_flow_risk" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Info
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlaggedIssuesScreen(
    onBack: () -> Unit,
    onNavigateToReviewIssue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlaggedIssuesViewModel = viewModel()
) {
    val flaggedTransactions by viewModel.flaggedTransactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flagged Issues") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (flaggedTransactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No Issues",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All Clear!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No flagged issues found in your active modules.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(flaggedTransactions, key = { it.id }) { transaction ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        viewModel.updateReviewStatus(transaction, "approved")
                                        true
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        viewModel.updateReviewStatus(transaction, "dismissed")
                                        true
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green
                                    SwipeToDismissBoxValue.EndToStart -> Color(0xFF9E9E9E) // Gray
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 8.dp)
                                        .background(color, shape = RoundedCornerShape(12.dp))
                                )
                            }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { onNavigateToReviewIssue() },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colored bar on the left
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .fillMaxHeight()
                                            .background(getIssueTypeColor(transaction.issueType))
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Icon
                                    Icon(
                                        imageVector = getIssueTypeIcon(transaction.issueType),
                                        contentDescription = null,
                                        tint = getIssueTypeColor(transaction.issueType),
                                        modifier = Modifier.size(28.dp)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Merchant & Reason (issueType)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 16.dp)
                                    ) {
                                        Text(
                                            text = transaction.merchant,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = transaction.issueType ?: "Unknown Issue",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Amount
                                    Text(
                                        text = "PKR ${transaction.amount}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


