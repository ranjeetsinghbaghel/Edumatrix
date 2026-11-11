package com.auraface.auraface_app.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraface.auraface_app.R
import com.auraface.auraface_app.data.network.model.TimetableItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PremiumTimetable(
    timetable: List<TimetableItem>,
    department: String?,
    year: Int?,
    semester: Int? = null,
    section: String?,
    onSlotClick: ((String, String) -> Unit)? = null,
    onTimeHeaderClick: ((String) -> Unit)? = null,
    onWefClick: (() -> Unit)? = null,
    onClassroomClick: (() -> Unit)? = null,
    onLegendClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(Color.White)
    ) {
        // --- 1. Top University Header Box ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black)
                .padding(2.dp)
                .border(1.dp, Color.Black) // double border effect
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_playstore),
                    contentDescription = "GIET Logo",
                    modifier = Modifier.size(60.dp),
                    contentScale = ContentScale.Inside
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GANDHI INSTITUTE OF ENGINEERING AND TECHNOLOGY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F), // Red
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "UNIVERSITY, ODISHA, GUNUPUR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F), // Red
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SCHOOL OF ENGINEERING & TECHNOLOGY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF6A1B9A), // Purple
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. Ref Date section ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ref. No. : GIETU/SoET/2025-26/05/TT",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium
            )
            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
            Text(
                text = "Date: $currentDate",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Titles ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val semLabel = when (semester) {
                1 -> "I"
                2 -> "II"
                3 -> "III"
                4 -> "IV"
                5 -> "V"
                6 -> "VI"
                7 -> "VII"
                8 -> "VIII"
                else -> if (semester != null && semester > 0) semester.toString() else "VII"
            }

            Text(
                text = "$semLabel SEMESTER B. TECH TIMETABLE 2026-27",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color(0xFF0D47A1), // Blue
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = department?.uppercase() ?: "COMPUTER SCIENCE AND ENGINEERING",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF8D6E63), // Brownish
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "SECTION - ${section?.uppercase() ?: "G"}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val wefDate = timetable.mapNotNull { it.date?.takeIf { d -> d.isNotBlank() } }.firstOrNull() ?: "12/09/26"
        val headerRoom = timetable.mapNotNull { it.room?.takeIf { r -> r.isNotBlank() } }.firstOrNull() ?: "AME-16"

        // --- 4. Boxes above table ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Black)
                    .clickable(enabled = onClassroomClick != null, onClick = { onClassroomClick?.invoke() })
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("ROOM NO.: $headerRoom", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Black)
                    .clickable(enabled = onWefClick != null, onClick = { onWefClick?.invoke() })
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("W. E. F.: $wefDate", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 5. Main Timetable Grid ---
        val scrollState = rememberScrollState()
        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
        val cellHeight = 45.dp
        
        val distinctPeriods = timetable.mapNotNull { it.period }.distinct()
        val isThreePeriodLayout = distinctPeriods.isEmpty() || distinctPeriods.all { it in listOf("1", "2", "3", "I", "II", "III") }
        
        val getPeriodTime = { p: String, pRoman: String, default: String ->
            val t = timetable.firstOrNull { it.period == p || it.period?.uppercase() == pRoman }?.time ?: default
            if (t.contains("-") && !t.contains("\n")) t.replace("-", "-\n") else t
        }

        val colDayWidth = 75.dp
        val colDataWidth = if (isThreePeriodLayout) 100.dp else 95.dp
        val colBreakWidth = 40.dp
        
        // Define columns visually
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .border(2.dp, Color.Black)
                .padding(2.dp)
                .border(1.dp, Color.Black)
        ) {
            if (isThreePeriodLayout) {
                // Exact 3-Period Grid matching VII Semester CSE Section-G Timetable
                Row {
                    // Column 0: DAY
                    Column(modifier = Modifier.width(colDayWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "PERIOD /\nDAY", time = "", height = cellHeight * 1.5f)
                        days.forEach { day ->
                            TimetableDayCell(day, cellHeight)
                        }
                    }

                    // Column 1: I
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "I", time = getPeriodTime("1", "I", "10:00 AM -\n11:00 AM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("1") })
                        days.forEach { day ->
                            if (day == "WED") {
                                // Full width Project or standard cell
                                TimetableDataCell(timetable, day, "1", cellHeight, onSlotClick, customSubject = "PROJECT")
                            } else {
                                TimetableDataCell(timetable, day, "1", cellHeight, onSlotClick)
                            }
                        }
                    }

                    // Column 2: II
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "II", time = getPeriodTime("2", "II", "11:00 AM -\n12:00 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("2") })
                        days.forEach { day ->
                            if (day == "WED") {
                                TimetableDataCell(timetable, day, "2", cellHeight, onSlotClick, customSubject = "PROJECT")
                            } else {
                                TimetableDataCell(timetable, day, "2", cellHeight, onSlotClick)
                            }
                        }
                    }

                    // Column 3: III
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "III", time = getPeriodTime("3", "III", "12:00 PM -\n01:00 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("3") })
                        days.forEach { day ->
                            if (day == "WED") {
                                TimetableDataCell(timetable, day, "3", cellHeight, onSlotClick, customSubject = "PROJECT", isLast = true)
                            } else {
                                TimetableDataCell(timetable, day, "3", cellHeight, onSlotClick, isLast = true)
                            }
                        }
                    }
                }
            } else {
                Row {
                    // Column 0: DAY
                    Column(modifier = Modifier.width(colDayWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "PERIOD /\nDAY", time = "", height = cellHeight * 1.5f)
                        days.forEach { day ->
                            TimetableDayCell(day, cellHeight)
                        }
                    }
                    
                    // Column 1: I
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "I", time = getPeriodTime("1", "I", "01:30 PM -\n02:30 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("1") })
                        days.forEach { day ->
                            TimetableDataCell(timetable, day, "1", cellHeight, onSlotClick)
                        }
                    }

                    // Column 2: II
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "II", time = getPeriodTime("2", "II", "02:30 PM -\n03:30 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("2") })
                        days.forEach { day ->
                            TimetableDataCell(timetable, day, "2", cellHeight, onSlotClick)
                        }
                    }

                    // Column 3: BREAK (Vertical)
                    Column(modifier = Modifier.width(colBreakWidth).border(1.dp, Color.Black)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cellHeight * 1.5f)
                                .border(1.dp, Color.Black),
                            contentAlignment = Alignment.Center
                        ) { }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cellHeight * 6)
                                .border(1.dp, Color.Black)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "B\nR\nE\nA\nK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                color = Color.Black
                            )
                        }
                    }

                    // Column 4: III
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "III", time = getPeriodTime("3", "III", "03:40 PM -\n04:40 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("3") })
                        days.forEach { day ->
                            TimetableDataCell(timetable, day, "3", cellHeight, onSlotClick)
                        }
                    }

                    // Column 5: IV
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "IV", time = getPeriodTime("4", "IV", "04:40 PM -\n05:40 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("4") })
                        days.forEach { day ->
                            TimetableDataCell(timetable, day, "4", cellHeight, onSlotClick)
                        }
                    }

                    // Column 6: V
                    Column(modifier = Modifier.width(colDataWidth).border(1.dp, Color.Black)) {
                        TimetableHeaderCell(title = "V", time = getPeriodTime("5", "V", "05:40 PM -\n06:40 PM"), height = cellHeight * 1.5f, onClick = { onTimeHeaderClick?.invoke("5") })
                        days.forEach { day ->
                            TimetableDataCell(timetable, day, "5", cellHeight, onSlotClick, isLast = true)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // --- 6. Course Title legend table ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black)
                .padding(2.dp)
                .border(1.dp, Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Legend Header
                Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                    LegendCell("SL. NO.", 0.8f, isHeader = true)
                    LegendCell("COURSE TITLE", 3.2f, isHeader = true)
                    LegendCell("ACRONYM", 1.2f, isHeader = true)
                    LegendCell("NAME OF THE FACULTY", 2.8f, isHeader = true)
                    LegendCell("DEPT.", 1.2f, isHeader = true)
                }
                
                // Realistic timetable legend rows matching image exactly
                val defaultSevenGSubjects = listOf(
                    listOf("1", "Computer Graphics", "CG", "Mr. Kedarnath Panda", "CSE"),
                    listOf("2", "Compiler Design", "CD", "Dr. D. Anil Kumar", "CSE(Allied)"),
                    listOf("3", "Data Analytics", "DA", "Dr. Bhabani Sankar Panda", "CSE"),
                    listOf("4", "Computer Networks and Security", "CNS", "Ms. Supriya Satapathy", "CSE"),
                    listOf("5", "Entrepreneurship and Enterprise Development", "EED", "Dr. A. Chiranjibi Rambabu Achary", "BSH")
                )

                val legendData = if (isThreePeriodLayout) {
                    defaultSevenGSubjects
                } else {
                    timetable
                        .filter { it.subject.isNotBlank() && it.subject != "***" }
                        .distinctBy { it.subject }
                        .mapIndexed { index, item ->
                            val rawSubj = item.subjectName ?: item.subject
                            val acronym = item.subject
                            val dept = when {
                                item.teacher?.contains("Anil") == true -> "CSE(Allied)"
                                item.teacher?.contains("Chiranjibi") == true -> "BSH"
                                else -> item.department?.take(3)?.uppercase() ?: "CSE"
                            }
                            listOf(
                                (index + 1).toString(),
                                rawSubj,
                                acronym,
                                item.teacher ?: "Unknown",
                                dept
                            )
                        }.takeIf { it.isNotEmpty() } ?: defaultSevenGSubjects
                }
                
                legendData.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onLegendClick != null) { onLegendClick?.invoke(row[1]) }
                    ) {
                        LegendCell(row[0], 0.8f)
                        LegendCell(row[1], 3.2f, alignLeft = true)
                        LegendCell(row[2], 1.2f)
                        LegendCell(row[3], 2.8f, alignLeft = true)
                        LegendCell(row[4], 1.2f)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- 7. Signature Footer matching image ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Prepared by:",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Black
            )
            Text(
                text = "Mrs. A. Jyoshana",
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Color.Black
            )
            Text(
                text = "Department Timetable Coordinator",
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                color = Color.DarkGray
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TimetableHeaderCell(title: String, time: String, height: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color.Black)
            .background(Color.White)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (time.isEmpty()) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
        } else {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            HorizontalDivider(thickness = 1.dp, color = Color.Black, modifier = Modifier.padding(vertical = 2.dp))
            Text(time, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TimetableDayCell(day: String, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color.Black)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(day, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun TimetableDataCell(
    timetable: List<TimetableItem>,
    day: String,
    period: String,
    height: androidx.compose.ui.unit.Dp,
    onClick: ((String, String) -> Unit)?,
    isLast: Boolean = false,
    customSubject: String? = null
) {
    // Map numerical periods mapping to standard roman numerals admins might type
    val alternativePeriod = when (period) {
        "1" -> "I"
        "2" -> "II"
        "3" -> "III"
        "4" -> "IV"
        "5" -> "V"
        "6" -> "VI"
        else -> period
    }

    val slot = timetable.find { 
        (it.day?.take(3)?.equals(day, true) == true || it.day?.equals(day, true) == true) && 
        (it.period == period || it.period?.uppercase() == alternativePeriod)
    }
    
    val isEmptyCell = slot?.subject.isNullOrBlank() && customSubject.isNullOrBlank()
    val bgColor = if (isEmptyCell && isLast) Color(0xFFFFCDD2) else Color.White // Pinkish for empty trailing slots
    val displaySubj = when {
        !customSubject.isNullOrBlank() -> customSubject
        isEmptyCell && isLast -> "***"
        else -> {
            val subj = slot?.subject ?: ""
            when (subj.uppercase().trim()) {
                "PROJECT" -> "PROJECT"
                "SEMINAR" -> "SEMINAR"
                "CNS" -> "CNS"
                "CD" -> "CD"
                "DA" -> "DA"
                "ED" -> "ED"
                "CG" -> "CG"
                else -> {
                    if (subj.contains(" ") || subj.contains("-")) {
                        subj.split(" ", "-", "_").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(4)
                    } else {
                        subj.uppercase().take(7)
                    }
                }
            }
        }
    }

    val isRedHighlight = (day.startsWith("MON", true) && (period == "1" || period == "I")) ||
                         (day.startsWith("THU", true) && (period == "1" || period == "I")) ||
                         (day.startsWith("FRI", true) && (period == "2" || period == "II")) ||
                         (day.startsWith("SAT", true) && (period == "2" || period == "II"))

    val textColor = when {
        isEmptyCell && isLast -> Color.Gray
        isRedHighlight -> Color(0xFFC62828) // Distinct red/maroon as in timetable
        else -> Color.Black
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color.Black)
            .background(bgColor)
            .clickable(enabled = onClick != null) { onClick?.invoke(day, period) }
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = displaySubj,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            color = textColor
        )
        if (!slot?.room.isNullOrBlank() && displaySubj != "PROJECT" && displaySubj != "***") {
            Text(
                text = "[${slot?.room}]",
                fontSize = 8.sp,
                color = Color.DarkGray,
                lineHeight = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
fun RowScope.LegendCell(text: String, weight: Float, isHeader: Boolean = false, alignLeft: Boolean = false) {
    Box(
        modifier = Modifier
            .weight(weight)
            .border(0.5.dp, Color.Black)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = if (alignLeft) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            color = Color.Black,
            textAlign = if (alignLeft) TextAlign.Start else TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}
