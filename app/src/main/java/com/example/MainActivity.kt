package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CloudScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    val context = LocalContext.current
    val viewModel: PosViewModel = viewModel()
    val transactions by viewModel.transactions.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val unsyncedCount = transactions.count { !it.isSynced }

    var currentTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to UI warnings & notifications from Viewmodel
    LaunchedEffect(key1 = true) {
        viewModel.uiNotification.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Apni Dukan Cloud POS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "آپنی دوکان ریٹیل سسٹم • Terminal #012PK",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Quick real-time cloud connection banner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = if (unsyncedCount == 0)
                                    Color(0xFFD1FAE5) // bg-emerald-100
                                else
                                    Color(0xFFFDE047).copy(alpha = 0.5f), // soft warm gold/yellow for pending
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF065F46)
                            )
                        } else {
                            Icon(
                                imageVector = if (unsyncedCount == 0) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = "Sync status icon",
                                tint = if (unsyncedCount == 0) Color(0xFF065F46) else Color(0xFF854D0E),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (unsyncedCount == 0) "Cloud Synced" else "$unsyncedCount Unsynced",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (unsyncedCount == 0) Color(0xFF065F46) else Color(0xFF854D0E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Checkout menu") },
                    label = { Text("Checkout / بل", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_tab_register")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { 
                        BadgedBox(badge = {
                            if (unsyncedCount > 0) {
                                Badge { Text("$unsyncedCount") }
                            }
                        }) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Cloud menu") 
                        }
                    },
                    label = { Text("Cloud / کلاؤڈ", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_tab_cloud")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Stock menu") },
                    label = { Text("Stock / اسٹاک", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_tab_inventory")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Reports menu") },
                    label = { Text("Reports / آمدنی", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_tab_reports")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Animating tab screens transitions
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> RegisterScreen(viewModel = viewModel)
                    1 -> CloudScreen(viewModel = viewModel)
                    2 -> InventoryScreen(viewModel = viewModel)
                    3 -> ReportsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
