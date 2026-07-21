package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tehreembabar23hayya.financialhealthauditor.data.AppDatabase
import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import com.tehreembabar23hayya.financialhealthauditor.utils.ParsedTransaction
import com.tehreembabar23hayya.financialhealthauditor.utils.TextParser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val _pastedText = MutableStateFlow("")
    val pastedText: StateFlow<String> = _pastedText.asStateFlow()

    private val _photoUri = MutableStateFlow<Uri?>(null)
    val photoUri: StateFlow<Uri?> = _photoUri.asStateFlow()

    private val _ocrText = MutableStateFlow("")
    val ocrText: StateFlow<String> = _ocrText.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parsedResult = MutableStateFlow<ParsedTransaction?>(null)
    val parsedResult: StateFlow<ParsedTransaction?> = _parsedResult.asStateFlow()

    private val _currentTabIndex = MutableStateFlow(0)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()

    init {
        _pastedText
            .onEach { text ->
                if (_currentTabIndex.value == 0) {
                    if (text.isBlank()) {
                        _isParsing.value = false
                        _parsedResult.value = null
                    } else {
                        _isParsing.value = true
                    }
                }
            }
            .debounce(400L)
            .onEach { text ->
                if (_currentTabIndex.value == 0 && text.isNotBlank()) {
                    _parsedResult.value = TextParser.parse(text)
                    _isParsing.value = false
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTabIndexChanged(index: Int) {
        _currentTabIndex.value = index
        if (index == 0) {
            if (_pastedText.value.isNotBlank()) {
                _parsedResult.value = TextParser.parse(_pastedText.value)
            } else {
                _parsedResult.value = null
            }
            _isParsing.value = false
        } else {
            if (_ocrText.value.isNotBlank()) {
                _parsedResult.value = TextParser.parse(_ocrText.value)
            } else {
                _parsedResult.value = null
            }
            _isParsing.value = false
        }
    }

    fun onPastedTextChanged(newText: String) {
        _pastedText.value = newText
    }

    fun processImageUri(context: Context, uri: Uri) {
        _photoUri.value = uri
        _isParsing.value = true
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    _ocrText.value = text
                    _parsedResult.value = TextParser.parse(text)
                    _isParsing.value = false
                }
                .addOnFailureListener {
                    _isParsing.value = false
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _isParsing.value = false
        }
    }

    fun addTransaction(onSuccess: () -> Unit) {
        val result = _parsedResult.value ?: return
        val amountVal = result.amount ?: return
        val isManual = _currentTabIndex.value == 0
        val sourceStr = if (isManual) "manual" else "ocr"
        val raw = if (isManual) _pastedText.value else _ocrText.value

        val newTransaction = Transaction(
            merchant = result.merchant ?: "Unknown Merchant",
            amount = amountVal,
            date = System.currentTimeMillis(),
            category = "General",
            rawText = raw,
            isIncome = !(result.isDebit ?: true),
            source = sourceStr,
            isRecurring = false,
            isFlagged = false,
            issueType = null,
            reviewStatus = "pending"
        )

        viewModelScope.launch {
            transactionDao.insertTransaction(newTransaction)
            onSuccess()
        }
    }
}
