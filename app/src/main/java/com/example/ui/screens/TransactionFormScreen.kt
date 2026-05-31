package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.utils.ExportHelper
import com.example.utils.FileStorageHelper
import com.example.utils.ReceiptParser
import com.example.utils.ParsedReceipt
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    viewModel: FinanceViewModel,
    transactionId: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var initialAmount by remember { mutableStateOf("") }
    var initialType by remember { mutableStateOf("expense") }
    var initialCategory by remember { mutableStateOf("") }
    var initialDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var initialNote by remember { mutableStateOf("") }
    var photoPathState by remember { mutableStateOf<String?>(null) }
    var isEditingMode by remember { mutableStateOf(false) }

    var receiverName by remember { mutableStateOf<String?>(null) }
    var receiverId by remember { mutableStateOf<String?>(null) }
    var remarks by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf<String?>(null) }
    var transactionCode by remember { mutableStateOf<String?>(null) }
    var processedBy by remember { mutableStateOf<String?>(null) }
    var purpose by remember { mutableStateOf<String?>(null) }
    var initiatorName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId > 0) {
            val tx = viewModel.getTransactionById(transactionId)
            if (tx != null) {
                initialAmount = tx.amount.toString()
                initialType = tx.type
                initialCategory = tx.category
                initialDate = tx.date
                initialNote = tx.note ?: ""
                photoPathState = tx.imagePath
                isEditingMode = true
                receiverName = tx.receiverName
                receiverId = tx.receiverId
                remarks = tx.remarks
                paymentMethod = tx.paymentMethod
                transactionCode = tx.transactionCode
                processedBy = tx.processedBy
                purpose = tx.purpose
                initiatorName = tx.initiatorName
            }
        } else {
            initialCategory = Category.EXPENSES.first().name
        }
    }

    var amount by remember(initialAmount) { mutableStateOf(initialAmount) }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var date by remember(initialDate) { mutableStateOf(initialDate) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showFullReceiptDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // OCR Confirmation Overlay State (Fix #16)
    var pendingOcrResult by remember { mutableStateOf<ParsedReceipt?>(null) }
    var showOcrConfirmationDialog by remember { mutableStateOf(false) }

    // Expandable metadata state (Fix #22)
    var showMetadataFields by remember { mutableStateOf(false) }

    val isFormValid = remember(amount, category) {
        val amtVal = amount.toDoubleOrNull()
        amtVal != null && amtVal > 0 && category.isNotEmpty()
    }

    // Inline amount validation state — only show error once user has typed something
    val amountHasError = remember(amount) {
        amount.isNotBlank() && (amount.toDoubleOrNull() == null || (amount.toDoubleOrNull() ?: 0.0) <= 0.0)
    }

    // Temp file URI for full-res camera capture
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val processImageForOcr = { path: String ->
        val file = File(path)
        if (file.exists()) {
            isScanning = true
            coroutineScope.launch {
                try {
                    val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            android.util.Log.d("OCR_RAW", visionText.text)
                            // Move heavy parsing off the main thread to prevent 40-frame jank
                            coroutineScope.launch {
                                val parsed = withContext(Dispatchers.Default) {
                                    ReceiptParser.parse(visionText.text)
                                }
                                isScanning = false
                                if (parsed.amount != null || parsed.date != null ||
                                    parsed.receiverName != null || parsed.paymentMethod != null) {
                                    pendingOcrResult = parsed
                                    showOcrConfirmationDialog = true
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Scanning complete but no relevant fields extracted.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            isScanning = false
                            Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    isScanning = false
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = FileStorageHelper.saveImageToInternalStorage(context, uri)
            if (saved != null) {
                photoPathState = saved
                processImageForOcr(saved)
            }
        }
    }

    // Storage/Gallery permission checks (Fix #10)
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Gallery permission is required to select receipt images", Toast.LENGTH_LONG).show()
        }
    }

    // Full-resolution camera launcher using TakePicture + FileProvider URI
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                val saved = FileStorageHelper.saveImageToInternalStorage(context, uri)
                if (saved != null) {
                    photoPathState = saved
                    processImageForOcr(saved)
                }
            }
        }
    }

    // Camera permission launcher (Fix #8)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", photoFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture receipts", Toast.LENGTH_LONG).show()
        }
    }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", photoFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasPerm = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            galleryLauncher.launch("image/*")
        } else {
            galleryPermissionLauncher.launch(perm)
        }
    }

    // DatePickerDialog state
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = if (isEditingMode) "Edit Transaction" else "Add Transaction", color = WhiteText) },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_form")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Amount", style = MaterialTheme.typography.labelLarge, color = GreyText)
                if (isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Scanning receipt...", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                    }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth().testTag("input_transaction_amount"),
                placeholder = { Text("Rs. 0", color = GreyText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountHasError,
                supportingText = if (amountHasError) {
                    { Text("Enter a valid amount greater than 0", color = RubyExpense) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (amountHasError) RubyExpense else TealPrimary,
                    unfocusedBorderColor = if (amountHasError) RubyExpense else DarkSurfaceElevated,
                    errorBorderColor = RubyExpense,
                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface,
                    errorContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // Type selector
            Text(text = "Transaction Type", style = MaterialTheme.typography.labelLarge, color = GreyText)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { type = "expense"; category = Category.EXPENSES.first().name },
                    modifier = Modifier.weight(1f).testTag("btn_type_expense"),
                    colors = ButtonDefaults.buttonColors(containerColor = if (type == "expense") RubyExpense else DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Expense", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { type = "income"; category = Category.INCOMES.first().name },
                    modifier = Modifier.weight(1f).testTag("btn_type_income"),
                    colors = ButtonDefaults.buttonColors(containerColor = if (type == "income") MintIncome else DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Income", fontWeight = FontWeight.Bold)
                }
            }

            // Category
            Text(text = "Category", style = MaterialTheme.typography.labelLarge, color = GreyText)
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentCategories = if (type == "expense") Category.EXPENSES else Category.INCOMES
                Row(
                    modifier = Modifier.fillMaxWidth().background(DarkSurface, RoundedCornerShape(12.dp))
                        .clickable { isCategoryDropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp).testTag("dropdown_select_category"),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = Category.getIcon(category, type), fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = category, color = WhiteText, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = WhiteText)
                }
                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated).fillMaxWidth(0.9f)
                ) {
                    currentCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = cat.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = cat.name, color = WhiteText)
                                }
                            },
                            onClick = { category = cat.name; isCategoryDropdownExpanded = false },
                            modifier = Modifier.testTag("category_item_${cat.name}")
                        )
                    }
                }
            }

            // Date picker field (Tapping anywhere in the box now launches the date selector!)
            Text(text = "Date", style = MaterialTheme.typography.labelLarge, color = GreyText)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().testTag("input_transaction_date"),
                    placeholder = { Text("YYYY-MM-DD", color = GreyText) },
                    readOnly = true,
                    enabled = false, // Disable typing but keep custom styling so it is clickable!
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = TealPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = DarkSurfaceElevated,
                        disabledTextColor = WhiteText,
                        disabledContainerColor = DarkSurface,
                        disabledTrailingIconColor = TealPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Note
            Text(text = "Notes (Optional)", style = MaterialTheme.typography.labelLarge, color = GreyText)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().height(110.dp).testTag("input_transaction_note"),
                placeholder = { Text("Enter a brief description here...", color = GreyText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp), maxLines = 4
            )

            // digital transaction metadata fields (Fix #22)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showMetadataFields = !showMetadataFields },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Digital Payment Metadata", fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 14.sp)
                        }
                        Icon(
                            imageVector = if (showMetadataFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = GreyText
                        )
                    }

                    AnimatedVisibility(visible = showMetadataFields || receiverName != null || receiverId != null || remarks != null || paymentMethod != null || transactionCode != null || processedBy != null || purpose != null || initiatorName != null) {
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Receiver Name
                            Text(text = "Merchant / Receiver Name", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = receiverName ?: "",
                                onValueChange = { receiverName = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_name"),
                                placeholder = { Text("e.g. eSewa Transfer / Shop Name", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Receiver Account/ID
                            Text(text = "Account / Mobile Number", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = receiverId ?: "",
                                onValueChange = { receiverId = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_id"),
                                placeholder = { Text("e.g. 9841xxxxxx / 01234567", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Payment Method
                            Text(text = "Payment Method", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = paymentMethod ?: "",
                                onValueChange = { paymentMethod = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_payment_method"),
                                placeholder = { Text("e.g. eSewa, Khalti, Fonepay, Cash", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Narrative Remarks
                            Text(text = "Narrative Remarks", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = remarks ?: "",
                                onValueChange = { remarks = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_remarks"),
                                placeholder = { Text("e.g. Funds transfer details", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Transaction Code
                            Text(text = "Transaction Code", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = transactionCode ?: "",
                                onValueChange = { transactionCode = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_transaction_code"),
                                placeholder = { Text("e.g. 16D37HB", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Processed By
                            Text(text = "Processed By", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = processedBy ?: "",
                                onValueChange = { processedBy = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_processed_by"),
                                placeholder = { Text("e.g. 9844296224", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Purpose
                            Text(text = "Purpose", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = purpose ?: "",
                                onValueChange = { purpose = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_purpose"),
                                placeholder = { Text("e.g. Personal Use", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )

                            // Initiator Name
                            Text(text = "Initiator Name", style = MaterialTheme.typography.labelMedium, color = GreyText)
                            OutlinedTextField(
                                value = initiatorName ?: "",
                                onValueChange = { initiatorName = it.ifBlank { null } },
                                modifier = Modifier.fillMaxWidth().testTag("input_initiator_name"),
                                placeholder = { Text("e.g. Sijan Maharjan", color = GreyText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                                    focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )
                        }
                    }
                }
            }

            // Receipt
            Text(text = "Receipt attachment", style = MaterialTheme.typography.labelLarge, color = GreyText)
            if (photoPathState == null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f).testTag("btn_capture_photo"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Capture Photo")
                    }
                    OutlinedButton(
                        onClick = { launchGallery() },
                        modifier = Modifier.weight(1f).testTag("btn_select_gallery"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "From Gallery")
                    }
                }
            } else {
                val file = remember(photoPathState) { File(photoPathState!!) }
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MintIncome)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Receipt Attached", color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row {
                                IconButton(onClick = { showFullReceiptDialog = true }, modifier = Modifier.testTag("btn_view_receipt")) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Photo", tint = TealPrimary)
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                                            ExportHelper.shareFile(context, uri, "image/jpeg")
                                        } catch (e: Exception) { e.printStackTrace() }
                                    },
                                    modifier = Modifier.testTag("btn_share_receipt")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share Receipt", tint = TealPrimary)
                                }
                                IconButton(
                                    onClick = { FileStorageHelper.deleteImage(photoPathState); photoPathState = null },
                                    modifier = Modifier.testTag("btn_delete_receipt")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Photo", tint = RubyExpense)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Receipt Attachment Preview",
                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).clickable { showFullReceiptDialog = true },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save button
            Button(
                onClick = {
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    val cleanNote = note.ifBlank { null }
                    // Normalize date format
                    val normalizedDate = date.replace("/", "-")
                    if (isEditingMode && transactionId != null) {
                        viewModel.updateTransaction(
                            id = transactionId, type = type, amount = amtVal,
                            category = category, date = normalizedDate, note = cleanNote,
                            imagePath = photoPathState, receiverName = receiverName,
                            receiverId = receiverId, remarks = remarks,
                            paymentMethod = paymentMethod, transactionCode = transactionCode,
                            processedBy = processedBy, purpose = purpose,
                            initiatorName = initiatorName
                        )
                    } else {
                        viewModel.addTransaction(
                            type = type, amount = amtVal, category = category,
                            date = normalizedDate, note = cleanNote,
                            imagePath = photoPathState, receiverName = receiverName,
                            receiverId = receiverId, remarks = remarks,
                            paymentMethod = paymentMethod, transactionCode = transactionCode,
                            processedBy = processedBy, purpose = purpose,
                            initiatorName = initiatorName
                        )
                    }
                    onDismiss()
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_save_transaction"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg, disabledContainerColor = GreyText.copy(alpha = 0.3f), disabledContentColor = GreyText),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isEditingMode) "Update Transaction" else "Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            // Delete button (edit mode only) — with confirmation
            if (isEditingMode && transactionId != null) {
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_delete_transaction"),
                    colors = ButtonDefaults.buttonColors(containerColor = RubyExpense, contentColor = WhiteText),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Delete Log Permanently", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // OCR Overwrite Confirmation Dialogue (Fix #16)
    if (showOcrConfirmationDialog && pendingOcrResult != null) {
        val ocr = pendingOcrResult!!
        AlertDialog(
            onDismissRequest = { showOcrConfirmationDialog = false },
            title = {
                Text(text = "Apply Scanned Receipt?", color = WhiteText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "We scanned the receipt and extracted the following data. Choose which fields you want to apply:", color = GreyText, fontSize = 13.sp)

                    Divider(color = DarkSurfaceElevated)

                    if (ocr.amount != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text("Rs. ${ocr.amount}", color = TealPrimary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (ocr.date != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.date, color = TealPrimary)
                        }
                    }
                    if (ocr.receiverName != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Receiver:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.receiverName, color = WhiteText)
                        }
                    }
                    if (ocr.receiverId != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("eSewa ID:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.receiverId, color = WhiteText, fontSize = 12.sp)
                        }
                    }
                    if (ocr.paymentMethod != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.paymentMethod, color = WhiteText)
                        }
                    }
                    if (ocr.transactionCode != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Txn Code:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.transactionCode, color = WhiteText)
                        }
                    }
                    if (ocr.purpose != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Purpose:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.purpose, color = WhiteText)
                        }
                    }
                    if (ocr.remarks != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remarks:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.remarks, color = WhiteText)
                        }
                    }
                    if (ocr.processedBy != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Processed By:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.processedBy, color = WhiteText)
                        }
                    }
                    if (ocr.initiatorName != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Initiator:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.initiatorName, color = WhiteText)
                        }
                    }
                    if (ocr.suggestedCategory != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Suggested Cat:", color = GreyText, fontWeight = FontWeight.Bold)
                            Text(ocr.suggestedCategory, color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ocr.amount != null) amount = ocr.amount.toString()
                        if (ocr.date != null) date = ocr.date
                        if (ocr.receiverName != null) receiverName = ocr.receiverName
                        if (ocr.receiverId != null) receiverId = ocr.receiverId
                        if (ocr.remarks != null) remarks = ocr.remarks
                        if (ocr.paymentMethod != null) paymentMethod = ocr.paymentMethod
                        if (ocr.transactionCode != null) transactionCode = ocr.transactionCode
                        if (ocr.processedBy != null) processedBy = ocr.processedBy
                        if (ocr.purpose != null) purpose = ocr.purpose
                        if (ocr.initiatorName != null) initiatorName = ocr.initiatorName
                        if (ocr.suggestedCategory != null) category = ocr.suggestedCategory

                        // Auto-fill note intelligently if appropriate
                        if (!ocr.remarks.isNullOrBlank()) {
                            note = ocr.remarks
                        } else if (!ocr.receiverName.isNullOrBlank()) {
                            note = ocr.receiverName
                        } else if (ocr.merchant != null) {
                            note = ocr.merchant
                        }

                        showOcrConfirmationDialog = false
                        Toast.makeText(context, "Receipt data applied successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
                ) {
                    Text("Apply Extracted Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOcrConfirmationDialog = false }
                ) {
                    Text("Discard", color = RubyExpense)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK", color = TealPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = GreyText) }
            },
            colors = DatePickerDefaults.colors(containerColor = DarkSurfaceElevated)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = DarkSurfaceElevated, titleContentColor = WhiteText, headlineContentColor = TealPrimary, weekdayContentColor = GreyText, dayContentColor = WhiteText, selectedDayContainerColor = TealPrimary, selectedDayContentColor = DarkBg, todayContentColor = TealPrimary, todayDateBorderColor = TealPrimary))
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && transactionId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Transaction?", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete this transaction and its attached receipt image. This action cannot be undone.", color = GreyText) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTransaction(transactionId); showDeleteConfirmDialog = false; onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyExpense)
                ) { Text("Delete", color = WhiteText) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel", color = TealPrimary) }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Full-screen receipt viewer
    if (showFullReceiptDialog && photoPathState != null) {
        val attachedFile = File(photoPathState!!)
        AlertDialog(
            onDismissRequest = { showFullReceiptDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFullReceiptDialog = false }, modifier = Modifier.testTag("btn_close_receipt_dialog")) {
                    Text("Close", color = TealPrimary)
                }
            },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Receipt Attachment View", fontSize = 16.sp, color = WhiteText, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", attachedFile)
                                ExportHelper.shareFile(context, uri, "image/jpeg")
                            } catch (e: Exception) { e.printStackTrace() }
                        },
                        modifier = Modifier.testTag("btn_share_receipt_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Receipt", tint = TealPrimary)
                    }
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    if (attachedFile.exists()) {
                        AsyncImage(model = attachedFile, contentDescription = "Receipt Zoomed", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Image file not found", color = RubyExpense)
                        }
                    }
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
