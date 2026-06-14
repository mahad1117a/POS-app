package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.models.CartItem
import com.example.data.models.Product
import com.example.data.models.Transaction
import com.example.data.repository.PosRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val cartListType = Types.newParameterizedType(List::class.java, CartItem::class.java)
    private val cartAdapter = moshi.adapter<List<CartItem>>(cartListType)

    val products: StateFlow<List<Product>>
    val transactions: StateFlow<List<Transaction>>
    val totalRevenue: StateFlow<Double?>
    val totalTaxCollected: StateFlow<Double?>

    // Cart Management
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Sales Tax Rate (Pakistan FBR Standard: 18%)
    private val _taxRate = MutableStateFlow(18.0)
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    // Payment Methods
    private val _paymentMethod = MutableStateFlow("Cash")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    // Customer details
    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    // Received Money input (For cash helper return)
    private val _receivedAmount = MutableStateFlow("")
    val receivedAmount: StateFlow<String> = _receivedAmount.asStateFlow()

    // Cloud syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage.asSharedFlow()

    // Active Checkout State
    private val _checkoutSuccess = MutableSharedFlow<Transaction>()
    val checkoutSuccess: SharedFlow<Transaction> = _checkoutSuccess.asSharedFlow()

    // UI Toast Messages or notifications
    private val _uiNotification = MutableSharedFlow<String>()
    val uiNotification: SharedFlow<String> = _uiNotification.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PosRepository(db.productDao(), db.transactionDao())

        products = repository.allProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        totalRevenue = repository.totalRevenue.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

        totalTaxCollected = repository.totalTaxCollected.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // --- Cart Actions ---
    fun addProductToCart(product: Product) {
        if (product.stock <= 0) {
            viewModelScope.launch {
                _uiNotification.emit("Stock runs empty for ${product.name}! / اسٹاک ختم ہو گیا ہے")
            }
            return
        }

        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.productId == product.id }

        if (existingIndex != -1) {
            val existingItem = currentList[existingIndex]
            if (existingItem.quantity + 1 > product.stock) {
                viewModelScope.launch {
                    _uiNotification.emit("Maximum available stock reached! / بقیہ اسٹاک حد سے بڑھ رہا ہے")
                }
                return
            }
            currentList[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            currentList.add(
                CartItem(
                    productId = product.id,
                    name = product.name,
                    urduName = product.urduName,
                    price = product.price,
                    quantity = 1
                )
            )
        }
        _cart.value = currentList
    }

    fun incrementCartQty(item: CartItem, availableProducts: List<Product>) {
        val targetProduct = availableProducts.find { it.id == item.productId } ?: return
        if (item.quantity + 1 > targetProduct.stock) {
            viewModelScope.launch {
                _uiNotification.emit("Cannot exceed available stock (${targetProduct.stock})")
            }
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == item.productId }
        if (index != -1) {
            currentList[index] = item.copy(quantity = item.quantity + 1)
            _cart.value = currentList
        }
    }

    fun decrementCartQty(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == item.productId }
        if (index != -1) {
            val element = currentList[index]
            if (element.quantity > 1) {
                currentList[index] = element.copy(quantity = element.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _cart.value = currentList
        }
    }

    fun removeCartItem(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        currentList.removeAll { it.productId == item.productId }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
        _receivedAmount.value = ""
        _customerPhone.value = ""
    }

    // --- Detail Configs ---
    fun setTaxRate(rate: Double) {
        _taxRate.value = rate
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        // If modern dynamic payments, autofill received cash since change isn't needed
        if (method != "Cash") {
            val total = getCartTotal()
            _receivedAmount.value = String.format("%.2f", total)
        } else {
            _receivedAmount.value = ""
        }
    }

    fun setCustomerPhone(phone: String) {
        _customerPhone.value = phone
    }

    fun setReceivedAmount(value: String) {
        _receivedAmount.value = value
    }

    fun addCashShortcut(amount: Int) {
        val currentStr = _receivedAmount.value
        val currentDouble = currentStr.toDoubleOrNull() ?: 0.0
        val newValue = currentDouble + amount
        _receivedAmount.value = String.format("%.0f", newValue)
    }

    // --- Calculated state ---
    fun getCartSubtotal(): Double {
        return _cart.value.sumOf { it.price * it.quantity }
    }

    fun getCartTaxAmount(): Double {
        val subtotal = getCartSubtotal()
        return subtotal * (_taxRate.value / 100.0)
    }

    fun getCartTotal(): Double {
        return getCartSubtotal() + getCartTaxAmount()
    }

    fun getChangeReturnAmount(): Double {
        val total = getCartTotal()
        val received = _receivedAmount.value.toDoubleOrNull() ?: 0.0
        return if (received > total) received - total else 0.0
    }

    // --- Inventory/Product Custom Actions ---
    fun addNewProduct(name: String, urduName: String, price: Double, stock: Int, category: String, barcode: String?) {
        viewModelScope.launch {
            if (name.isBlank() || price <= 0.0 || stock < 0) {
                _uiNotification.emit("Please fill required fields properly. / براہ کرم درست معلومات درج کریں۔")
                return@launch
            }
            val product = Product(
                name = name.trim(),
                urduName = urduName.trim().ifEmpty { name },
                price = price,
                stock = stock,
                category = category,
                barcode = barcode?.trim()?.ifEmpty { null }
            )
            repository.insertProduct(product)
            _uiNotification.emit("Added product: $name to database / پروڈکٹ شامل کر دی گئی")
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _uiNotification.emit("Product deleted from inventory / پروڈکٹ خارج کر دی گئی")
        }
    }

    // --- Checkout logic ---
    fun processCheckout() {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) {
            viewModelScope.launch { _uiNotification.emit("Cart is empty / آپ کی ٹوکری خالی ہے") }
            return
        }

        val total = getCartTotal()
        val receivedStr = _receivedAmount.value
        val receivedVal = receivedStr.toDoubleOrNull() ?: 0.0

        if (_paymentMethod.value == "Cash" && receivedVal < total) {
            viewModelScope.launch {
                _uiNotification.emit("Received amount is less than bill total! / وصول شدہ رقم بل سے کم ہے")
            }
            return
        }

        viewModelScope.launch {
            val subtotal = getCartSubtotal()
            val taxAmount = getCartTaxAmount()
            val change = if (_paymentMethod.value == "Cash") receivedVal - total else 0.0

            // Generate FBR dynamic invoice syntax
            // In Pakistan, POS Integrated Invoice contains a 12 digit format: terminal_id - transaction_seq
            val terminalId = "PK-DUK-991A"
            val seqNumber = String.format("%06d", Random.nextInt(1, 999999))
            val fbrInvoiceNo = "FBR-$terminalId-2026-$seqNumber"

            val itemsString = try {
                cartAdapter.toJson(currentCart) ?: "[]"
            } catch (e: Exception) {
                "[]"
            }

            val transactionId = "TXN-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"

            val newTransaction = Transaction(
                id = transactionId,
                timestamp = System.currentTimeMillis(),
                subtotal = subtotal,
                taxRate = _taxRate.value,
                taxAmount = taxAmount,
                totalAmount = total,
                paymentMethod = _paymentMethod.value,
                customerPhone = _customerPhone.value.trim().ifEmpty { null },
                receivedAmount = if (_paymentMethod.value == "Cash") receivedVal else total,
                changeAmount = if (change > 0) change else 0.0,
                fbrInvoiceNo = fbrInvoiceNo,
                itemsJson = itemsString,
                isSynced = false // Pushed to cloud on sync triggers
            )

            repository.checkout(newTransaction, currentCart)
            _checkoutSuccess.emit(newTransaction)
            _uiNotification.emit("Checkout successful! Receipt Generated. / بل کی ادائیگی کامیاب")
            clearCart()
        }
    }

    // --- Real-time Cloud Sync Trigger ---
    fun syncWithCloud() {
        if (_isSyncing.value) return
        _isSyncing.value = true

        viewModelScope.launch {
            try {
                // Simulate real-time secure network handshake with Dukan PK Cloud server
                val uploadedCount = repository.syncTransactionsWithCloud()
                if (uploadedCount > 0) {
                    _syncMessage.emit("Successfully synced $uploadedCount transaction(s) with cloud server! / کلاؤڈ سرور کے ساتھ ریکارڈ ہم آہنگ")
                } else {
                    _syncMessage.emit("All transaction data is fully synced and secure! / تمام ڈیٹا کلاؤڈ پر محفوظ ہے")
                }
            } catch (e: Exception) {
                _syncMessage.emit("Sync failed: Check internet / ہم آہنگی کا عمل ناکام")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
