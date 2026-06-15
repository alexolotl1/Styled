package com.example.styled.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class ClothingItem(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val image_url: String,
    val title: String,
    val description: String,
    val category: String,
    val color_primary: String,
    @SerialName("created_at")
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())
) {
    companion object {
        // Valid categories
        val VALID_CATEGORIES = listOf("top", "bottom", "outerwear", "shoes", "accessory")
    }
}

