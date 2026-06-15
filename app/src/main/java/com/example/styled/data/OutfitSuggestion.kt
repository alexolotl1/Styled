package com.example.styled.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutfitSuggestion(
    val outfit_name: String,
    val item_ids: List<String>,
    val color_notes: String,
    val layering_notes: String,
    val vibe_notes: String,
    val overall_score: Int,
    val outfit_description: String = "",
    val accessory_suggestions: String = ""
)

