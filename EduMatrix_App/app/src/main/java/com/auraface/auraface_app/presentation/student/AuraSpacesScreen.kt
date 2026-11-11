package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.auraface.auraface_app.data.network.model.BookingRequest
import com.auraface.auraface_app.data.network.model.MyBookingOut
import com.auraface.auraface_app.data.network.model.SpaceItem
import com.auraface.auraface_app.data.repository.SpacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AuraSpacesViewModel @Inject constructor(
    private val repository: SpacesRepository
) : ViewModel() {
    
    var spaces by mutableStateOf<List<SpaceItem>>(emptyList())
    var myBookings by mutableStateOf<List<MyBookingOut>>(emptyList())
    var isLoading by mutableStateOf(false)
    
    var bookingMessage by mutableStateOf("")
    var isBooking by mutableStateOf(false)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val spacesRes = repository.getSpaces()
                if (spacesRes.isSuccessful) spaces = spacesRes.body() ?: emptyList()
                
                val bookingsRes = repository.getMyBookings()
                if (bookingsRes.isSuccessful) myBookings = bookingsRes.body() ?: emptyList()
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    fun bookSpace(spaceId: String, date: String, timeSlot: String, purpose: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isBooking = true
            try {
                val res = repository.bookSpace(BookingRequest(spaceId, date, timeSlot, purpose))
                if (res.isSuccessful) {
                    val data = res.body()
                    if (data != null) {
                        bookingMessage = data.message
                        onSuccess()
                        loadData() // Refresh bindings
                    }
                } else {
                    bookingMessage = "Failed to book space. It might be taken."
                }
            } catch (e: Exception) {
                bookingMessage = "Network error."
            } finally {
                isBooking = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraSpacesScreen(
    navController: NavController,
    viewModel: AuraSpacesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    
    var showBookingDialog by remember { mutableStateOf<SpaceItem?>(null) }
    var selectedTimeSlot by remember { mutableStateOf("10:00 AM - 11:00 AM") }
    var purposeText by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.bookingMessage) {
        if (viewModel.bookingMessage.isNotEmpty()) {
            Toast.makeText(context, viewModel.bookingMessage, Toast.LENGTH_LONG).show()
            viewModel.bookingMessage = ""
        }
    }

    val tabs = listOf("Available Spaces", "My Bookings")
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Spaces \uD83C\uDFE2", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6366F1)) // Indigo
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF6366F1)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                }
            } else if (selectedTab == 0) {
                // Browse Spaces
                val categories = listOf("All") + viewModel.spaces.map { it.type }.distinct()
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF6366F1) else Color.White,
                            border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                val filteredSpaces = if (selectedCategory == "All") viewModel.spaces else viewModel.spaces.filter { it.type == selectedCategory }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSpaces) { space ->
                        SpaceCard(space = space) {
                            if (space.is_available) {
                                showBookingDialog = space
                            } else {
                                Toast.makeText(context, "Space currently unavailable. Next slot at ${space.next_available_time}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

            } else {
                // My Bookings
                if (viewModel.myBookings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("You have no upcoming bookings.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                         items(viewModel.myBookings) { booking ->
                             MyBookingCard(booking)
                         }
                    }
                }
            }
        }

        // Booking Dialog
        if (showBookingDialog != null) {
            val space = showBookingDialog!!
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            AlertDialog(
                onDismissRequest = { showBookingDialog = null },
                title = { Text("Book ${space.name}") },
                text = {
                    Column {
                        Text("Location: ${space.location}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Select Time Slot", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val slots = listOf("10:00 AM - 11:00 AM", "11:00 AM - 12:00 PM", "02:00 PM - 03:00 PM", "04:00 PM - 05:00 PM")
                        slots.forEach { slot ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedTimeSlot == slot, onClick = { selectedTimeSlot = slot })
                                Text(slot, fontSize = 14.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = purposeText,
                            onValueChange = { purposeText = it },
                            label = { Text("Purpose (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            viewModel.bookSpace(space.id, todayDateStr, selectedTimeSlot, purposeText) {
                                showBookingDialog = null
                                purposeText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        enabled = !viewModel.isBooking
                    ) {
                        if (viewModel.isBooking) {
                             CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Confirm Booking")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBookingDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun SpaceCard(space: SpaceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Text(space.icon_emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(space.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                Text("${space.location} • Capacity: ${space.capacity}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                
                if (space.is_available) {
                    Text("Available Now", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Occupied • Next slot: ${space.next_available_time}", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun MyBookingCard(booking: MyBookingOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(booking.icon_emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(booking.space_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(booking.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(booking.date, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(booking.time_slot, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
            }
        }
    }
}
