package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Product
import com.example.ui.viewmodel.PosViewModel

@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isAddPanelOpen by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // Form states
    var prodName by remember { mutableStateOf("") }
    var prodUrduName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodStock by remember { mutableStateOf("") }
    var prodBarcode by remember { mutableStateOf("") }
    var prodCategory by remember { mutableStateOf("Beverages / مشروبات") }

    val categories = listOf(
        "Beverages / مشروبات",
        "Dairy / دودھ دہی",
        "Spices / مصالحہ جات",
        "Cooking / گھی تیل",
        "Snacks / چپس بسکٹ",
        "Household / صابن سرف",
        "Bakery / بیکری",
        "Cosmetics / سنگھار"
    )

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.urduName.contains(searchQuery, ignoreCase = true) ||
        it.barcode?.contains(searchQuery) == true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > 650.dp

        if (isWide) {
            // Tablet Side-by-Side: Left form, Right stock list
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Form
                Card(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Text("Add New Stock / پروڈکٹ رجسٹریشن", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        InventoryForm(
                            name = prodName, onNameChange = { prodName = it },
                            urdu = prodUrduName, onUrduChange = { prodUrduName = it },
                            price = prodPrice, onPriceChange = { prodPrice = it },
                            stock = prodStock, onStockChange = { prodStock = it },
                            barcode = prodBarcode, onBarcodeChange = { prodBarcode = it },
                            category = prodCategory, onCategoryChange = { prodCategory = it },
                            categories = categories,
                            onSubmit = {
                                viewModel.addNewProduct(
                                    prodName, prodUrduName,
                                    prodPrice.toDoubleOrNull() ?: 0.0,
                                    prodStock.toIntOrNull() ?: 0,
                                    prodCategory, prodBarcode
                                )
                                // Reset form
                                prodName = ""
                                prodUrduName = ""
                                prodPrice = ""
                                prodStock = ""
                                prodBarcode = ""
                            }
                        )
                    }
                }

                // Right Panel: Search list
                Column(modifier = Modifier.weight(2.5f)) {
                    InventoryHeaderAndSearch(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onToggleAdd = { isAddPanelOpen = !isAddPanelOpen },
                        isWide = true,
                        count = products.size
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InventoryList(filteredProducts, viewModel, onEditClick = { productToEdit = it }, modifier = Modifier.fillMaxSize())
                }
            }
        } else {
            // Mobile: Expandable top form, bottom stock list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                InventoryHeaderAndSearch(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onToggleAdd = { isAddPanelOpen = !isAddPanelOpen },
                    isWide = false,
                    count = products.size
                )

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(
                    visible = isAddPanelOpen,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Add New Item / پروڈکٹ رجسٹریشن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            InventoryForm(
                                name = prodName, onNameChange = { prodName = it },
                                urdu = prodUrduName, onUrduChange = { prodUrduName = it },
                                price = prodPrice, onPriceChange = { prodPrice = it },
                                stock = prodStock, onStockChange = { prodStock = it },
                                barcode = prodBarcode, onBarcodeChange = { prodBarcode = it },
                                category = prodCategory, onCategoryChange = { prodCategory = it },
                                categories = categories,
                                onSubmit = {
                                    viewModel.addNewProduct(
                                        prodName, prodUrduName,
                                        prodPrice.toDoubleOrNull() ?: 0.0,
                                        prodStock.toIntOrNull() ?: 0,
                                        prodCategory, prodBarcode
                                    )
                                    // Reset form and collapse
                                    prodName = ""
                                    prodUrduName = ""
                                    prodPrice = ""
                                    prodStock = ""
                                    prodBarcode = ""
                                    isAddPanelOpen = false
                                }
                            )
                        }
                    }
                }

                InventoryList(filteredProducts, viewModel, onEditClick = { productToEdit = it }, modifier = Modifier.weight(1f))
            }
        }

        if (productToEdit != null) {
            EditProductDialog(
                product = productToEdit!!,
                categories = categories,
                onSave = { updatedProduct ->
                    viewModel.updateProduct(updatedProduct)
                    productToEdit = null
                },
                onDismiss = { productToEdit = null }
            )
        }
    }
}

