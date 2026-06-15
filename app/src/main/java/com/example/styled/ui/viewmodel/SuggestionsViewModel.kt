package com.example.styled.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.styled.BuildConfig
import com.example.styled.LocalSecrets
import com.example.styled.data.ClothingItem
import com.example.styled.data.OutfitSuggestion
import com.example.styled.data.ShoppingSuggestion
import com.example.styled.network.GroqApiClient
import com.example.styled.network.SupabaseClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SuggestionsUiState(
    val outfitSuggestions: List<OutfitSuggestion> = emptyList(),
    val shoppingSuggestions: List<ShoppingSuggestion> = emptyList(),
    val clothingItems: Map<String, ClothingItem> = emptyMap(),
    val isLoading: Boolean = false,
    val isRegenerating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SuggestionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SuggestionsUiState())
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    private var userId: String = "default_user"

    init {
        loadClothingItemsAndGenerateSuggestions()
    }

    fun setUserId(userId: String) {
        this.userId = userId
        loadClothingItemsAndGenerateSuggestions()
    }

    fun loadClothingItemsAndGenerateSuggestions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = SupabaseClientProvider.getAllClothingItems()
            result.onSuccess { items ->
                _uiState.value = _uiState.value.copy(
                    clothingItems = items.associateBy { it.id }
                )
                generateOutfitSuggestions(items)
                generateShoppingSuggestions(items)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load items"
                )
            }
        }
    }

    private fun generateOutfitSuggestions(items: List<ClothingItem>) {
        viewModelScope.launch {
            val groqApiKey = if (BuildConfig.GROQ_API_KEY.isNotBlank() && BuildConfig.GROQ_API_KEY != "your-groq-api-key-here") {
                BuildConfig.GROQ_API_KEY
            } else {
                LocalSecrets.GROQ_API_KEY
            }

            if (groqApiKey.isBlank()) return@launch

            val result = GroqApiClient.generateOutfitSuggestions(items, groqApiKey)
            result.onSuccess { suggestions ->
                _uiState.value = _uiState.value.copy(outfitSuggestions = suggestions)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun generateShoppingSuggestions(items: List<ClothingItem>) {
        viewModelScope.launch {
            val groqApiKey = if (BuildConfig.GROQ_API_KEY.isNotBlank() && BuildConfig.GROQ_API_KEY != "your-groq-api-key-here") {
                BuildConfig.GROQ_API_KEY
            } else {
                LocalSecrets.GROQ_API_KEY
            }

            if (groqApiKey.isBlank()) return@launch

            val result = GroqApiClient.generateShoppingSuggestions(items, groqApiKey)
            result.onSuccess { suggestions ->
                _uiState.value = _uiState.value.copy(
                    shoppingSuggestions = suggestions,
                    isLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun regenerateOutfits() {
        if (_uiState.value.clothingItems.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(isRegenerating = true)
            generateOutfitSuggestions(_uiState.value.clothingItems.values.toList())
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

