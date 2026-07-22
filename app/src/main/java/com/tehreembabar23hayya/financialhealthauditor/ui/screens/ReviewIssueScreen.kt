package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

class ReviewIssueViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()

    val pendingTransaction: StateFlow<Transaction?> = dao.getAllTransactions()
        .map { list ->
            list.firstOrNull { it.isFlagged && it.reviewStatus == "pending" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateReviewStatus(transaction: Transaction, status: String) {
        viewModelScope.launch {
            dao.insertTransaction(transaction.copy(reviewStatus = status))
        }
    }

    fun updateDraftText(transaction: Transaction, draftText: String) {
        viewModelScope.launch {
            dao.insertTransaction(transaction.copy(draftText = draftText))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewIssueScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewIssueViewModel = viewModel()
) {
    val transaction by viewModel.pendingTransaction.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Issue") },
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
            val currentTx = transaction
            if (currentTx == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No pending issues to review.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            } else {
                var draftText by remember(currentTx.id) { mutableStateOf(currentTx.draftText ?: "") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Transaction details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Transaction Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Merchant:", fontWeight = FontWeight.Medium)
                                Text(currentTx.merchant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount:", fontWeight = FontWeight.Medium)
                                Text("PKR ${currentTx.amount}")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Category:", fontWeight = FontWeight.Medium)
                                Text(currentTx.category)
                            }
                        }
                    }

                    // Bordered explanation callout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Explanation",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentTx.explanation ?: "This transaction is flagged as a duplicate subscription.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Editable draft field
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = {
                            draftText = it
                            viewModel.updateDraftText(currentTx, it)
                        },
                        label = { Text("Resolution Email Draft") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateReviewStatus(currentTx, "dismissed")
                                onBack()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Dismiss")
                        }

                        Button(
                            onClick = {
                                viewModel.updateReviewStatus(currentTx, "approved")
                                
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_SUBJECT, "Cancellation Request: ${currentTx.merchant}")
                                    putExtra(Intent.EXTRA_TEXT, draftText)
                                }
                                val chooserIntent = Intent.createChooser(emailIntent, "Send cancellation request...")
                                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooserIntent)
                                
                                onBack()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve & send")
                        }
                    }
                }
            }
        }
    }
}

