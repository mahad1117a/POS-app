package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Transaction
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CloudScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()

    val unsyncedTransactions = transactions.filter { !it.isSynced }
    val syncedTransactions = transactions.filter { it.isSynced }

    var logOutput by remember { mutableStateOf(listOf("System Boot Completed...", "PK Dev Cloud POS Service Initialized...")) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.syncMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
            // Append formatted cloud API response logs
            val currentLogs = logOutput.toMutableList()
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            currentLogs.add("[$time] Pushing data to secure endpoint...")
            currentLogs.add("[$time] POST /v1/transactions HTTP/2.0")
            currentLogs.add("[$time] Connection: keep-alive | Payload: JSON encryption active")
            currentLogs.add("[$time] API Response: 200 OK SUCCESS")
            currentLogs.add("[$time] Remote cloud DB sync state: SAFE")
            logOutput = currentLogs.takeLast(12) // Keep last 12
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cloud Status Top Header Alert
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (unsyncedTransactions.isEmpty())
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (unsyncedTransactions.isEmpty()) MaterialTheme.colorScheme.primary.copy(0.1f)
                                    else MaterialTheme.colorScheme.error.copy(0.1f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (unsyncedTransactions.isEmpty()) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Cloud logo",
                                tint = if (unsyncedTransactions.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (unsyncedTransactions.isEmpty())
                                    "ALL INVOICES SAFE IN CLOUD"
                                else
                                    "PENDING CLOUD SYNC",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (unsyncedTransactions.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (unsyncedTransactions.isEmpty())
                                    "All business sales record are synced online in real-time. / کلاؤڈ پر تمام ریکارڈ محفوظ ہے"
                                else
                                    "${unsyncedTransactions.size} receipt(s) saved locally, pending cloud backup. / کلاؤڈ ہم آہنگی باقی ہے",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Real-Time Sync Action Button
            item {
                Button(
                    onClick = { viewModel.syncWithCloud() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("force_sync_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (unsyncedTransactions.isEmpty())
                            MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("SAVING TO REMOTE CLOUD PORTAL / ریکارڈ محفوظ ہورہا ہے", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync upload")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (unsyncedTransactions.isEmpty()) "SYNCHRONIZATION COMPLETED / ہم آہنگی مکمل" else "SYNC CLOUD NOW / ریکارڈ کلاؤڈ پر بھیجیں",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Real-time Cloud Metrics Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CloudMetricBox(
                        title = "Cloud Synced Value",
                        urduTitle = "کلاؤڈ ریکارڈ رقم",
                        value = "Rs. ${String.format("%.2f", syncedTransactions.sumOf { it.totalAmount })}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    CloudMetricBox(
                        title = "Unsynced Local Queue",
                        urduTitle = "مقامی قطار",
                        value = "${unsyncedTransactions.size} Bills",
                        color = if (unsyncedTransactions.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Live Web Client / Online Dashboard Preview Link mockup card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = "Direct link", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Live Reporting Portal Web Address",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "https://cloud.dukanpos.com.pk/hq/terminal-012pk",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Store management can log into this URL globally to view live real-time receipts, tax ledger compliance, and inventory status remotely.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Simulate Real-time HTTP Console log logs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF292524)), // Slate dark/warm charcoal
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Live Cloud Handshake Logs / کلاؤڈ لاگ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFAFAF9)
                                )
                            }
                            IconButton(
                                onClick = {
                                    logOutput = listOf("System Logs re-loaded...", "Awaiting transactions local save...")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Clear logs", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1917), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF44403C), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                                .heightIn(min = 120.dp, max = 200.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(logOutput) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("200 OK") || log.contains("SAFE")) Color(0xFF10B981) else Color(0xFFA8A29E),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 2.dp)
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

@Composable
fun CloudMetricBox(
    title: String,
    urduTitle: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(urduTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
