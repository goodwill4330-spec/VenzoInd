package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogueProduct
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CataloguePickerBottomSheet(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit,
    onProductSelected: (CatalogueProduct) -> Unit
) {
    val bColors = LocalBharatColors.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Ready Catalogues, 1: Create Custom Item
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val defaultProducts = remember {
        listOf(
            CatalogueProduct(
                id = "cat_1",
                title = "Wireless ANC Earbuds Pro",
                price = 1499.0,
                originalPrice = 2999.0,
                discountPercent = 50,
                category = "Electronics",
                description = "40dB Active Noise Cancellation, 36hr Battery Backup with Fast Type-C Charging and Quad-Mic HD Calling.",
                badge = "BESTSELLER"
            ),
            CatalogueProduct(
                id = "cat_2",
                title = "65W GaN Fast Charger",
                price = 899.0,
                originalPrice = 1499.0,
                discountPercent = 40,
                category = "Electronics",
                description = "Dual Type-C + USB-A ports, PD 3.0 ultra-compact GaN fast power adapter for laptops and smartphones.",
                badge = "TRENDING"
            ),
            CatalogueProduct(
                id = "cat_3",
                title = "Pure Banarasi Silk Saree",
                price = 3499.0,
                originalPrice = 5999.0,
                discountPercent = 42,
                category = "Fashion",
                description = "Traditional Handwoven Zari border with unstitched matching blouse piece. Authentic Made in India.",
                badge = "PREMIUM"
            ),
            CatalogueProduct(
                id = "cat_4",
                title = "Khadi Cotton Casual Kurta",
                price = 849.0,
                originalPrice = 1299.0,
                discountPercent = 35,
                category = "Fashion",
                description = "100% breathable organic handspun khadi cotton with mandarin collar and coconut shell buttons.",
                badge = "ECO-FRIENDLY"
            ),
            CatalogueProduct(
                id = "cat_5",
                title = "Kashmiri Royal Saffron (5g)",
                price = 1250.0,
                originalPrice = 1600.0,
                discountPercent = 22,
                category = "Groceries",
                description = "Grade-A Mongra saffron strands from Pampore, Kashmir. 100% pure aroma and natural golden color.",
                badge = "GI TAGGED"
            ),
            CatalogueProduct(
                id = "cat_6",
                title = "Organic First Flush Darjeeling Tea (250g)",
                price = 499.0,
                originalPrice = 699.0,
                discountPercent = 28,
                category = "Groceries",
                description = "Single-estate whole leaf black tea with muscatel floral notes. Direct from Himalayan gardens.",
                badge = "ORGANIC"
            ),
            CatalogueProduct(
                id = "cat_7",
                title = "Handcrafted Brass Diya Puja Stand",
                price = 799.0,
                originalPrice = 1199.0,
                discountPercent = 33,
                category = "Handicrafts",
                description = "Solid brass heavy traditional five-step peacock engraved lamp. Auspicious home and festive decor.",
                badge = "ARTISAN"
            ),
            CatalogueProduct(
                id = "cat_8",
                title = "Business Website & App Design",
                price = 4999.0,
                originalPrice = 8999.0,
                discountPercent = 45,
                category = "Services",
                description = "Modern responsive mobile & web layout with UPI payment gateway integration and SEO setup.",
                badge = "SERVICE"
            )
        )
    }

    // Custom Item Form State
    var customTitle by remember { mutableStateOf("") }
    var customPrice by remember { mutableStateOf("") }
    var customOriginalPrice by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Electronics") }
    var customDescription by remember { mutableStateOf("") }

    val categories = listOf("All", "Electronics", "Fashion", "Groceries", "Handicrafts", "Services")

    val filteredProducts = remember(selectedCategory, searchQuery) {
        defaultProducts.filter { prod ->
            (selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || prod.title.contains(searchQuery, ignoreCase = true) || prod.description.contains(searchQuery, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BharatSaffron.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = BharatSaffron, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "Business Catalogue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = bColors.textPrimary
                        )
                        Text(
                            text = "Share products with instant UPI order links",
                            fontSize = 11.5.sp,
                            color = bColors.textSecondary
                        )
                    }
                }
                TricolorGlowPill(text = "Venzo Commerce")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = BharatSaffron,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BharatSaffron,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Browse Items (${defaultProducts.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) BharatSaffron else bColors.textSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "+ Custom Product",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) BharatSaffron else bColors.textSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Search & Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search products, spices, electronics...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = bColors.textMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = bColors.textMuted)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("catalogue_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BharatSaffron,
                        unfocusedBorderColor = bColors.glassBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BharatSaffron else (if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else bColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredProducts) { prod ->
                        CatalogueItemCard(
                            product = prod,
                            onSend = {
                                onProductSelected(prod)
                                onDismiss()
                            }
                        )
                    }
                }
            } else {
                // Custom Product Creation Form
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "Product Details",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = bColors.textPrimary
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Product / Service Name *") },
                            placeholder = { Text("e.g. Wireless Bluetooth Speaker") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_catalogue_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = customPrice,
                                onValueChange = { customPrice = it },
                                label = { Text("Selling Price (₹) *") },
                                placeholder = { Text("999") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_catalogue_price_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = customOriginalPrice,
                                onValueChange = { customOriginalPrice = it },
                                label = { Text("MRP / Strike Price (₹)") },
                                placeholder = { Text("1499") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_catalogue_mrp_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = bColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories.filter { it != "All" }) { cat ->
                                val isSelected = customCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) BharatGreenLight else (if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { customCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else bColors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = customDescription,
                            onValueChange = { customDescription = it },
                            label = { Text("Description & Features") },
                            placeholder = { Text("Highlights, warranty, size, or material details...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("custom_catalogue_desc_input"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val priceVal = customPrice.toDoubleOrNull() ?: 0.0
                                if (customTitle.isNotBlank() && priceVal > 0) {
                                    val origPrice = customOriginalPrice.toDoubleOrNull()
                                    val disc = if (origPrice != null && origPrice > priceVal) {
                                        (((origPrice - priceVal) / origPrice) * 100).toInt()
                                    } else 0

                                    val customProd = CatalogueProduct(
                                        id = "custom_${UUID.randomUUID()}",
                                        title = customTitle.trim(),
                                        price = priceVal,
                                        originalPrice = origPrice,
                                        discountPercent = disc,
                                        category = customCategory,
                                        description = customDescription.ifBlank { "High quality $customTitle. Authentic & instant dispatch." },
                                        badge = "CUSTOM"
                                    )
                                    onProductSelected(customProd)
                                    onDismiss()
                                }
                            },
                            enabled = customTitle.isNotBlank() && (customPrice.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_custom_catalogue_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Catalogue to Chat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogueItemCard(
    product: CatalogueProduct,
    onSend: () -> Unit
) {
    val bColors = LocalBharatColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (bColors.isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (product.category) {
                            "Electronics" -> BharatElectricCyan.copy(alpha = 0.15f)
                            "Fashion" -> Color(0xFFEC4899).copy(alpha = 0.15f)
                            "Groceries" -> BharatGreenLight.copy(alpha = 0.15f)
                            "Handicrafts" -> BharatSaffron.copy(alpha = 0.15f)
                            else -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        when (product.category) {
                            "Electronics" -> BharatElectricCyan.copy(alpha = 0.4f)
                            "Fashion" -> Color(0xFFEC4899).copy(alpha = 0.4f)
                            "Groceries" -> BharatGreenLight.copy(alpha = 0.4f)
                            "Handicrafts" -> BharatSaffron.copy(alpha = 0.4f)
                            else -> Color(0xFF8B5CF6).copy(alpha = 0.4f)
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (product.category) {
                        "Electronics" -> Icons.Default.Devices
                        "Fashion" -> Icons.Default.Checkroom
                        "Groceries" -> Icons.Default.LocalGroceryStore
                        "Handicrafts" -> Icons.Default.Palette
                        "Services" -> Icons.Default.DesignServices
                        else -> Icons.Default.ShoppingBag
                    },
                    contentDescription = null,
                    tint = when (product.category) {
                        "Electronics" -> BharatElectricCyan
                        "Fashion" -> Color(0xFFEC4899)
                        "Groceries" -> BharatGreenLight
                        "Handicrafts" -> BharatSaffron
                        else -> Color(0xFF8B5CF6)
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = bColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    product.badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BharatSaffron.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = it,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatSaffron,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = product.description,
                    fontSize = 11.sp,
                    color = bColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = BharatGreenLight
                    )
                    if (product.originalPrice != null && product.originalPrice > product.price) {
                        Text(
                            text = "₹${product.originalPrice.toInt()}",
                            fontSize = 11.sp,
                            color = bColors.textMuted,
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                        )
                    }
                    if (product.discountPercent > 0) {
                        Text(
                            text = "${product.discountPercent}% OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            FilledTonalButton(
                onClick = onSend,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = BharatGreenLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
