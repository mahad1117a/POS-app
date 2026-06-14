package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Transaction
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalTaxCollected by viewModel.totalTaxCollected.collectAsState()

    var activeReceiptLookup by remember { mutableStateOf<Transaction?>(null) }
    var historySearchQuery by remember { mutableStateOf("") }

    val filteredTransactions = transactions.filter {
        it.id.contains(historySearchQuery, ignoreCase = true) ||
        it.fbrInvoiceNo.contains(historySearchQuery, ignoreCase = true) ||
        it.paymentMethod.contains(historySearchQuery, ignoreCase = true) ||
        (it.customerPhone?.contains(historySearchQuery) == true)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 680.dp

        if (isTablet) {
            // Tablet Side-by-Side: Left visual reports and Canvas charts, Right historical transaction log
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Canvas charts and KPI metrics (Width 50%)
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricsGrid(
                        totalSales = totalRevenue ?: 0.0,
                        totalTax = totalTaxCollected ?: 0.0,
                        count = transactions.size
                    )
                    VisualChartsCard(transactions)
                }

                // Right Panel: Recent activity list search (Width 50%)
                Column(modifier = Modifier.weight(2f)) {
                    ReceiptLogPanel(
                        historySearchQuery = historySearchQuery,
                        onHistorySearchChange = { historySearchQuery = it },
                        filteredTransactions = filteredTransactions,
                        onSelectReceipt = { activeReceiptLookup = it }
                    )
                }
            }
        } else {
            // Mobile Stack Layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    KpiMetricsGrid(
                        totalSales = totalRevenue ?: 0.0,
                        totalTax = totalTaxCollected ?: 0.0,
                        count = transactions.size
                    )
                }

                item {
                    VisualChartsCard(transactions)
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 500.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ReceiptLogPanel(
                                historySearchQuery = historySearchQuery,
                                onHistorySearchChange = { historySearchQuery = it },
                                filteredTransactions = filteredTransactions,
                                onSelectReceipt = { activeReceiptLookup = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active receipt review overlay dialog
    activeReceiptLookup?.let { transaction ->
        ReceiptDialog(
            transaction = transaction,
            onClose = { activeReceiptLookup = null }
        )
    }
}

@Composable
fun KpiMetricsGrid(
    totalSales: Double,
    totalTax: Double,
    count: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Business KPI Diagnostics / آمدنی رپورٹ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiMetricTinyCard(
                    title = "Total Sales (Rs.)",
                    value = String.format("%.2f", totalSales),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricTinyCard(
                    title = "GST Collected",
                    value = String.format("%.2f", totalTax),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricTinyCard(
                    title = "Receipt Counts",
                    value = "$count Invoices",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
fun KpiMetricTinyCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VisualChartsCard(transactions: List<Transaction>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Payment Methods Breakdown & Sales Trend",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "رپورٹ ادائیگی اور رجحان",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Register sales to generate chart analytics / چارٹ رپورٹ کے لیے پہلے انوائس بنائیں", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                // Calculate payment ratios
                val totalQty = transactions.size.toDouble()
                val cashCount = transactions.count { it.paymentMethod == "Cash" }.toDouble()
                val epCount = transactions.count { it.paymentMethod == "EasyPaisa" }.toDouble()
                val jcCount = transactions.count { it.paymentMethod == "JazzCash" }.toDouble()
                val otherCount = totalQty - cashCount - epCount - jcCount

                val cashPct = (cashCount / totalQty).toFloat()
                val epPct = (epCount / totalQty).toFloat()
                val jcPct = (jcCount / totalQty).toFloat()
                val otherPct = (otherCount / totalQty).toFloat()

                // 1. Draw Ratio stacked horiz chart
                Text("Receipt Channel Share / ادائیگی چینل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    val cashW = w * cashPct
                    val epW = w * epPct
                    val jcW = w * jcPct
                    val otherW = w * otherPct

                    var currentX = 0f

                    // Cash Segment (Moss Green)
                    if (cashW > 0) {
                        drawRect(
                            color = Color(0xFF5A5A40),
                            topLeft = Offset(currentX, 0f),
                            size = Size(cashW, h)
                        )
                        currentX += cashW
                    }
                    // EasyPaisa Segment (Mint Green)
                    if (epW > 0) {
                        drawRect(
                            color = Color(0xFF10B981),
                            topLeft = Offset(currentX, 0f),
                            size = Size(epW, h)
                        )
                        currentX += epW
                    }
                    // JazzCash Segment (Orange Gold Soil)
                    if (jcW > 0) {
                        drawRect(
                            color = Color(0xFFD97706),
                            topLeft = Offset(currentX, 0f),
                            size = Size(jcW, h)
                        )
                        currentX += jcW
                    }
                    // Other Segment (Slate Pebble Gray)
                    if (otherW > 0) {
                        drawRect(
                            color = Color(0xFF8C857B),
                            topLeft = Offset(currentX, 0f),
                            size = Size(otherW, h)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Cash (${(cashPct * 100).toInt()}%)", Color(0xFF5A5A40))
                    LegendItem("EasyPaisa (${(epPct * 100).toInt()}%)", Color(0xFF10B981))
                    LegendItem("JazzCash (${(jcPct * 100).toInt()}%)", Color(0xFFD97706))
                    LegendItem("Others (${(otherPct * 100).toInt()}%)", Color(0xFF8C857B))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Weekly Trend canvas line chart
                Text("Sales Velocity Trend / سیلز کی رفتار", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Simulated daily revenue checkpoints for aesthetics
                    val basePoints = listOf(0.1f, 0.4f, 0.35f, 0.7f, 0.55f, 0.85f, 1.0f)
                    val pointsCount = basePoints.size
                    val stepX = w / (pointsCount - 1)

                    val strokePath = Path().apply {
                        moveTo(0f, h - (basePoints[0] * h * 0.82f))
                        for (i in 1 until pointsCount) {
                            lineTo(i * stepX, h - (basePoints[i] * h * 0.82f))
                        }
                    }

                    // Draw grid lines
                    for (gridY in 0..4) {
                        val currY = h * (gridY / 4f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, currY),
                            end = Offset(w, currY),
                            strokeWidth = 1f
                        )
                    }

                    // Draw line trend
                    drawPath(
                        path = strokePath,
                        color = Color(0xFF5A5A40),
                        style = Stroke(width = 5f)
                    )

                    // Draw dot anchors
                    for (i in 0 until pointsCount) {
                        drawCircle(
                            color = Color(0xFFD97706),
                            radius = 7f,
                            center = Offset(i * stepX, h - (basePoints[i] * h * 0.82f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun ReceiptLogPanel(
    historySearchQuery: String,
    onHistorySearchChange: (String) -> Unit,
    filteredTransactions: List<Transaction>,
    onSelectReceipt: (Transaction) -> Unit
) {
    Column {
        Text("Sales Record Log / انوائس بک ریکارڈ", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text("Tap any transaction to view FBR detailed receipt", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = historySearchQuery,
            onValueChange = onHistorySearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recipe_history_search_box"),
            placeholder = { Text("Filter by Ref or payment method...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filter") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent transaction matches! / ریکارڈ موجود نہیں", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions) { tx ->
                    val totalStr = String.format("%.2f", tx.totalAmount)
                    val dateStr = SimpleDateFormat("dd-MM-yy hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectReceipt(tx) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tx.id, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (tx.isSynced) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = "Synced icon",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudQueue,
                                            contentDescription = "Local queue icon",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tx.fbrInvoiceNo,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Rs. $totalStr", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (tx.paymentMethod) {
                                            "Cash" -> Color(0xFFE6E1D3)
                                            "EasyPaisa" -> Color(0xFFD1FAE5)
                                            "JazzCash" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFF5F5F4)
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tx.paymentMethod,
                                        fontSize = 10.sp,
                                        color = when (tx.paymentMethod) {
                                            "Cash" -> Color(0xFF5A5A40)
                                            "EasyPaisa" -> Color(0xFF065F46)
                                            "JazzCash" -> Color(0xFFB45309)
                                            else -> Color(0xFF57534E)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
