package com.example.styled.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.styled.data.ClothingItem
import com.example.styled.data.OutfitSuggestion
import com.example.styled.data.ShoppingSuggestion
import com.example.styled.ui.viewmodel.SuggestionsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    modifier: Modifier = Modifier,
    viewModel: SuggestionsViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab = remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.value.error) {
        uiState.value.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suggestions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (selectedTab.value == 0) {
                                viewModel.regenerateOutfits()
                            } else {
                                viewModel.regenerateShopping()
                            }
                        },
                        enabled = !uiState.value.isRegenerating
                    ) {
                        if (uiState.value.isRegenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate suggestions"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Tab Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab.value == 0,
                    onClick = { selectedTab.value = 0 },
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    label = { Text("Outfits") }
                )
                SegmentedButton(
                    selected = selectedTab.value == 1,
                    onClick = { selectedTab.value = 1 },
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    label = { Text("Shopping") }
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    uiState.value.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    selectedTab.value == 0 -> {
                        OutfitsTab(
                            outfits = uiState.value.outfitSuggestions,
                            clothingItems = uiState.value.clothingItems,
                            isEmpty = uiState.value.outfitSuggestions.isEmpty()
                        )
                    }
                    selectedTab.value == 1 -> {
                        ShoppingTab(
                            suggestions = uiState.value.shoppingSuggestions,
                            isEmpty = uiState.value.shoppingSuggestions.isEmpty()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OutfitsTab(
    outfits: List<OutfitSuggestion>,
    clothingItems: Map<String, ClothingItem>,
    isEmpty: Boolean
) {
    if (isEmpty) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add clothes to get suggestions!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(outfits) { outfit ->
                OutfitSimpleCard(outfit, clothingItems)
            }
        }
    }
}

@Composable
fun OutfitSimpleCard(
    outfit: OutfitSuggestion,
    clothingItems: Map<String, ClothingItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Outfit Name
            Text(
                text = outfit.outfit_name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Outfit Description
            if (outfit.outfit_description.isNotEmpty()) {
                Text(
                    text = outfit.outfit_description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Bigger Item Images
            if (outfit.item_ids.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(outfit.item_ids) { itemId ->
                        val item = clothingItems[itemId]
                        if (item != null) {
                            Box(
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.LightGray)
                            ) {
                                AsyncImage(
                                    model = item.image_url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Accessory Suggestions
            if (outfit.accessory_suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✨ Accessories & Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = outfit.accessory_suggestions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingTab(
    suggestions: List<ShoppingSuggestion>,
    isEmpty: Boolean
) {
    val context = LocalContext.current

    if (isEmpty) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add clothes to get suggestions!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(suggestions) { suggestion ->
                ShoppingCard(
                    suggestion = suggestion,
                    onLinkClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
        }
    }
}

@Composable
fun ShoppingCard(
    suggestion: ShoppingSuggestion,
    onLinkClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onLinkClick(suggestion.search_url) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.item_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(6.dp, 4.dp)
                ) {
                    Text(
                        text = suggestion.suggested_store,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(6.dp, 4.dp)
                ) {
                    Text(
                        text = suggestion.price_range,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}

