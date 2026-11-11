@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LibraryItem(
    val title: String,
    val author: String,
    val type: String, // "PDF", "EPUB", "DOCX"
    val size: String,
    val category: String,
    val views: Int
)

@Composable
fun SmartLibraryScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("All", "E-Books", "Lecture Notes", "Previous Papers", "Projects")
    var selectedCategory by remember { mutableStateOf("All") }

    val allItems = listOf(
        LibraryItem("Advanced Data Structures", "Prof. R. Sharma", "PDF", "2.4 MB", "E-Books", 1240),
        LibraryItem("Operating Systems Mid-Term 2024", "Exams Dept", "PDF", "1.1 MB", "Previous Papers", 850),
        LibraryItem("Machine Learning Notes (Unit 1-3)", "Dr. S. Gupta", "PDF", "4.5 MB", "Lecture Notes", 3200),
        LibraryItem("Design Patterns in Java", "Aura Press", "EPUB", "8.2 MB", "E-Books", 410),
        LibraryItem("DBMS Final Project SRS", "Batch 2025", "DOCX", "500 KB", "Projects", 150),
        LibraryItem("Software Engineering Q-Bank", "Exams Dept", "PDF", "3.0 MB", "Previous Papers", 950),
        LibraryItem("Cloud Computing (AWS/Azure)", "Tech Series", "PDF", "12 MB", "E-Books", 500)
    )

    val filteredItems = allItems.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart E-Library", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4F46E5))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            // Header Search Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search books, notes, papers...", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.Transparent else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Items
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No resources found", color = Color(0xFF94A3B8), fontSize = 16.sp)
                        }
                    }
                } else {
                    items(filteredItems) { item ->
                        LibraryItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItemCard(item: LibraryItem) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (item.category) {
                            "E-Books" -> Color(0xFFE0E7FF)
                            "Lecture Notes" -> Color(0xFFDCFCE7)
                            "Previous Papers" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFF1F5F9)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.category) {
                        "E-Books" -> Icons.Default.MenuBook
                        "Lecture Notes" -> Icons.Default.Description
                        "Previous Papers" -> Icons.Default.HistoryEdu
                        else -> Icons.Default.FolderZip
                    },
                    contentDescription = null,
                    tint = when (item.category) {
                        "E-Books" -> Color(0xFF4F46E5)
                        "Lecture Notes" -> Color(0xFF10B981)
                        "Previous Papers" -> Color(0xFFF59E0B)
                        else -> Color(0xFF64748B)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By ${item.author} • ${item.size}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${item.views} Views", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(item.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Download Button
            IconButton(
                onClick = { 
                    if (!isDownloading) {
                        isDownloading = true
                        scope.launch {
                            delay(1500)
                            isDownloading = false
                            Toast.makeText(context, "${item.title} downloaded successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .size(40.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF4F46E5))
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF4F46E5))
                }
            }
        }
    }
}
