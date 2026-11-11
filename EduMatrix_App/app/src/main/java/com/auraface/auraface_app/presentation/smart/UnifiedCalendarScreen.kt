package com.auraface.auraface_app.presentation.smart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.auraface.auraface_app.R

data class CalendarEventItem(
    val eventName: String,
    val dates: String,
    val isHighlighted: Boolean = false
)

val calendarData = listOf(
    CalendarEventItem("Starting of Instructions/Classes", "27 November 2025"),
    CalendarEventItem("Registration (₹ 1500 registration fee)", "3-10 January 2026"),
    CalendarEventItem("Project Allocation", "13 December 2025"),
    CalendarEventItem("Project Review I", "30-31 January 2026"),
    CalendarEventItem("Cycle Test - I (Two subjects per day)", "12-14 February 2026", true),
    CalendarEventItem("Display of Cycle Test - I Marks", "19 February 2026"),
    CalendarEventItem("Project Review II", "23-24 March 2026"),
    CalendarEventItem("Form Fill-up (₹ 1000 form fill-up fee)", "30-31 March 2026"),
    CalendarEventItem("Practical / Sessional Examinations and Project Viva-Voce", "2-8 April 2026", true),
    CalendarEventItem("Cycle Test - II (Two subjects per day)", "9-11 April 2026", true),
    CalendarEventItem("Display of Cycle Test - II Marks", "16 April 2026"),
    CalendarEventItem("Closing of Instructions/Classes", "11 April 2026"),
    CalendarEventItem("Sending of Internal and Laboratory Marks to CoE", "25 April 2026"),
    CalendarEventItem("Semester End Examinations", "16 April - 9 May 2026", true),
    CalendarEventItem("Publication of Semester End Examinations Results", "23 May 2026"),
    CalendarEventItem("Commencement of Odd Semester 2026-27", "1 July 2026")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCalendarScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academic Calendar", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D47A1)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F5F9))
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_playstore),
                            contentDescription = "GIET Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentScale = ContentScale.Inside
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GANDHI INSTITUTE OF ENGINEERING AND TECHNOLOGY UNIVERSITY, ODISHA, GUNUPUR",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "(Established Vide Odisha Act 23 of 2018, Included by UGC, New Delhi, and Approved by AICTE, INC, PCI, New Delhi)",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ACADEMIC CALENDAR FOR EVEN SEMESTER 2025-26",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0D47A1),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "B. Tech - VI Semester [Third Year]",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D47A1))
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("S. No.", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center)
                            Text("Event", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(0.5f).padding(start = 8.dp))
                            Text("Date(s)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(0.35f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            
            itemsIndexed(calendarData) { index, event ->
                val bg = if (index % 2 == 0) Color.White else Color(0xFFF8FAFC)
                val highlightBg = if (event.isHighlighted) Color(0xFFFEF3C7) else bg
                val textColor = if (event.isHighlighted) Color(0xFFB45309) else Color(0xFF1E293B)
                val fontWeight = if (event.isHighlighted) FontWeight.Bold else FontWeight.Medium
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .shadow(if (event.isHighlighted) 4.dp else 1.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = highlightBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = if (event.isHighlighted) Color(0xFFB45309) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(0.15f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = event.eventName,
                            fontWeight = fontWeight,
                            color = textColor,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(0.5f).padding(start = 8.dp),
                            lineHeight = 16.sp
                        )
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(0.35f).padding(horizontal = 4.dp)) {
                            Text(
                                text = event.dates,
                                fontWeight = FontWeight.SemiBold,
                                color = if (event.isHighlighted) Color(0xFF92400E) else Color(0xFF0D47A1),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("NOTE:", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        NoteBullet("At least one class test is to be conducted by the departments before each cycle test.")
                        NoteBullet("Students are required to submit a minimum of two assignments.")
                        NoteBullet("Non-registered students will not be allowed to attend classes or reside in hostels.")
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Text("N.B.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E293B), modifier = Modifier.padding(end = 6.dp))
                            Text(
                                "If the total number of instructional days is less than 90, subject teachers are required to arrange extra classes to meet the requirement.",
                                fontSize = 11.sp,
                                color = Color(0xFF334155),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteBullet(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 6.dp)) {
        Text("❖", color = Color(0xFF0D47A1), fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
        Text(text, fontSize = 12.sp, color = Color(0xFF334155), lineHeight = 16.sp)
    }
}
