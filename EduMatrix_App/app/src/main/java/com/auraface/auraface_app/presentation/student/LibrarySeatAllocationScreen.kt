@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.api.SpacesApi
import com.auraface.auraface_app.data.remote.dto.LibrarySeatDto
import com.auraface.auraface_app.data.remote.dto.LibraryBookingResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibrarySeatAllocationViewModel @Inject constructor(
    private val spacesApi: SpacesApi
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _seats = MutableStateFlow<List<LibrarySeatDto>>(emptyList())
    val seats: StateFlow<List<LibrarySeatDto>> = _seats

    private val _bookingResult = MutableStateFlow<LibraryBookingResponseDto?>(null)
    val bookingResult: StateFlow<LibraryBookingResponseDto?> = _bookingResult

    fun loadSeats(onError: (String) -> Unit) {
        _loading.value = true
        _bookingResult.value = null
        viewModelScope.launch {
            try {
                val res = spacesApi.getLibrarySeats()
                if (res.isSuccessful && res.body() != null) {
                    _seats.value = res.body()!!
                } else {
                    onError("Failed to fetch library seats")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to connect to server")
            } finally {
                _loading.value = false
            }
        }
    }

    fun bookSeat(seatId: Int, timeSlot: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val res = spacesApi.bookLibrarySeat(
                    mapOf("seat_id" to seatId, "time_slot" to timeSlot)
                )
                if (res.isSuccessful && res.body() != null) {
                    _bookingResult.value = res.body()!!
                    // Reload seats
                    val reload = spacesApi.getLibrarySeats()
                    if (reload.isSuccessful && reload.body() != null) {
                        _seats.value = reload.body()!!
                    }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Booking request rejected")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Booking request failed")
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearBooking() {
        _bookingResult.value = null
    }
}

@Composable
fun LibrarySeatAllocationScreen(
    navController: NavController,
    viewModel: LibrarySeatAllocationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val seats by viewModel.seats.collectAsState()
    val bookingResult by viewModel.bookingResult.collectAsState()

    var selectedFloor by remember { mutableIntStateOf(1) } // 1 or 2
    var selectedTimeSlot by remember { mutableStateOf("10:00 AM - 12:00 PM") }
    var selectedSeat by remember { mutableStateOf<LibrarySeatDto?>(null) }
    var showTimeDropdown by remember { mutableStateOf(false) }

    val timeSlots = listOf(
        "10:00 AM - 12:00 PM",
        "12:00 PM - 02:00 PM",
        "02:00 PM - 04:00 PM",
        "04:00 PM - 06:00 PM"
    )

    LaunchedEffect(Unit) {
        viewModel.loadSeats { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        }
    }

    // Reset selected seat if floor changes
    LaunchedEffect(selectedFloor) {
        selectedSeat = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library Seat Allocation", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4F46E5))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            if (loading && seats.isEmpty()) {
                CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header selector cards (Floor choice)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFloor == 1) Color(0xFFEEF2FF) else Color.White
                            ),
                            border = if (selectedFloor == 1) BorderStroke(1.5.dp, Color(0xFF4F46E5)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFloor = 1 },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Floor 1", fontWeight = FontWeight.Bold, color = if (selectedFloor == 1) Color(0xFF4F46E5) else Color(0xFF475569))
                                Text("Quiet Study Zone", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFloor == 2) Color(0xFFEEF2FF) else Color.White
                            ),
                            border = if (selectedFloor == 2) BorderStroke(1.5.dp, Color(0xFF4F46E5)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFloor = 2 },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Floor 2", fontWeight = FontWeight.Bold, color = if (selectedFloor == 2) Color(0xFF4F46E5) else Color(0xFF475569))
                                Text("Computer Lab", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time Slot Picker Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeDropdown = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF4F46E5))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Selected Booking Window", fontSize = 11.sp, color = Color.Gray)
                                Text(selectedTimeSlot, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 15.sp)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            
                            DropdownMenu(
                                expanded = showTimeDropdown,
                                onDismissRequest = { showTimeDropdown = false }
                            ) {
                                timeSlots.forEach { slot ->
                                    DropdownMenuItem(
                                        text = { Text(slot) },
                                        onClick = {
                                            selectedTimeSlot = slot
                                            showTimeDropdown = false
                                            selectedSeat = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Legend indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LegendItem("Available", Color(0xFF10B981))
                        LegendItem("Selected", Color(0xFF3B82F6))
                        LegendItem("Occupied", Color(0xFFEF4444))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Classroom Desk Layout Grid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Seat Grid Layout
                    val filteredSeats = seats.filter { it.floor == selectedFloor }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredSeats) { seat ->
                            val isSelected = selectedSeat?.id == seat.id
                            val containerColor = if (seat.is_booked) Color(0xFFFEE2E2)
                                                else if (isSelected) Color(0xFFDBEAFE)
                                                else Color(0xFFD1FAE5)
                            val borderColor = if (seat.is_booked) Color(0xFFEF4444)
                                            else if (isSelected) Color(0xFF3B82F6)
                                            else Color(0xFF10B981)
                            val textColor = if (seat.is_booked) Color(0xFF991B1B)
                                            else if (isSelected) Color(0xFF1E40AF)
                                            else Color(0xFF065F46)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = containerColor,
                                border = BorderStroke(1.5.dp, borderColor),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable(enabled = !seat.is_booked) {
                                        selectedSeat = if (isSelected) null else seat
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (seat.floor == 2) Icons.Default.Computer else Icons.Default.Chair,
                                            contentDescription = null,
                                            tint = borderColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = seat.seat_no,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Booking Action Button
                    Button(
                        onClick = {
                            selectedSeat?.let { seat ->
                                viewModel.bookSeat(seat.id, selectedTimeSlot, {
                                    Toast.makeText(context, "Library seat successfully locked!", Toast.LENGTH_SHORT).show()
                                }, { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedSeat != null
                    ) {
                        Text(
                            text = if (selectedSeat != null) "Lock Seat ${selectedSeat!!.seat_no}" else "Select an Available Seat",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Booking confirmation ticket modal
        if (bookingResult != null) {
            TicketDialog(
                booking = bookingResult!!,
                onDismiss = {
                    viewModel.clearBooking()
                    selectedSeat = null
                }
            )
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TicketDialog(
    booking: LibraryBookingResponseDto,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Seat Allocation Active", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(16.dp))

                // Ticket Body Card (looks like a cinema/travel ticket)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(booking.ticket_no, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5), fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Seat Number", fontSize = 10.sp, color = Color.Gray)
                                Text(booking.seat_no, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Floor / Zone", fontSize = 10.sp, color = Color.Gray)
                                Text("Floor ${booking.floor} - ${booking.zone}", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Reserved Booking Time", fontSize = 10.sp, color = Color.Gray)
                            Text(booking.time_slot, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Simulated entry QR barcode scanner
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(64.dp)
                                    .fillMaxWidth(0.8f),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val barcodeWidths = listOf(2, 4, 1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 4)
                                barcodeWidths.forEach { width ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(width.dp)
                                            .background(Color.Black)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("DIGITAL GATE ENTRY PASS", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Pass", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
