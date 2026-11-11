package com.auraface.auraface_app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auraface.auraface_app.data.network.model.TimetableItem

@Composable
fun TimetableList(timetable: List<TimetableItem>) {
    // Sort logic? Monday=1,..
    // Simple approach: Map days to order
    val daysOrder = mapOf("monday" to 1, "tuesday" to 2, "wednesday" to 3, "thursday" to 4, "friday" to 5, "saturday" to 6, "sunday" to 7)
    
    val sortedList = timetable.sortedWith(compareBy({ daysOrder[it.day?.lowercase()] ?: 8 }, { it.time }))

    if (sortedList.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
             Text("No classes scheduled yet.", color = MaterialTheme.colorScheme.secondary)
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            sortedList.forEach { item ->
                TimetableCard(item)
            }
        }
    }
}

@Composable
fun TimetableCard(item: TimetableItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (item.teacher != null) {
                    Text("Teacher: ${item.teacher}", style = MaterialTheme.typography.bodyMedium)
                }
                if (item.department != null) {
                    Text("Class: ${item.year}-${item.section} (${item.department})", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = item.day ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.time ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
