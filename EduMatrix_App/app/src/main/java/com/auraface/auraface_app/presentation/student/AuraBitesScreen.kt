package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.*
import com.auraface.auraface_app.data.repository.CanteenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuraBitesViewModel @Inject constructor(
    private val repository: CanteenRepository
) : ViewModel() {
    
    var status by mutableStateOf<CanteenStatus?>(null)
    var menu by mutableStateOf<List<MenuItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    
    // Cart management mapping Menu ID to Quantity
    var cart = mutableStateMapOf<String, Int>()
    
    var orderMessage by mutableStateOf("")
    var isPlacingOrder by mutableStateOf(false)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val statusRes = repository.getStatus()
                if (statusRes.isSuccessful) status = statusRes.body()
                
                val menuRes = repository.getMenu()
                if (menuRes.isSuccessful) menu = menuRes.body() ?: emptyList()
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                isLoading = false
            }
        }
    }

    fun addToCart(item: MenuItem) {
        val count = cart[item.id] ?: 0
        cart[item.id] = count + 1
    }
    
    fun removeFromCart(item: MenuItem) {
        val count = cart[item.id] ?: 0
        if (count > 1) {
            cart[item.id] = count - 1
        } else {
            cart.remove(item.id)
        }
    }

    fun placeOrder() {
        if (cart.isEmpty()) return
        viewModelScope.launch {
            isPlacingOrder = true
            try {
                val items = cart.map { OrderItem(it.key, it.value) }
                val res = repository.placeOrder(OrderRequest(items, "ASAP", ""))
                if (res.isSuccessful) {
                    val data = res.body()
                    if (data != null) {
                        orderMessage = "${data.message}\nTotal: ₹${data.total_amount}"
                        cart.clear()
                    }
                }
            } catch (e: Exception) {
                orderMessage = "Failed to place order."
            } finally {
                isPlacingOrder = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraBitesScreen(
    navController: NavController,
    viewModel: AuraBitesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var showCartDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.orderMessage) {
        if (viewModel.orderMessage.isNotEmpty()) {
            Toast.makeText(context, viewModel.orderMessage, Toast.LENGTH_LONG).show()
            viewModel.orderMessage = ""
            showCartDialog = false
        }
    }

    val cartTotalItems = viewModel.cart.values.sum()
    val cartTotalPrice = viewModel.cart.map { entry -> 
        val item = viewModel.menu.find { it.id == entry.key }
        (item?.price ?: 0.0) * entry.value 
    }.sum()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Bites \uD83C\uDF54", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE11D48)) // Rose Red
            )
        },
        floatingActionButton = {
            if (cartTotalItems > 0) {
                ExtendedFloatingActionButton(
                    onClick = { showCartDialog = true },
                    containerColor = Color(0xFFE11D48),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Cart") },
                    text = { Text("View Cart ($cartTotalItems) • ₹$cartTotalPrice") }
                )
            }
        },
        containerColor = Color(0xFFFFF1F2)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // Status Header
            viewModel.status?.let { status ->
                val crowdColor = when {
                    status.crowd_percentage > 80 -> Color(0xFFEF4444)
                    status.crowd_percentage > 50 -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = crowdColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Live Canteen Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Crowd: ${status.crowd_percentage}%", fontWeight = FontWeight.SemiBold, color = crowdColor)
                            Text("Wait: ~${status.wait_time_mins} mins", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { status.crowd_percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = crowdColor,
                            trackColor = Color(0xFFF3F4F6)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(8.dp)) {
                            Text("🔥 Popular Now: ${status.popular_now}", fontSize = 12.sp, modifier = Modifier.padding(8.dp), color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Categories
            val categories = listOf("All") + viewModel.menu.map { it.category }.distinct()
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFFE11D48) else Color.White,
                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Grid
            val filteredMenu = if (selectedCategory == "All") viewModel.menu else viewModel.menu.filter { it.category == selectedCategory }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // space for FAB
            ) {
                items(filteredMenu) { item ->
                    MenuCard(
                        item = item,
                        cartCount = viewModel.cart[item.id] ?: 0,
                        onAdd = { viewModel.addToCart(item) },
                        onRemove = { viewModel.removeFromCart(item) }
                    )
                }
            }
        }

        // Cart Checkout Dialog
        if (showCartDialog) {
            AlertDialog(
                onDismissRequest = { showCartDialog = false },
                title = { Text("Your Order \uD83D\uDED2") },
                text = {
                    Column {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(viewModel.cart.toList()) { (itemId, qty) ->
                                val mItem = viewModel.menu.find { it.id == itemId }
                                if (mItem != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${mItem.name} x$qty", modifier = Modifier.weight(1f), maxLines = 1, fontSize = 14.sp)
                                        Text("₹${mItem.price * qty}", fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("₹$cartTotalPrice", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFE11D48))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.placeOrder() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        enabled = !viewModel.isPlacingOrder
                    ) {
                        if (viewModel.isPlacingOrder) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Confirm & Pay")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCartDialog = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun MenuCard(
    item: MenuItem,
    cartCount: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.padding(4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF9FAFB)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.image_emoji, fontSize = 64.sp)
                // Veg / Non-Veg Indicator
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = if(item.is_veg) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = Color(0xFF1F2937))
            Text("₹${item.price} • ${item.prep_time_mins} mins", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (cartCount == 0) {
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE11D48)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("ADD", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).background(Color(0xFFE11D48), RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.White)
                    }
                    Text(cartCount.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
            }
        }
    }
}
