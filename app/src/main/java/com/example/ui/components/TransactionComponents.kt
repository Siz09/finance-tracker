package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.utils.CurrencyFormatter
import java.io.File

@Composable
fun TransactionCardItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val emoji = remember(transaction) { Category.getIcon(transaction.category, transaction.type) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "caret")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
            .testTag("transaction_card_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Main Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = WhiteText
                            )
                            if (transaction.imagePath != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.Receipt, contentDescription = "Receipt Attached", tint = TealPrimary, modifier = Modifier.size(14.dp))
                            }
                            if (transaction.isRecurring) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.Autorenew, contentDescription = "Recurring", tint = MintIncome, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (!transaction.note.isNullOrBlank()) {
                            Text(
                                text = transaction.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = GreyText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(text = transaction.date, style = MaterialTheme.typography.bodySmall, color = GreyText.copy(alpha = 0.7f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (transaction.type == "expense") "- " else "+ ") + CurrencyFormatter.format(transaction.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (transaction.type == "expense") RubyExpense else MintIncome
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = GreyText,
                        modifier = Modifier.size(20.dp).rotate(rotationState)
                    )
                }
            }

            // Expandable details drawer showing OCR / digital metadata & quick actions
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Check if any digital metadata exists
                    val hasMetadata = transaction.receiverName != null || transaction.receiverId != null ||
                            transaction.remarks != null || transaction.paymentMethod != null ||
                            transaction.transactionCode != null || transaction.processedBy != null ||
                            transaction.purpose != null || transaction.initiatorName != null

                    if (hasMetadata) {
                        Text(
                            text = "DIGITAL PAYMENT METADATA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            letterSpacing = 1.sp
                        )

                        transaction.receiverName?.let {
                            MetadataRow(label = "Merchant / Receiver", value = it)
                        }
                        transaction.receiverId?.let {
                            MetadataRow(label = "Mobile / Wallet ID", value = it)
                        }
                        transaction.paymentMethod?.let {
                            MetadataRow(label = "Payment Channel", value = it)
                        }
                        transaction.transactionCode?.let {
                            MetadataRow(label = "Transaction Code", value = it)
                        }
                        transaction.remarks?.let {
                            MetadataRow(label = "Remarks / Memo", value = it)
                        }
                        transaction.processedBy?.let {
                            MetadataRow(label = "Processed By", value = it)
                        }
                        transaction.purpose?.let {
                            MetadataRow(label = "Purpose", value = it)
                        }
                        transaction.initiatorName?.let {
                            MetadataRow(label = "Initiator", value = it)
                        }
                    }

                    if (transaction.isRecurring) {
                        Text(
                            text = "RECURRING SYSTEM SCHEDULER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintIncome,
                            letterSpacing = 1.sp
                        )
                        MetadataRow(label = "Frequency", value = transaction.recurrenceFrequency?.uppercase() ?: "MONTHLY")
                    }

                    // Thumbnail preview if receipt exists
                    transaction.imagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(
                                model = file,
                                contentDescription = "Receipt Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Row of quick actions inside details drawer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onEditClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Log Entry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = WhiteText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
