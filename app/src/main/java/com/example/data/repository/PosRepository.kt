package com.example.data.repository

import com.example.data.local.ProductDao
import com.example.data.local.TransactionDao
import com.example.data.models.CartItem
import com.example.data.models.Product
import com.example.data.models.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PosRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalRevenue: Flow<Double?> = transactionDao.getTotalRevenueFlow()
    val totalTaxCollected: Flow<Double?> = transactionDao.getTotalTaxCollectedFlow()

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun deleteProduct(productId: Int) {
        productDao.deleteProductById(productId)
    }

    /**
     * Completes a transaction, saves it locally, and updates product stock levels.
     */
    suspend fun checkout(transaction: Transaction, items: List<CartItem>) {
        // Save the transaction
        transactionDao.insertTransaction(transaction)

        // Decrement quantities from stock in the database
        for (item in items) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val newStock = (product.stock - item.quantity).coerceAtLeast(0)
                productDao.updateProductStock(product.id, newStock)
            }
        }
    }

    /**
     * Pre-populates the database with typical Pakistani retail/Karyana grocery store items if empty.
     */
    suspend fun prepopulateIfEmpty() {
        val currentProducts = allProducts.first()
        if (currentProducts.isEmpty()) {
            val initialList = listOf(
                Product(name = "Tapal Danedar Tea 430g", urduName = "ٹاپال دانے دار چائے", price = 650.00, stock = 45, category = "Beverages / مشروبات", barcode = "1001"),
                Product(name = "Everyday Milk Powder 400g", urduName = "ایوری ڈے خشک دودھ", price = 820.00, stock = 30, category = "Dairy / دودھ دہی", barcode = "1002"),
                Product(name = "Shan Biryani Masala Double", urduName = "شان بریانی مصالحہ", price = 150.00, stock = 60, category = "Spices / مصالحہ جات", barcode = "1003"),
                Product(name = "Rooh Afza Syrup 800ml", urduName = "روح افزا شربت", price = 410.00, stock = 25, category = "Beverages / مشروبات", barcode = "1004"),
                Product(name = "Dalda Banaspati Ghee 1kg", urduName = "ڈالڈا گھی", price = 560.00, stock = 35, category = "Cooking / گھی تیل", barcode = "1005"),
                Product(name = "Lays Masala Chips Large", urduName = "لیز چپس بڑا", price = 110.00, stock = 80, category = "Snacks / چپس بسکٹ", barcode = "1006"),
                Product(name = "Sufi Washing Soap Pack", urduName = "صوفی صابن", price = 180.00, stock = 40, category = "Household / صابن سرف", barcode = "1007"),
                Product(name = "National Chilli Powder 200g", urduName = "نیشنل لال مرچ پاؤڈر", price = 240.00, stock = 50, category = "Spices / مصالحہ جات", barcode = "1008"),
                Product(name = "Coca-Cola 1.5 Litre", urduName = "کوکا کولا ڈیڑھ لیٹر", price = 220.00, stock = 100, category = "Beverages / مشروبات", barcode = "1009"),
                Product(name = "Dawn Bread Large Plain", urduName = "ڈان ڈبل روٹی بڑی", price = 250.00, stock = 15, category = "Bakery / بیکری", barcode = "1010"),
                Product(name = "Knorr Soupy Noodles Pack", urduName = "کنور نوڈلز", price = 70.00, stock = 90, category = "Snacks / چپس بسکٹ", barcode = "1011"),
                Product(name = "Sunsilk Shampoo 360ml", urduName = "سن سلک شیمپو", price = 620.00, stock = 22, category = "Cosmetics / سنگھار", barcode = "1012")
            )
            productDao.insertProducts(initialList)
        }
    }

    /**
     * Simulates real-time cloud uploading/sync of local POS transactions.
     * Connects with PK Cloud POS gateway.
     */
    suspend fun syncTransactionsWithCloud(): Int {
        val unsyncedList = transactionDao.getUnsyncedTransactions()
        if (unsyncedList.isEmpty()) return 0

        // Simulate network delay to PK National Retail Cloud Server (cloud.fbr.pos.gov.pk / dukan-cloud-api)
        delay(2000)

        val syncTime = System.currentTimeMillis()
        for (tx in unsyncedList) {
            transactionDao.markAsSynced(tx.id, syncTime)
        }
        return unsyncedList.size
    }
}
