package com.example.styled.data

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingSuggestion(
    val item_name: String,
    val reason: String,
    val suggested_store: String,
    val search_url: String,
    val price_range: String
)

