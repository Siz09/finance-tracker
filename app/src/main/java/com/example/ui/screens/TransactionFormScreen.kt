package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.utils.ExportHelper
import com.example.utils.FileStorageHelper
import com.example.utils.ReceiptParser
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    viewModel: FinanceViewModel,
    transactionId: Int? = null, // None if adding, populated if editing
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Retrieve initial transaction values if editing
    var initialAmount by remember { mutableStateOf("") }
    var initialType by remember { mutableStateOf("expense") }
    var initialCategory by remember { mutableStateOf("") }
    var initialDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var initialNote by remember { mutableStateOf("") }
    var photoPathState by remember { mutableStateOf<String?>(null) }
    var isEditingMode by remember { mutableStateOf(false) }

    // Deep OCR captured fields
    var receiverName by remember { mutableStateOf<String?>(null) }
    var receiverId by remember { mutableStateOf<String?>(null) }
    var remarks by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf<String?>(null) }

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
            }
        } else {
            // Set first item as default category on blank form
            initialCategory = Category.EXPENSES.first().name
        }
    }

    // Input States
    var amount by remember(initialAmount) { mutableStateOf(initialAmount) }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var date by remember(initialDate) { mutableStateOf(initialDate) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    // Dropdown list category controller
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Full screen screen zoomable overlay indicator
    var showFullReceiptDialog by remember { mutableStateOf(false) }

    // OCR Loading state
    var isScanning by remember { mutableStateOf(false) }

    // Form inputs validators
    val isFormValid = remember(amount, category) {
        val amtVal = amount.toDoubleOrNull()
        amtVal != null && amtVal > 0 && category.isNotEmpty()
    }

    // OCR logic
    val processImageForOcr = { path: String ->
        val file = File(path)
        if (file.exists()) {
            isScanning = true
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val parsed = ReceiptParser.parse(visionText.text)
                    if (parsed.amount != null) amount = parsed.amount.toString()
                    if (parsed.date != null) date = parsed.date
                    
                    // Capture deep extraction data
                    receiverName = parsed.receiverName
                    receiverId = parsed.receiverId
                    remarks = parsed.remarks
                    paymentMethod = parsed.paymentMethod
                    
                    // If remarks exist, prioritize them in the note field
                    if (!parsed.remarks.isNullOrBlank()) {
                        note = parsed.remarks
                    } else if (!parsed.receiverName.isNullOrBlank()) {
                        note = parsed.receiverName
                    } else if (parsed.merchant != null) {
                        note = parsed.merchant
                    }

                    isScanning = false
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    isScanning = false
                }
        }
    }

    // Activity result loaders
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

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                // Save temp bitmap to cache first, write to Uri, copy to sandbox receipts
                val tempFile = File(context.cacheDir, "temp_camera.jpg")
                val out = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()

                val tempUri = Uri.fromFile(tempFile)
                val saved = FileStorageHelper.saveImageToInternalStorage(context, tempUri)
                if (saved != null) {
                    photoPathState = saved
                    processImageForOcr(saved)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
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
            // --- Amount input Box ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_amount"),
                placeholder = { Text("Rs. 0.00", color = GreyText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // --- Type selector ---
            Text(text = "Transaction Type", style = MaterialTheme.typography.labelLarge, color = GreyText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        type = "expense"
                        category = Category.EXPENSES.first().name
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_type_expense"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "expense") RubyExpense else DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Expense", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        type = "income"
                        category = Category.INCOMES.first().name
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_type_income"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "income") MintIncome else DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Income", fontWeight = FontWeight.Bold)
                }
            }

            // --- Category Selector ---
            Text(text = "Category", style = MaterialTheme.typography.labelLarge, color = GreyText)
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentCategories = if (type == "expense") Category.EXPENSES else Category.INCOMES
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .clickable { isCategoryDropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("dropdown_select_category"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier
                        .background(DarkSurfaceElevated)
                        .fillMaxWidth(0.9f)
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
                            onClick = {
                                category = cat.name
                                isCategoryDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("category_item_${cat.name}")
                        )
                    }
                }
            }

            // --- Date String Input ---
            Text(text = "Date", style = MaterialTheme.typography.labelLarge, color = GreyText)
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_date"),
                placeholder = { Text("YYYY-MM-DD", color = GreyText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // --- Note input (Optional) ---
            Text(text = "Notes (Optional)", style = MaterialTheme.typography.labelLarge, color = GreyText)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("input_transaction_note"),
                placeholder = { Text("Enter a brief description here...", color = GreyText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            // --- Receipt Attachment Box ---
            Text(text = "Receipt attachment", style = MaterialTheme.typography.labelLarge, color = GreyText)
            
            if (photoPathState == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_capture_photo"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Capture Photo")
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_select_gallery"),
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
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MintIncome)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Receipt Attached",
                                    color = WhiteText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Share and Delete toolbar inside attachment card
                            Row {
                                IconButton(
                                    onClick = { showFullReceiptDialog = true },
                                    modifier = Modifier.testTag("btn_view_receipt")
                                ) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Photo", tint = TealPrimary)
                                }
                                IconButton(
                                    onClick = {
                                        FileStorageHelper.deleteImage(photoPathState)
                                        photoPathState = null
                                    },
                                    modifier = Modifier.testTag("btn_delete_receipt")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Photo", tint = RubyExpense)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Small Preview
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Receipt Attachment Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showFullReceiptDialog = true },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Saving action button!
            Button(
                onClick = {
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    val cleanNote = note.ifBlank { null }
                    if (isEditingMode && transactionId != null) {
                        viewModel.updateTransaction(
                            id = transactionId,
                            type = type,
                            amount = amtVal,
                            category = category,
                            date = date,
                            note = cleanNote,
                            imagePath = photoPathState,
                            receiverName = receiverName,
                            receiverId = receiverId,
                            remarks = remarks,
                            paymentMethod = paymentMethod
                        )
                    } else {
                        viewModel.addTransaction(
                            type = type,
                            amount = amtVal,
                            category = category,
                            date = date,
                            note = cleanNote,
                            imagePath = photoPathState,
                            receiverName = receiverName,
                            receiverId = receiverId,
                            remarks = remarks,
                            paymentMethod = paymentMethod
                        )
                    }
                    onDismiss()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_transaction"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    contentColor = DarkBg,
                    disabledContainerColor = GreyText.copy(alpha = 0.3f),
                    disabledContentColor = GreyText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isEditingMode) "Update Transaction" else "Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            // Deletion action (only visible in edit mode)
            if (isEditingMode && transactionId != null) {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transactionId)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_delete_transaction"),
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

    // --- ZOOM IMAGE DIALOG OVERLAY (WITH SHARING ACTION) ---
    if (showFullReceiptDialog && photoPathState != null) {
        val attachedFile = File(photoPathState!!)
        
        AlertDialog(
            onDismissRequest = { showFullReceiptDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showFullReceiptDialog = false },
                    modifier = Modifier.testTag("btn_close_receipt_dialog")
                ) {
                    Text("Close", color = TealPrimary)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Receipt Attachment View", fontSize = 16.sp, color = WhiteText, fontWeight = FontWeight.Bold)
                    
                    // Share Receipt sheet option
                    IconButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "com.aistudio.financetracker.axpdky.fileprovider",
                                    attachedFile
                                )
                                ExportHelper.shareFile(context, uri, "image/jpeg")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.testTag("btn_share_receipt_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Receipt Attachment", tint = TealPrimary)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (attachedFile.exists()) {
                        AsyncImage(
                            model = attachedFile,
                            contentDescription = "Receipt Attachment Zoomed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Image load error file does not exist", color = RubyExpense)
                        }
                    }
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
