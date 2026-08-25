package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.UserProfileGlassComponent
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel

@Composable
fun ProfileWalletTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UserProfileGlassComponent(
                viewModel = viewModel,
                onNavigateToSettings = {
                    viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.SETTINGS)
                }
            )
        }
    }
}

