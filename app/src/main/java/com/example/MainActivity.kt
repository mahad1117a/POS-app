package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val shopName by viewModel.shopName.collectAsState()

    val unsyncedCount = transactions.count { !it.isSynced }

    var currentTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val shopConfigured by viewModel.shopConfigured.collectAsState()
    var showShopNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(shopConfigured) {
        if (!shopConfigured) {
            showShopNameDialog = true
        }
    }

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
                    Column(
                        modifier = Modifier
                            .then(
                                if (!shopConfigured) {
                                    Modifier.clickable { showShopNameDialog = true }
                                } else Modifier
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (shopName.isNotEmpty()) shopName else "Apni Dukan Cloud POS",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (!shopConfigured) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit store name",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified branch lock",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
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

            if (showShopNameDialog) {
                ShopOnboardingDialog(
                    currentName = shopName,
                    currentLocation = viewModel.shopLocation.collectAsState().value,
                    onSave = { name, location ->
                        viewModel.configureShop(name, location)
                        showShopNameDialog = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOnboardingDialog(
    currentName: String,
    currentLocation: String,
    onSave: (String, String) -> Unit
) {
    var nameState by remember { mutableStateOf(currentName) }
    var locationState by remember { mutableStateOf(currentLocation) }
    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Don't dismiss without completing setup */ },
        confirmButton = {
            Button(
                onClick = {
                    if (nameState.trim().isBlank()) {
                        errorText = "Shop name cannot be empty / دکان کا نام لازمی ہے"
                    } else if (locationState.trim().isBlank()) {
                        errorText = "Shop location cannot be empty / پتہ لکھنا لازمی ہے"
                    } else {
                        onSave(nameState.trim(), locationState.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lock Configuration / محفوظ اور لاک کریں", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Store icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Store (Once) / دکان کی ترتیب",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your Business Name and Address. To ensure business compliance and billing integrity, this configuration cannot be altered later.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = nameState,
                    onValueChange = {
                        nameState = it
                        if (it.trim().isNotEmpty()) errorText = ""
                    },
                    label = { Text("Shop Name / دکان کا نام", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Faisal General Store") },
                    singleLine = true,
                    isError = errorText.contains("name"),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = "Store icon", modifier = Modifier.size(20.dp)) }
                )

                OutlinedTextField(
                    value = locationState,
                    onValueChange = {
                        locationState = it
                        if (it.trim().isNotEmpty()) errorText = ""
                    },
                    label = { Text("Store Location / پتہ", fontSize = 12.sp) },
                    placeholder = { Text("e.g. G11 Markaz, Islamabad") },
                    singleLine = true,
                    isError = errorText.contains("location"),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location icon", modifier = Modifier.size(20.dp)) }
                )
                
                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Suggested Preset Brands:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = listOf(
                        "Faisal Mart" to "G11 Markaz, Islamabad",
                        "Madina Retail" to "DHA Phase 6, Lahore",
                        "Bismillah Store" to "Saddar, Karachi"
                    )
                    suggestions.forEach { (sName, sLoc) ->
                        Card(
                            onClick = {
                                nameState = sName
                                locationState = sLoc
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
