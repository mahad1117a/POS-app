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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.CartItem
import com.example.data.models.Product
import com.example.data.models.Transaction
import com.example.ui.viewmodel.PosViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val selectedPaymentMethod by viewModel.paymentMethod.collectAsState()
    val receivedAmount by viewModel.receivedAmount.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var barcodeSearchQuery by remember { mutableStateOf("") }
    var showReceiptDialog by remember { mutableStateOf<Transaction?>(null) }
    var isPhoneError by remember { mutableStateOf(false) }

    // Listen for successful checkouts to pop up receipt
    LaunchedEffect(key1 = true) {
        viewModel.checkoutSuccess.collectLatest { tx ->
            showReceiptDialog = tx
        }
    }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.urduName.contains(searchQuery, ignoreCase = true) ||
        it.category.contains(searchQuery, ignoreCase = true)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 680.dp

        if (isWideScreen) {
            // Tablet / Desktop Dual-Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Cart & Checkout Setup (Width 40%)
                Card(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    CartPane(
                        cart = cart,
                        viewModel = viewModel,
                        products = products,
                        taxRate = taxRate,
                        selectedPaymentMethod = selectedPaymentMethod,
                        receivedAmount = receivedAmount,
                        customerPhone = customerPhone,
                        isPhoneError = isPhoneError,
                        onPhoneChange = {
                            isPhoneError = it.isNotEmpty() && !it.startsWith("03") && !it.startsWith("+92")
                            viewModel.setCustomerPhone(it)
                        }
                    )
                }

                // Right Panel: Product catalog & scanner (Width 60%)
                Column(
                    modifier = Modifier
                        .weight(2.2f)
                        .fillMaxHeight()
                ) {
                    ProductCatalogPane(
                        filteredProducts = filteredProducts,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        barcodeSearchQuery = barcodeSearchQuery,
                        onBarcodeChange = { barcodeSearchQuery = it },
                        products = products,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Normal Mobile Grid Layout using a scrollable stack
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                ProductCatalogPane(
                    filteredProducts = filteredProducts,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    barcodeSearchQuery = barcodeSearchQuery,
                    onBarcodeChange = { barcodeSearchQuery = it },
                    products = products,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1.1f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(
                    visible = cart.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.weight(1.9f)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        CartPane(
                            cart = cart,
                            viewModel = viewModel,
                            products = products,
                            taxRate = taxRate,
                            selectedPaymentMethod = selectedPaymentMethod,
                            receivedAmount = receivedAmount,
                            customerPhone = customerPhone,
                            isPhoneError = isPhoneError,
                            onPhoneChange = {
                                isPhoneError = it.isNotEmpty() && !it.startsWith("03") && !it.startsWith("+92")
                                viewModel.setCustomerPhone(it)
                            }
                        )
                    }
                }

                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(0.3f)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛒 Tap on items to register client cart / بل بنانے کیلئے چیزوں پر ٹیپ کریں",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Receipt pop-up Dialog
    showReceiptDialog?.let { transaction ->
        ReceiptDialog(
            transaction = transaction,
            onClose = {
                showReceiptDialog = null
            }
        )
    }
}

@Composable
fun ProductCatalogPane(
    filteredProducts: List<Product>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    barcodeSearchQuery: String,
    onBarcodeChange: (String) -> Unit,
    products: List<Product>,
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Quick Barcode simulation scan bar + general search
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("product_search_input"),
                placeholder = { Text("Search items / تلاش...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Barcode simulator input
            TextField(
                value = barcodeSearchQuery,
                onValueChange = onBarcodeChange,
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("barcode_simulation_input"),
                placeholder = { Text("Barcode / بارکوڈ...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = "Barcode icon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (barcodeSearchQuery.isNotEmpty()) {
                        val found = products.find { it.barcode == barcodeSearchQuery }
                        if (found != null) {
                            viewModel.addProductToCart(found)
                            onBarcodeChange("")
                        } else {
                            // Quick simulation feedback
                            onBarcodeChange("")
                        }
                    }
                },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick simulation helper barcodes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Simulate scan: ", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
            listOf("1001", "1002", "1004", "1006").forEach { bar ->
                Card(
                    modifier = Modifier.clickable {
                        val p = products.find { it.barcode == bar }
                        if (p != null) viewModel.addProductToCart(p)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Bar $bar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Empty icon",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No inventory matching search! / اسٹاک دستیاب نہیں",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addProductToCart(product) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (product.stock == 0)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = product.urduName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(product.category.split(" / ")[0], fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                    if (product.stock <= 5) {
                                        Text(
                                            "Only ${product.stock} left",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            "Stock: ${product.stock}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rs. ${product.price}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { viewModel.addProductToCart(product) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add quantity", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartPane(
    cart: List<CartItem>,
    viewModel: PosViewModel,
    products: List<Product>,
    taxRate: Double,
    selectedPaymentMethod: String,
    receivedAmount: String,
    customerPhone: String,
    isPhoneError: Boolean,
    onPhoneChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Basket / کارٹ", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("${cart.size} items", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            TextButton(
                onClick = { viewModel.clearCart() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear cart")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear / خالی", fontSize = 12.sp)
            }
        }

        // Cart items scrollable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cart) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(item.urduName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Rs. ${item.price} each", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }

                    // Qty selectors
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { viewModel.decrementCartQty(item) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "${item.quantity}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.incrementCartQty(item, products) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }

                    // Final item row total
                    Text(
                        text = "Rs. ${item.price * item.quantity}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        }

        // Taxes compliance selectors (18% standard, 15% services, 0% Tax-Free)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "FBR Sales Tax / سیلز ٹیکس (GST):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${taxRate}%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(18.0, 15.0, 0.0).forEach { rate ->
                        FilterChip(
                            selected = taxRate == rate,
                            onClick = { viewModel.setTaxRate(rate) },
                            label = {
                                Text(
                                    text = if (rate == 0.0) "Tax-Free" else "${rate.toInt()}% GST",
                                    fontSize = 10.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Payment and customer setup form
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = customerPhone,
                onValueChange = onPhoneChange,
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("customer_phone_input"),
                placeholder = { Text("Customer Cell / فون نمبر", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone icon", modifier = Modifier.size(16.dp)) },
                singleLine = true,
                isError = isPhoneError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Dynamic Payments
            Box(modifier = Modifier.weight(1f)) {
                var expandedPay by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { expandedPay = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedPaymentMethod, fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = expandedPay,
                    onDismissRequest = { expandedPay = false }
                ) {
                    listOf("Cash", "EasyPaisa", "JazzCash", "SadaPay", "NayaPay", "Bank Transfer", "Card").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.setPaymentMethod(option)
                                expandedPay = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Cash Input setup (Hidden if using digital direct payments)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedPaymentMethod == "Cash") {
                TextField(
                    value = receivedAmount,
                    onValueChange = { viewModel.setReceivedAmount(it) },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("cash_received_input"),
                    placeholder = { Text("Cash Received / وصول شدہ", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = "Currency icon", modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Gateway: Realtime cloud payment confirmation / کلاؤڈ تصدیق",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Calculations panel Right Hand
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Subtotal: Rs. ${String.format("%.2f", viewModel.getCartSubtotal())}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
                Text(
                    text = "GST Tax: Rs. ${String.format("%.2f", viewModel.getCartTaxAmount())}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
                Text(
                    text = "Total: Rs. ${String.format("%.2f", viewModel.getCartTotal())}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Pakistani banknote shortcut grid for cashless ease
        if (selectedPaymentMethod == "Cash") {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(10, 50, 100, 500, 1000, 5000).forEach { note ->
                    AssistChip(
                        onClick = { viewModel.addCashShortcut(note) },
                        label = { Text("₨ $note", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Change Return Indicator (only for cash payments)
        if (selectedPaymentMethod == "Cash" && receivedAmount.isNotEmpty()) {
            val change = viewModel.getChangeReturnAmount()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (change > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Change Return / بقایا راقم (Zaid Raqam):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (change > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                )
                Text(
                    "Rs. ${String.format("%.2f", change)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (change > 0) MaterialTheme.colorScheme.primary else Color.Red
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // BIG PAY BUTTON
        Button(
            onClick = { viewModel.processCheckout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("checkout_commit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.DoneOutline, contentDescription = "Pay")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "COMPLETE CHECKOUT / بل ادا کریں",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ReceiptDialog(
    transaction: Transaction,
    onClose: () -> Unit
) {
    val dateString = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))

    // Parse the itemsJson back to List<CartItem>
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val cartListType = Types.newParameterizedType(List::class.java, CartItem::class.java)
    val adapter = moshi.adapter<List<CartItem>>(cartListType)
    val items = try {
        adapter.fromJson(transaction.itemsJson) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thermal Receipt header styling
                Text(
                    "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    "BISMILLAH APNI DUKAN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Integrated POS Retail Terminal #012PK",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Text(
                    "G11 Markaz, Islamabad, Pakistan",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dotted break line
                DottedLine()

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Rows
                ReceiptMetadataRow(label = "Invoice Ref:", value = transaction.id)
                ReceiptMetadataRow(label = "Date/Time:", value = dateString)
                ReceiptMetadataRow(label = "Payment By:", value = transaction.paymentMethod)
                transaction.customerPhone?.let {
                    ReceiptMetadataRow(label = "Client Cell (SMS):", value = it)
                }

                Spacer(modifier = Modifier.height(10.dp))
                DottedLine()
                Spacer(modifier = Modifier.height(8.dp))

                // Shopping Rows
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                item.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                item.urduName,
                                fontSize = 11.sp,
                                color = Color.Blue
                            )
                            Text(
                                "${item.quantity} Qty x Rs. ${item.price}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            "Rs. ${item.price * item.quantity}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                DottedLine()
                Spacer(modifier = Modifier.height(8.dp))

                // Totals
                ReceiptTotalRow(label = "Subtotal:", value = "Rs. ${String.format("%.2f", transaction.subtotal)}")
                ReceiptTotalRow(
                    label = "FBR GST (${transaction.taxRate.toInt()}%):",
                    value = "Rs. ${String.format("%.2f", transaction.taxAmount)}"
                )
                ReceiptTotalRow(
                    label = "GRAND TOTAL:",
                    value = "Rs. ${String.format("%.2f", transaction.totalAmount)}",
                    isGrand = true
                )

                if (transaction.paymentMethod == "Cash") {
                    ReceiptTotalRow(label = "Paid Amount:", value = "Rs. ${String.format("%.2f", transaction.receivedAmount)}")
                    ReceiptTotalRow(label = "Change Return:", value = "Rs. ${String.format("%.2f", transaction.changeAmount)}")
                }

                Spacer(modifier = Modifier.height(12.dp))
                DottedLine()
                Spacer(modifier = Modifier.height(8.dp))

                // FBR Official Compliance Seal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "FBR VERIFIED INVOICE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF14532D) // Dark Spruce green
                        )
                        Text(
                            transaction.fbrInvoiceNo,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }

                    // Scannable QR Code simulation
                    Canvas(
                        modifier = Modifier
                            .size(45.dp)
                            .padding(4.dp)
                    ) {
                        // Drawing simulated QR Code dots
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val gridSize = 5
                        val tileW = canvasWidth / gridSize
                        val tileH = canvasHeight / gridSize

                        // Generate stable random tiles based on invoice string
                        val r = kotlin.random.Random(transaction.id.hashCode() + 10)

                        for (i in 0 until gridSize) {
                            for (j in 0 until gridSize) {
                                // Corners are solid block lookups
                                val isCorner = (i < 2 && j < 2) || (i > 2 && j < 2) || (i < 2 && j > 2)
                                if (isCorner || r.nextBoolean()) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(i * tileW, j * tileH),
                                        size = androidx.compose.ui.geometry.Size(tileW, tileH)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "شکریہ! پاکستان کا ٹیکس سسٹم مضبوط بنائیں",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Proudly Synced online in Real-time",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CLOSE RECEIPT / رسید بند کریں", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ReceiptMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun ReceiptTotalRow(label: String, value: String, isGrand: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = if (isGrand) 14.sp else 11.sp,
            fontWeight = if (isGrand) FontWeight.ExtraBold else FontWeight.Bold,
            color = Color.Black
        )
        Text(
            value,
            fontSize = if (isGrand) 14.sp else 11.sp,
            fontWeight = if (isGrand) FontWeight.ExtraBold else FontWeight.ExtraBold,
            color = if (isGrand) Color(0xFF15803D) else Color.Black
        )
    }
}

@Composable
fun DottedLine() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        this.drawLine(
            color = Color.Gray,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}
