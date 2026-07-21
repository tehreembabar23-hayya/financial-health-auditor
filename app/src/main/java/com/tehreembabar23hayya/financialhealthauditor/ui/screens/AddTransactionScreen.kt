package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehreembabar23hayya.financialhealthauditor.utils.ParsedTransaction

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTabIndex by viewModel.currentTabIndex.collectAsState()
    val pastedText by viewModel.pastedText.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()

    val isAddEnabled = parsedResult?.amount != null

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processImageUri(context, it) }
    }

    val tabs = listOf("Paste text", "Photo")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
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
                            viewModel.addTransaction(onSuccess = onBack)
                        },
                        enabled = isAddEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Add transaction",
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
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = currentTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTabIndex == index,
                        onClick = { viewModel.onTabIndexChanged(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (currentTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (currentTabIndex) {
                0 -> {
                    // Paste text tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { viewModel.onPastedTextChanged(it) },
                            placeholder = { Text("Paste transaction text or receipt details here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            minLines = 6,
                            singleLine = false
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ParsedResultChips(
                            isParsing = isParsing,
                            parsedResult = parsedResult
                        )
                    }
                }
                1 -> {
                    // Photo tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (photoUri != null) "Photo selected" else "Choose a photo of your bill",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { galleryLauncher.launch("image/*") }
                                ) {
                                    Text(if (photoUri != null) "Change photo" else "Choose from gallery")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ParsedResultChips(
                            isParsing = isParsing,
                            parsedResult = parsedResult
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParsedResultChips(
    isParsing: Boolean,
    parsedResult: ParsedTransaction?
) {
    if (isParsing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reading message…",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    } else if (parsedResult != null) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            parsedResult.amount?.let { amountVal ->
                AssistChip(
                    onClick = {},
                    label = { Text("Amount: Rs ${String.format("%.2f", amountVal)}") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            parsedResult.merchant?.let { merchantVal ->
                AssistChip(
                    onClick = {},
                    label = { Text("Merchant: $merchantVal") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            parsedResult.isDebit?.let { isDebit ->
                AssistChip(
                    onClick = {},
                    label = { Text(if (isDebit) "Debit" else "Credit") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}



