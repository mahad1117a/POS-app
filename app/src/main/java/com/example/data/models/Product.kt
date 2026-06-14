package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val urduName: String,
    val price: Double,
    val stock: Int,
    val category: String,
    val barcode: String? = null
)

data class CartItem(
    val productId: Int,
    val name: String,
    val urduName: String,
    val price: Double,
    val quantity: Int
)
