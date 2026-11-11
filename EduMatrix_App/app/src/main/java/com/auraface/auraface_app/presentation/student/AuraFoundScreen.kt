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
import com.auraface.auraface_app.data.network.model.LostItemCreate
import com.auraface.auraface_app.data.network.model.LostItemOut
import com.auraface.auraface_app.data.repository.LostFoundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuraFoundViewModel @Inject constructor(
    private val repository: LostFoundRepository
) : ViewModel() {
    
    var items by mutableStateOf<List<LostItemOut>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isReporting by mutableStateOf(false)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val res = repository.getAllItems()
                if (res.isSuccessful) items = res.body() ?: emptyList()
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun reportItem(title: String, desc: String, loc: String, type: String, cat: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isReporting = true
            try {
                val res = repository.reportItem(LostItemCreate(title, desc, loc, type, cat))
                if (res.isSuccessful) {
                    onSuccess()
                    loadData()
                }
            } catch (e: Exception) {
            } finally {
                isReporting = false
            }
        }
    }

    fun resolveItem(id: String) {
        viewModelScope.launch {
            try {
                repository.resolveItem(id)
                loadData()
            } catch (e: Exception) { }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraFoundScreen(
    navController: NavController,
    viewModel: AuraFoundViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Found \uD83D\uDD0D", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F766E)) // Teal
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showReportDialog = true },
                containerColor = Color(0xFF0F766E),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report Item")
            }
        },
        containerColor = Color(0xFFF0FDFA) // Light Teal Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // Filter Pills
            val filters = listOf("All", "LOST", "FOUND")
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF0F766E) else Color.White,
                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFCCFBF1)) else null,
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = if (filter == "All") "All Items" else filter,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (viewModel.isLoading && viewModel.items.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0F766E))
                 }
            } else {
                val filteredItems = if (selectedFilter == "All") viewModel.items else viewModel.items.filter { it.type == selectedFilter }
                
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items match your filter.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems) { item ->
                            LostItemCard(item = item, onResolve = { viewModel.resolveItem(item.id) })
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
                    }
                }
            }
        }

        // Report Dialog
        if (showReportDialog) {
            ReportItemDialog(
                isReporting = viewModel.isReporting,
                onDismiss = { showReportDialog = false },
                onSubmit = { title, desc, loc, type, cat ->
                    viewModel.reportItem(title, desc, loc, type, cat) {
                        showReportDialog = false
                        Toast.makeText(context, "Item reported successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun LostItemCard(item: LostItemOut, onResolve: () -> Unit) {
    val isLost = item.type == "LOST"
    val colorTag = if (isLost) Color(0xFFEF4444) else Color(0xFF10B981)
    val alphaColor = if (item.status == "RESOLVED") 0.5f else 1f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if(item.status == "RESOLVED") 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorTag.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if(isLost) "🕵️ LOOKING FOR" else "🎯 FOUND ITEM",
                        color = colorTag,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                if (item.status == "RESOLVED") {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                        Text("✅ RESOLVED", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(item.date_reported, color = Color.Gray, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B).copy(alpha = alphaColor))
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, fontSize = 14.sp, color = Color.DarkGray.copy(alpha = alphaColor))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray.copy(alpha=alphaColor), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(item.location_found_or_lost, fontSize = 13.sp, color = Color.Gray.copy(alpha=alphaColor), fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Reported by: ${item.reporter_name}", fontSize = 11.sp, color = Color.Gray.copy(alpha=alphaColor))
                    Text("Contact: ${item.contact_info}", fontSize = 11.sp, color = Color(0xFF6366F1).copy(alpha=alphaColor), fontWeight = FontWeight.SemiBold)
                }
                if (item.status == "ACTIVE") {
                     OutlinedButton(
                         onClick = onResolve,
                         contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                         modifier = Modifier.height(32.dp)
                     ) {
                         Text("Mark Resolved", fontSize = 12.sp)
                     }
                }
            }
        }
    }
}

@Composable
fun ReportItemDialog(
    isReporting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf("") }
    var isLost by remember { mutableStateOf(true) } // True = Lost, False = Found
    
    val categories = listOf("Electronics", "ID/Wallet", "Keys", "Other")
    var selectedCat by remember { mutableStateOf(categories[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Item") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = isLost, onClick = { isLost = true }, label = { Text("I Lost Something") })
                    FilterChip(selected = !isLost, onClick = { isLost = false }, label = { Text("I Found Something") })
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description/Details") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("Category", fontSize = 12.sp, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if(title.isNotBlank() && loc.isNotBlank()) {
                         onSubmit(title, desc, loc, if(isLost) "LOST" else "FOUND", selectedCat)
                    }
                },
                enabled = !isReporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                 if (isReporting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                 else Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
