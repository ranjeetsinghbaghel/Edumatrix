@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.TimetableItem
import com.auraface.auraface_app.domain.model.Teacher

@Composable
fun ManageTimetableScreen(
    navController: NavController,
    viewModel: AdminTimetableViewModel = hiltViewModel<AdminTimetableViewModel>()
) {
    var showAddDialog by remember { mutableStateOf<Boolean>(false) }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var initialDay by remember { mutableStateOf<String>("Monday") }
    var initialPeriod by remember { mutableStateOf<String>("1") }
    var initialSubject by remember { mutableStateOf<String>("") }
    var initialTime by remember { mutableStateOf<String>("") }
    var initialRoom by remember { mutableStateOf<String>("") }
    var initialTeacherId by remember { mutableStateOf<Int?>(null) }
    var initialDate by remember { mutableStateOf<String>("") }
    var slotToDelete by remember { mutableStateOf<TimetableItem?>(null) }
    var hasAttemptedFetch by remember { mutableStateOf(false) }
    
    var showWefEditDialog by remember { mutableStateOf(false) }
    var showClassroomEditDialog by remember { mutableStateOf(false) }
    var activeTimePeriodEdit by remember { mutableStateOf<String?>(null) }
    var activeLegendSubject by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Timetable") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.selectedDepartment.isNotEmpty() && viewModel.selectedYear.isNotEmpty() && viewModel.selectedSection.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Slot")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Class Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.selectedDepartment,
                        onValueChange = { newVal -> viewModel.selectedDepartment = newVal },
                        label = { Text("Dept") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.selectedYear,
                        onValueChange = { newVal -> viewModel.selectedYear = newVal },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.selectedSection,
                        onValueChange = { newVal -> viewModel.selectedSection = newVal },
                        label = { Text("Sec") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.selectedSemester,
                        onValueChange = { newVal -> viewModel.selectedSemester = newVal },
                        label = { Text("Sem") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = { 
                        viewModel.loadTimetable() 
                        hasAttemptedFetch = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.selectedDepartment.isNotEmpty() && viewModel.selectedYear.isNotEmpty() && viewModel.selectedSection.isNotEmpty() && viewModel.selectedSemester.isNotEmpty()
                ) {
                    Text("Fetch Timetable")
                }
            }

            if (hasAttemptedFetch || viewModel.timetable.isNotEmpty()) {
                item {
                    Text("Schedule Preview", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    com.auraface.auraface_app.presentation.common.PremiumTimetable(
                        timetable = viewModel.timetable,
                        department = viewModel.selectedDepartment,
                        year = viewModel.selectedYear.toIntOrNull(),
                        semester = viewModel.selectedSemester.toIntOrNull(),
                        section = viewModel.selectedSection,
                        onSlotClick = { dayAbr, periodAbr ->
                            val fullDay = when (dayAbr.uppercase()) {
                                "MON" -> "Monday"
                                "TUE" -> "Tuesday"
                                "WED" -> "Wednesday"
                                "THU" -> "Thursday"
                                "FRI" -> "Friday"
                                "SAT" -> "Saturday"
                                else -> dayAbr
                            }
                            val altPeriod = when (periodAbr) { "1" -> "I"; "2" -> "II"; "3" -> "III"; "4" -> "IV"; "5" -> "V"; "6" -> "VI"; else -> periodAbr }
                            
                            val existing = viewModel.timetable.find { 
                                (it.day?.equals(fullDay, true) == true || it.day?.take(3)?.equals(dayAbr.take(3), true) == true) && 
                                (it.period == periodAbr || it.period?.uppercase() == altPeriod) 
                            }
                            
                            editingId = existing?.id
                            initialDay = existing?.day ?: fullDay
                            initialPeriod = existing?.period ?: periodAbr
                            initialSubject = existing?.subject ?: ""
                            initialTime = existing?.time ?: ""
                            initialRoom = existing?.room ?: ""
                            initialTeacherId = existing?.teacherId
                            initialDate = existing?.date ?: ""
                            showAddDialog = true
                        },
                        onTimeHeaderClick = { period ->
                            activeTimePeriodEdit = period
                        },
                        onWefClick = {
                            showWefEditDialog = true
                        },
                        onClassroomClick = {
                            showClassroomEditDialog = true
                        },
                        onLegendClick = { subject ->
                            activeLegendSubject = subject
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (viewModel.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (viewModel.errorMessage != null) {
                item {
                    Text(viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                val grouped = viewModel.timetable.groupBy { it.day ?: "Unknown" }
                val daysOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                
                daysOrder.forEach { day ->
                    val slots = grouped[day]
                    if (slots != null) {
                        item {
                            Text(day, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        items(slots.sortedBy { it.time }) { slot ->
                            TimetableManageCard(
                                item = slot,
                                onEdit = {
                                    editingId = slot.id
                                    initialDay = slot.day ?: ""
                                    initialPeriod = slot.period ?: ""
                                    initialSubject = slot.subject
                                    initialTime = slot.time ?: ""
                                    initialRoom = slot.room ?: ""
                                    initialTeacherId = slot.teacherId
                                    initialDate = slot.date ?: ""
                                    showAddDialog = true
                                },
                                onDelete = {
                                    slotToDelete = slot
                                },
                                onApprove = {
                                    viewModel.approveSlot(slot)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimetableSlotDialog(
            title = if (editingId != null) "Edit Slot" else "Add Slot",
            initialDay = initialDay,
            initialPeriod = initialPeriod,
            initialSubject = initialSubject,
            initialTime = initialTime,
            initialRoom = initialRoom,
            initialSemester = viewModel.selectedSemester,
            initialTeacherId = initialTeacherId,
            teachers = viewModel.teachersList,
            onDismiss = { 
                showAddDialog = false
                editingId = null
            },
            onDelete = if (editingId != null) {
                {
                    viewModel.deleteSlot(editingId!!)
                    showAddDialog = false
                    editingId = null
                }
            } else null,
            onConfirm = { day, time, subject, teacherId, period, room, semester ->
                // inherit existing date
                val inheritedDate = viewModel.timetable.mapNotNull { it.date?.takeIf { d -> d.isNotBlank() } }.firstOrNull()
                if (editingId != null) {
                    viewModel.updateSlot(editingId!!, day, time, subject, teacherId, period, room, semester, inheritedDate) {
                        showAddDialog = false
                        editingId = null
                    }
                } else {
                    viewModel.addSlot(day, time, subject, teacherId, period, room, semester, inheritedDate) {
                        showAddDialog = false
                    }
                }
            }
        )
    }

    slotToDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            title = { Text("Delete Slot") },
            text = { Text("Are you sure you want to delete ${slot.subject} on ${slot.day} at ${slot.time}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSlot(slot.id)
                        slotToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { slotToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWefEditDialog) {
        val initialDate = viewModel.timetable.mapNotNull { it.date?.takeIf { d -> d.isNotBlank() } }.firstOrNull() ?: ""
        var newDate by remember(initialDate) { mutableStateOf(initialDate) }
        AlertDialog(
            onDismissRequest = { showWefEditDialog = false },
            title = { Text("Edit W.E.F Date") },
            text = { OutlinedTextField(value = newDate, onValueChange = { newVal -> newDate = newVal }, label = { Text("Date (YYYY-MM-DD)") }) },
            confirmButton = { Button(onClick = { viewModel.bulkUpdateWefDate(newDate) { showWefEditDialog = false } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showWefEditDialog = false }) { Text("Cancel") } }
        )
    }

    if (activeLegendSubject != null) {
        val originalSubject = activeLegendSubject!!
        val sampleSlot = viewModel.timetable.firstOrNull { it.subject == originalSubject }
        
        var newSubject by remember(originalSubject) { mutableStateOf(originalSubject) }
        var selectedTeacher by remember(sampleSlot?.teacherId) { mutableStateOf<Teacher?>(viewModel.teachersList.find { it.id == sampleSlot?.teacherId }) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { activeLegendSubject = null },
            title = { Text("Update Subject globally") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newSubject, onValueChange = { newSubject = it }, label = { Text("Full Course Title") })
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTeacher?.full_name ?: selectedTeacher?.username ?: "Select Teacher",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Teacher") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedTeacher = null
                                    expanded = false
                                }
                            )
                            viewModel.teachersList.forEach { teacher ->
                                DropdownMenuItem(
                                    text = { Text(teacher.full_name ?: teacher.username) },
                                    onClick = {
                                        selectedTeacher = teacher
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.bulkUpdateSubjectAndTeacher(originalSubject, newSubject, selectedTeacher?.id) { activeLegendSubject = null } }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { activeLegendSubject = null }) { Text("Cancel") }
            }
        )
    }

    if (showClassroomEditDialog) {
        val initialRoom = viewModel.timetable.mapNotNull { it.room?.takeIf { r -> r.isNotBlank() } }.firstOrNull() ?: ""
        var newRoom by remember(initialRoom) { mutableStateOf(initialRoom) }
        AlertDialog(
            onDismissRequest = { showClassroomEditDialog = false },
            title = { Text("Edit Default Classroom") },
            text = { OutlinedTextField(value = newRoom, onValueChange = { newVal -> newRoom = newVal }, label = { Text("Room (e.g. CSB-04)") }) },
            confirmButton = { Button(onClick = { viewModel.bulkUpdateClassroom(newRoom) { showClassroomEditDialog = false } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showClassroomEditDialog = false }) { Text("Cancel") } }
        )
    }

    if (activeTimePeriodEdit != null) {
        val p = activeTimePeriodEdit!!
        val altP = when (p) { "1" -> "I"; "2" -> "II"; "3" -> "III"; "4" -> "IV"; "5" -> "V"; "6" -> "VI"; else -> p }
        val defaultKnownTime = viewModel.timetable.firstOrNull { it.period == p || it.period?.uppercase() == altP }?.time ?: ""
        var newTime by remember(p, defaultKnownTime) { mutableStateOf(defaultKnownTime) }
        
        AlertDialog(
            onDismissRequest = { activeTimePeriodEdit = null },
            title = { Text("Edit Time for Period $p") },
            text = { OutlinedTextField(value = newTime, onValueChange = { newVal -> newTime = newVal }, label = { Text("Time (e.g. 01:30 PM - 02:30 PM)") }) },
            confirmButton = { Button(onClick = { viewModel.bulkUpdatePeriodTime(p, newTime) { activeTimePeriodEdit = null } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { activeTimePeriodEdit = null }) { Text("Cancel") } }
        )
    }
}


@Composable
fun TimetableManageCard(item: TimetableItem, onEdit: () -> Unit, onDelete: () -> Unit, onApprove: () -> Unit) {
    val isPending = item.status == "PENDING"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.subject, fontWeight = FontWeight.Bold)
                Text("${item.time} (${item.period}) ${if (item.semester != null) "Sem: ${item.semester}" else ""}")
                if (item.date != null) {
                    Text("Date: ${item.date}", style = MaterialTheme.typography.bodySmall)
                }
                if (isPending) {
                     Text("PENDING APPROVAL", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                     if (!item.requestReason.isNullOrBlank()) {
                         Text("Reason: ${item.requestReason}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                     }
                }
                if (item.room != null) {
                    Text("Room: ${item.room}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.teacher != null) {
                    Text("Teacher: ${item.teacher}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                if (isPending) {
                    IconButton(onClick = onApprove) {
                         Icon(Icons.Default.Done, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddTimetableSlotDialog(
    title: String = "Add Slot",
    initialDay: String = "Monday",
    initialPeriod: String = "1",
    initialSubject: String = "",
    initialTime: String = "",
    initialRoom: String = "",
    initialSemester: String = "",
    initialTeacherId: Int? = null,
    teachers: List<Teacher> = emptyList(),
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (String, String, String, Int?, String, String?, Int?) -> Unit
) {
    var day by remember { mutableStateOf<String>(initialDay) }
    var time by remember { mutableStateOf<String>(initialTime) }
    var subject by remember { mutableStateOf<String>(initialSubject) }
    var selectedTeacher by remember { mutableStateOf<Teacher?>(teachers.find { it.id == initialTeacherId }) }
    var period by remember { mutableStateOf<String>(initialPeriod) }
    var room by remember { mutableStateOf<String>(initialRoom) }
    var sem by remember { mutableStateOf<String>(initialSemester) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Simplified day selection
                OutlinedTextField(value = day, onValueChange = { newVal -> day = newVal }, label = { Text("Day (e.g. Monday)") })
                OutlinedTextField(value = time, onValueChange = { newVal -> time = newVal }, label = { Text("Time (e.g. 09:00 - 10:00)") })
                OutlinedTextField(value = subject, onValueChange = { newVal -> subject = newVal }, label = { Text("Subject Name") })
                
                // Teacher Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedTeacher?.full_name ?: selectedTeacher?.username ?: "Select Teacher",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Teacher") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedTeacher = null
                                expanded = false
                            }
                        )
                        teachers.forEach { teacher ->
                            DropdownMenuItem(
                                text = { Text(teacher.full_name ?: teacher.username) },
                                onClick = {
                                    selectedTeacher = teacher
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = period, onValueChange = { newVal -> period = newVal }, label = { Text("Period (1, 2, 3...)") })
                OutlinedTextField(value = room, onValueChange = { newVal -> room = newVal }, label = { Text("Room/Classroom (e.g. CSB-04)") })
                OutlinedTextField(value = sem, onValueChange = { newVal -> sem = newVal }, label = { Text("Semester (e.g. 6)") })
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    onConfirm(day, time, subject, selectedTeacher?.id, period, room.ifBlank { null }, sem.toIntOrNull())
                }) {
                    Text(if (title.contains("Edit")) "Update" else "Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
