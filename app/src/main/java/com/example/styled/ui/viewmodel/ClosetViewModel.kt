package com.example.styled.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.styled.data.ClothingItem
import com.example.styled.network.GroqApiClient
import com.example.styled.network.SupabaseClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

data class ClosetUiState(
    val clothingItems: List<ClothingItem> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ClosetViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ClosetUiState())
    val uiState: StateFlow<ClosetUiState> = _uiState.asStateFlow()

    private var userId: String = "default_user"

    init {
        loadClothingItems()
    }

    fun setUserId(userId: String) {
        this.userId = userId
        loadClothingItems()
    }

    fun loadClothingItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = SupabaseClientProvider.getAllClothingItems()
                result.onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        clothingItems = items,
                        isLoading = false
                    )
                }.onFailure { e ->
                    Log.e("ClosetViewModel", "Error loading items", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load clothing items: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("ClosetViewModel", "Error loading items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load clothing items: ${e.message}"
                )
            }
        }
    }

    fun uploadAndAnalyzeImage(
        imageFile: File,
        groqApiKey: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            try {
                // Upload image to Supabase Storage
                val fileName = "clothing-${UUID.randomUUID()}.jpg"
                val uploadResult = SupabaseClientProvider.uploadFile(
                    "clothing-images",
                    fileName,
                    imageFile.readBytes()
                )
                
                uploadResult.onSuccess { imageUrl ->
                    Log.d("ClosetViewModel", "Image uploaded: $imageUrl")
                    
                    // Analyze with Groq
                    viewModelScope.launch {
                        val analysisResult = GroqApiClient.analyzeClothingImage(imageUrl, groqApiKey)
                        
                        analysisResult.onSuccess { clothingItem ->
                            val itemToInsert = clothingItem.copy(
                                id = UUID.randomUUID().toString(),
                                user_id = userId,
                                image_url = imageUrl
                            )
                            
                            // Insert into database
                            SupabaseClientProvider.insertClothingItem(itemToInsert).onSuccess {
                                _uiState.value = _uiState.value.copy(
                                    isUploading = false,
                                    successMessage = "Item added successfully!"
                                )
                                
                                // Reload items
                                loadClothingItems()
                                
                                // Clear success message after 3 seconds
                                viewModelScope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    _uiState.value = _uiState.value.copy(successMessage = null)
                                }
                            }.onFailure { e ->
                                _uiState.value = _uiState.value.copy(
                                    isUploading = false,
                                    error = "Failed to save item: ${e.message}"
                                )
                            }
                        }.onFailure { e ->
                            Log.e("ClosetViewModel", "Error analyzing image", e)
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                error = "Failed to analyze image: ${e.message}"
                            )
                        }
                    }
                }.onFailure { e ->
                    Log.e("ClosetViewModel", "Error uploading image", e)
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Failed to upload image: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("ClosetViewModel", "Error uploading image", e)
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "Failed to upload image: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}


