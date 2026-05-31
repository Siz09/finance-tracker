package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.utils.ParsedReceipt
import com.example.ui.theme.*
import com.example.utils.ExportHelper
import java.io.File

@Composable
fun OcrConfirmationDialog(
    ocr: ParsedReceipt,
    onDismiss: () -> Unit,
    onApply: (
        amount: String?,
        date: String?,
        receiverName: String?,
        receiverId: String?,
        remarks: String?,
        paymentMethod: String?,
        transactionCode: String?,
        processedBy: String?,
        purpose: String?,
        initiatorName: String?,
        suggestedCategory: String?,
        autoNote: String?
    ) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Apply Scanned Receipt?", color = WhiteText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "We scanned the receipt and extracted the following data. Choose which fields you want to apply:", color = GreyText, fontSize = 13.sp)

                HorizontalDivider(color = DarkSurfaceElevated)

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
                    val autoNote = when {
                        !ocr.remarks.isNullOrBlank() -> ocr.remarks
                        !ocr.receiverName.isNullOrBlank() -> ocr.receiverName
                        ocr.merchant != null -> ocr.merchant
                        else -> null
                    }
                    onApply(
                        ocr.amount?.toString(),
                        ocr.date,
                        ocr.receiverName,
                        ocr.receiverId,
                        ocr.remarks,
                        ocr.paymentMethod,
                        ocr.transactionCode,
                        ocr.processedBy,
                        ocr.purpose,
                        ocr.initiatorName,
                        ocr.suggestedCategory,
                        autoNote
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
            ) {
                Text("Apply Extracted Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard", color = RubyExpense)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Transaction?", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = { Text("This will permanently delete this transaction and its attached receipt image. This action cannot be undone.", color = GreyText) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RubyExpense)
            ) { Text("Delete", color = WhiteText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TealPrimary) }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun FullReceiptViewerDialog(
    photoPath: String,
    context: Context,
    onDismiss: () -> Unit
) {
    val attachedFile = File(photoPath)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_receipt_dialog")) {
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
                    AsyncImage(
                        model = attachedFile,
                        contentDescription = "Receipt Zoomed",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
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
