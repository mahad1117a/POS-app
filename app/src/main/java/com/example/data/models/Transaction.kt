package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val subtotal: Double,
    val taxRate: Double, // e.g. 18.0 for standard FBR General Sales Tax (GST)
    val taxAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String, // "Cash", "EasyPaisa", "JazzCash", "SadaPay", "NayaPay", "Bank Transfer", "Card"
    val customerPhone: String?,
    val receivedAmount: Double,
    val changeAmount: Double,
    val fbrInvoiceNo: String, // Pakistan FBR POS integrated invoice simulation
    val itemsJson: String, // JSON list of CartItems
    val isSynced: Boolean = false,
    val syncedAt: Long? = null
)
