package com.example.styled

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.styled.ui.screens.ClosetScreen
import com.example.styled.ui.screens.HomeScreen
import com.example.styled.ui.screens.SuggestionsScreen
import com.example.styled.ui.theme.StyledTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            StyledTheme {
                StyledApp(
                    onRequestPermission = { permission ->
                        if (ContextCompat.checkSelfPermission(
                                this,
                                permission
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(permission)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StyledApp(onRequestPermission: (String) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // API keys from BuildConfig or LocalSecrets fallback
    val groqApiKey = if (BuildConfig.GROQ_API_KEY.isNotBlank() && BuildConfig.GROQ_API_KEY != "your-groq-api-key-here") {
        BuildConfig.GROQ_API_KEY
    } else {
        LocalSecrets.GROQ_API_KEY
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Closet"
                        )
                    },
                    label = { Text("Closet") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Suggestions"
                        )
                    },
                    label = { Text("Suggestions") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(modifier = Modifier.padding(innerPadding))
            1 -> ClosetScreen(
                modifier = Modifier.padding(innerPadding),
                groqApiKey = groqApiKey,
                onRequestPermission = onRequestPermission
            )
            2 -> SuggestionsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}