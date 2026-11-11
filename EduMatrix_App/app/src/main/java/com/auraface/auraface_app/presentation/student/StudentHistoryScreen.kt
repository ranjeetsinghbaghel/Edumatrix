@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.domain.model.Attendance

@Composable
fun StudentHistoryScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val records = viewModel.attendanceRecords

    LaunchedEffect(Unit) {
        viewModel.loadAttendanceHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Subject Wise Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = "No attendance records found.",
                    color = Color.Gray
                )
            }
        } else {
            val subjects = remember(records) {
                records.filter { !it.subject.isNullOrBlank() }
                    .map { it.subject!! }
                    .distinct()
                    .sorted()
            }

            val dateGroups = remember(records) {
                val map = LinkedHashMap<String, List<Attendance>>()
                records.forEach { record ->
                    val d = record.date ?: "Unknown Date"
                    val list = map.getOrPut(d) { mutableListOf() }
                    (list as MutableList).add(record)
                }
                map.toList()
            }

            val tableScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FA))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Day Wise Attendance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                        
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(tableScrollState)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.background(Color(0xFFF1F3F4))
                            ) {
                                TableCell(text = "Date", isHeader = true, width = 120.dp)
                                subjects.forEach { subject ->
                                    TableCell(text = subject.uppercase(), isHeader = true, width = 100.dp)
                                }
                                TableCell(text = "Total", isHeader = true, width = 80.dp)
                            }

                            // Data Rows
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(dateGroups) { (date, dailyRecords) ->
                                    Row {
                                        TableCell(text = date, isHeader = false, width = 120.dp)
                                        
                                        var dailyPresent = 0
                                        var dailyTotal = 0
                                        
                                        subjects.forEach { subject ->
                                            val subjRecords = dailyRecords.filter { it.subject == subject }
                                            val presentCount = subjRecords.count { 
                                                it.status.equals("Present", ignoreCase = true) || 
                                                it.status.equals("Late", ignoreCase = true) 
                                            }
                                            val totalCount = subjRecords.count { 
                                                it.status != "NC" 
                                            }
                                            
                                            val cellText = if (totalCount == 0) "NC" else "$presentCount/$totalCount"
                                            
                                            dailyPresent += presentCount
                                            dailyTotal += totalCount

                                            val bgColor = when {
                                                cellText == "NC" -> Color.Transparent
                                                presentCount == totalCount -> Color(0xFFD0DCFF)
                                                else -> Color(0xFFFFCDD2)
                                            }
                                            
                                            TableCell(
                                                text = cellText, 
                                                isHeader = false, 
                                                width = 100.dp,
                                                backgroundColor = bgColor
                                            )
                                        }
                                        
                                        val dailyOverall = if (dailyTotal == 0) "NC" else "$dailyPresent/$dailyTotal"
                                        val totalBgColor = when {
                                            dailyOverall == "NC" -> Color.Transparent
                                            dailyPresent == dailyTotal -> Color(0xFFD0DCFF)
                                            else -> Color(0xFFFFCDD2)
                                        }
                                        
                                        TableCell(
                                            text = dailyOverall, 
                                            isHeader = false, 
                                            width = 80.dp,
                                            backgroundColor = totalBgColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean,
    width: androidx.compose.ui.unit.Dp,
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .width(width)
            .background(backgroundColor)
            .border(0.5.dp, Color(0xFFE0E0E0))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isHeader) 13.sp else 14.sp,
            color = if (isHeader) Color(0xFF333333) else Color.DarkGray,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