@Composable
fun InventoryHeaderAndSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleAdd: () -> Unit,
    isWide: Boolean,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Store Inventory / مال کا اسٹاک", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text("$count Registered items", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }

        if (!isWide) {
            Button(
                onClick = onToggleAdd,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.AddBox, contentDescription = "Toggle add", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REGISTRATION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_search_box"),
        placeholder = { Text("Search by name or barcode / تلاش کریں...", fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
fun InventoryForm(
    name: String, onNameChange: (String) -> Unit,
    urdu: String, onUrduChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    stock: String, onStockChange: (String) -> Unit,
    barcode: String, onBarcodeChange: (String) -> Unit,
    category: String, onCategoryChange: (String) -> Unit,
    categories: List<String>,
    onSubmit: () -> Unit
) {
    var expandedCat by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Product name (English)*", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_prod_name_input"),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )

        TextField(
            value = urdu,
            onValueChange = onUrduChange,
            placeholder = { Text("پروڈکٹ کا نام (Urdu)", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_prod_urdu_input"),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = price,
                onValueChange = onPriceChange,
                placeholder = { Text("Price (Rs.)*", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .testTag("new_prod_price_input"),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )

            TextField(
                value = stock,
                onValueChange = onStockChange,
                placeholder = { Text("Stock Level*", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("new_prod_stock_input"),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )
        }

        TextField(
            value = barcode,
            onValueChange = onBarcodeChange,
            placeholder = { Text("Barcode / بارکوڈ (Optional)", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
        )

        // Category dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedCat = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                }
            }
            DropdownMenu(
                expanded = expandedCat,
                onDismissRequest = { expandedCat = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat, fontSize = 12.sp) },
                        onClick = {
                            onCategoryChange(cat)
                            expandedCat = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("commit_add_product_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save")
            Spacer(modifier = Modifier.width(6.dp))
            Text("REGISTER PRODUCT / شامل کریں", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun InventoryList(
    filteredProducts: List<Product>,
    viewModel: PosViewModel,
    onEditClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filteredProducts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No registered inventory items! / اسٹاک ریکارڈ خالی ہے", textAlign = TextAlign.Center, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (item.barcode != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "[Bar: ${item.barcode}]",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Text(item.urduName, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = item.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Stock counts right hand
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${item.price}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Badge(
                                    containerColor = if (item.stock == 0) Color.Red else if (item.stock < 5) Color.Yellow else Color(0xFF15803D)
                                ) {
                                    Text(
                                        text = if (item.stock == 0) "Out of Stock" else "Stock: ${item.stock}",
                                        color = if (item.stock == 0 || item.stock >= 5) Color.White else Color.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onEditClick(item) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit product", modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteProduct(item.id) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete product", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: Product,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit,
    categories: List<String>
) {
    var nameState by remember { mutableStateOf(product.name) }
    var urduState by remember { mutableStateOf(product.urduName) }
    var priceState by remember { mutableStateOf(product.price.toString()) }
    var stockState by remember { mutableStateOf(product.stock.toString()) }
    var barcodeState by remember { mutableStateOf(product.barcode ?: "") }
    var categoryState by remember { mutableStateOf(product.category) }
    var expandedCat by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = priceState.toDoubleOrNull()
                    val stockVal = stockState.toIntOrNull()
                    if (nameState.trim().isBlank()) {
                        errorText = "Product name cannot be empty"
                    } else if (priceVal == null || priceVal <= 0.0) {
                        errorText = "Enter a valid price"
                    } else if (stockVal == null || stockVal < 0) {
                        errorText = "Enter a valid stock level"
                    } else {
                        onSave(
                            product.copy(
                                name = nameState.trim(),
                                urduName = urduState.trim().ifEmpty { nameState.trim() },
                                price = priceVal,
                                stock = stockVal,
                                barcode = barcodeState.trim().ifEmpty { null },
                                category = categoryState
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes / ٹھیک ہے", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel / منسوخ", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Product & Restock / ترمیم پروڈکٹ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Product Name (English)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = urduState,
                    onValueChange = { urduState = it },
                    label = { Text("پروڈکٹ کا نام (Urdu)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceState,
                        onValueChange = { priceState = it },
                        label = { Text("Price (Rs.)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = stockState,
                        onValueChange = { stockState = it },
                        label = { Text("Stock / اسٹاک", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = barcodeState,
                    onValueChange = { barcodeState = it },
                    label = { Text("Barcode (Optional)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCat = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(categoryState, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontSize = 12.sp) },
                                onClick = {
                                    categoryState = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 5.dp
    )
}
