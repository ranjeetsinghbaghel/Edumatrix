package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var eventsList by remember { mutableStateOf<List<com.auraface.auraface_app.data.network.api.CalendarEventDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var title by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Holiday") }
    var expandedType by remember { mutableStateOf(false) }
    val eventTypes = listOf("Holiday", "Exam", "Deadline")

    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshEvents() {
        isLoading = true
        coroutineScope.launch {
            try {
                eventsList = viewModel.getAcademicCalendar()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshEvents()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Add Event Form
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add New Event", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date Range (e.g. 15 Oct - 20 Oct)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = !expandedType }
                    ) {
                        OutlinedTextField(
                            value = eventType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Event Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().padding(bottom = 16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            eventTypes.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        eventType = selection
                                        expandedType = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && dateStr.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        viewModel.addCalendarEvent(title, dateStr, eventType)
                                        title = ""
                                        dateStr = ""
                                        refreshEvents()
                                        snackbarHostState.showSnackbar("Event added")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(e.message ?: "Failed to add event")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Event")
                    }
                }
            }

            // Events List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (eventsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No events found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(eventsList) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("${event.date} • ${event.type}", color = Color.Gray, fontSize = 14.sp)
                                }
                                IconButton(onClick = {
                                    event.id?.let {
                                        coroutineScope.launch {
                                            try {
                                                viewModel.deleteCalendarEvent(it)
                                                refreshEvents()
                                                snackbarHostState.showSnackbar("Deleted")
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(e.message ?: "Failed")
                                            }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
