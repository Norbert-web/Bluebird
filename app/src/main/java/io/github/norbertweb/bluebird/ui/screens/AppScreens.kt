package io.github.norbertweb.bluebird.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import io.github.norbertweb.bluebird.ui.components.FluentIcon
import io.github.norbertweb.bluebird.ui.components.LocalWindowRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
//  DESIGN TOKENS  (macOS-like frosted glass palette)
// ─────────────────────────────────────────────────────────────────────────────

private object Mac {
    // Backgrounds
    val surfaceDark   = Color(0xFF1E1E1E)
    val surfaceLight  = Color(0xFFF2F2F7)
    val panelDark     = Color(0xFF2A2A2A)
    val panelLight    = Color(0xFFFFFFFF)
    val sidebarDark   = Color(0xFF1C1C1E)
    val sidebarLight  = Color(0xFFF5F5F5)

    // Glass
    val glassDark     = Color(0x44FFFFFF)
    val glassLight    = Color(0x88FFFFFF)
    val borderDark    = Color(0x22FFFFFF)
    val borderLight   = Color(0x33000000)

    // Accent
    val blue          = Color(0xFF007AFF)
    val blueSoft      = Color(0xFF5AC8FA)
    val green         = Color(0xFF34C759)
    val orange        = Color(0xFFFF9500)
    val red           = Color(0xFFFF3B30)
    val yellow        = Color(0xFFFFCC00)
    val purple        = Color(0xFFAF52DE)
    val pink          = Color(0xFFFF2D55)
    val teal          = Color(0xFF5AC8FA)

    // Text
    val textPrimaryDark    = Color(0xFFFFFFFF)
    val textPrimaryLight   = Color(0xFF1C1C1E)
    val textSecondaryDark  = Color(0xFF8E8E93)
    val textSecondaryLight = Color(0xFF6C6C70)
    val textTertiaryDark   = Color(0xFF48484A)
    val textTertiaryLight  = Color(0xFFAEAEB2)
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHARED COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    val border = if (isDark) Mac.borderDark else Mac.borderLight
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

@Composable
private fun SectionHeader(text: String, isDark: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        ),
        color = if (isDark) Mac.textSecondaryDark else Mac.textSecondaryLight,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  CALCULATOR  — Real functional with history, scientific mode, animations
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CalculatorScreen(isDark: Boolean) {
    val bg     = if (isDark) Mac.surfaceDark  else Mac.surfaceLight
    val panel  = if (isDark) Mac.panelDark    else Mac.panelLight
    val text   = if (isDark) Mac.textPrimaryDark else Mac.textPrimaryLight
    val subtle = if (isDark) Mac.textSecondaryDark else Mac.textSecondaryLight

    // ── State ──────────────────────────────────────────────────────────────
    var display     by remember { mutableStateOf("0") }
    var expression  by remember { mutableStateOf("") }
    var firstNum    by remember { mutableStateOf(0.0) }
    var secondNum   by remember { mutableStateOf(0.0) }
    var pendingOp   by remember { mutableStateOf("") }
    var newNumber   by remember { mutableStateOf(true) }
    var justEvaled  by remember { mutableStateOf(false) }
    var history     by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var isScientific by remember { mutableStateOf(false) }
    var isDegrees   by remember { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(true) }
    var memory      by remember { mutableStateOf(0.0) }
    var hasMemory   by remember { mutableStateOf(false) }

    // ── Display size ───────────────────────────────────────────────────────
    val fontSize = when {
        display.length > 14 -> 22.sp
        display.length > 10 -> 30.sp
        display.length > 7  -> 38.sp
        else                -> 52.sp
    }

    // ── Core logic ─────────────────────────────────────────────────────────
    fun toRad(deg: Double) = if (isDegrees) Math.toRadians(deg) else deg

    fun applyOp(a: Double, op: String, b: Double): Double = when (op) {
        "+"  -> a + b
        "-"  -> a - b
        "×"  -> a * b
        "÷"  -> if (b != 0.0) a / b else Double.NaN
        "%"  -> a % b
        "xʸ" -> a.pow(b)
        else -> b
    }

    fun applyUnary(op: String, a: Double): Double = when (op) {
        "sin"   -> sin(toRad(a))
        "cos"   -> cos(toRad(a))
        "tan"   -> tan(toRad(a))
        "sin⁻¹" -> if (isDegrees) Math.toDegrees(asin(a)) else asin(a)
        "cos⁻¹" -> if (isDegrees) Math.toDegrees(acos(a)) else acos(a)
        "tan⁻¹" -> if (isDegrees) Math.toDegrees(atan(a)) else atan(a)
        "ln"    -> ln(a)
        "log₁₀" -> log10(a)
        "log₂"  -> log2(a)
        "√"     -> sqrt(a)
        "∛"     -> a.pow(1.0 / 3)
        "x²"    -> a * a
        "x³"    -> a * a * a
        "1/x"   -> if (a != 0.0) 1.0 / a else Double.NaN
        "n!"    -> { var r = 1.0; for (i in 2..a.toInt()) r *= i; r }
        "eˣ"    -> exp(a)
        "10ˣ"   -> 10.0.pow(a)
        "+/-"   -> -a
        "%"     -> a / 100
        "π"     -> Math.PI
        "e"     -> Math.E
        else    -> a
    }

    fun fmt(v: Double): String {
        if (v.isNaN()) return "Error"
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        return if (v == v.toLong().toDouble() && abs(v) < 1e15) v.toLong().toString()
        else "%.10g".format(v).trimEnd('0').trimEnd('.')
    }

    fun handleInput(value: String) {
        when (value) {
            // ── Clear / back ──────────────────────────────────────────────
            "AC" -> {
                display = "0"; expression = ""; firstNum = 0.0
                pendingOp = ""; newNumber = true; justEvaled = false
            }
            "C"  -> { display = "0"; newNumber = true }
            "⌫"  -> {
                if (!newNumber && display != "0") {
                    display = if (display.length == 1 || (display.length == 2 && display.startsWith("-")))
                        "0" else display.dropLast(1)
                }
            }
            // ── Memory ────────────────────────────────────────────────────
            "MC" -> { memory = 0.0; hasMemory = false }
            "MR" -> { display = fmt(memory); newNumber = true }
            "M+" -> { memory += display.toDoubleOrNull() ?: 0.0; hasMemory = true }
            "M-" -> { memory -= display.toDoubleOrNull() ?: 0.0; hasMemory = true }
            "MS" -> { memory = display.toDoubleOrNull() ?: 0.0; hasMemory = true }
            // ── Constants ─────────────────────────────────────────────────
            "π", "e" -> {
                display = fmt(applyUnary(value, 0.0))
                newNumber = true
            }
            // ── Unary ops ─────────────────────────────────────────────────
            "sin","cos","tan","sin⁻¹","cos⁻¹","tan⁻¹",
            "ln","log₁₀","log₂","√","∛","x²","x³",
            "1/x","n!","eˣ","10ˣ","+/-","%" -> {
                val a = display.toDoubleOrNull() ?: 0.0
                val result = applyUnary(value, a)
                val exprStr = "$value($display)"
                if (justEvaled) expression = "$exprStr ="
                display = fmt(result)
                newNumber = true
                if (!result.isNaN() && !result.isInfinite())
                    history = (listOf(exprStr to display) + history).take(30)
            }
            // ── Binary ops ────────────────────────────────────────────────
            "+", "-", "×", "÷", "%", "xʸ" -> {
                val a = display.toDoubleOrNull() ?: 0.0
                if (pendingOp.isNotEmpty() && !newNumber) {
                    val result = applyOp(firstNum, pendingOp, a)
                    display = fmt(result); firstNum = result
                } else {
                    firstNum = a
                }
                pendingOp = value
                expression = "$display $value"
                newNumber = true; justEvaled = false
            }
            // ── Equals ────────────────────────────────────────────────────
            "=" -> {
                val b = display.toDoubleOrNull() ?: 0.0
                secondNum = b
                val result = applyOp(firstNum, pendingOp, b)
                val expr = "$expression $display ="
                history = (listOf(expr to fmt(result)) + history).take(30)
                expression = expr
                display = fmt(result)
                firstNum = result
                newNumber = true; justEvaled = true
            }
            // ── Decimal ───────────────────────────────────────────────────
            "." -> {
                if (newNumber) { display = "0."; newNumber = false }
                else if (!display.contains(".")) display += "."
            }
            // ── Digits ───────────────────────────────────────────────────
            else -> {
                if (newNumber) { display = value; newNumber = false }
                else display = if (display == "0") value else display + value
                if (justEvaled) { expression = ""; justEvaled = false }
            }
        }
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    Row(modifier = Modifier.fillMaxSize().background(bg)) {

        // History sidebar
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(if (isDark) Mac.sidebarDark else Mac.sidebarLight)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("History", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall, color = text)
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { history = emptyList() }, contentPadding = PaddingValues(4.dp)) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall, color = Mac.blue)
                        }
                    }
                }
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No history", style = MaterialTheme.typography.bodySmall, color = subtle)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(history) { (expr, result) ->
                            GlassCard(isDark, Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(expr, style = MaterialTheme.typography.labelSmall,
                                        color = subtle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(result, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium, color = text, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main calculator
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(if (isDark) Mac.panelDark else Mac.panelLight)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { showHistory = !showHistory }, modifier = Modifier.size(32.dp)) {
                    Icon(FluentIcon.History, null, tint = subtle, modifier = Modifier.size(18.dp))
                }
                Text(if (isScientific) "Scientific" else "Standard",
                    style = MaterialTheme.typography.labelMedium, color = subtle)
                Spacer(Modifier.weight(1f))
                if (isDegrees || isScientific) {
                    if (isScientific) {
                        TextButton(onClick = { isDegrees = !isDegrees }, contentPadding = PaddingValues(6.dp)) {
                            Text(if (isDegrees) "DEG" else "RAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Mac.blue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                TextButton(onClick = { isScientific = !isScientific }, contentPadding = PaddingValues(6.dp)) {
                    Text(if (isScientific) "Standard" else "Scientific",
                        style = MaterialTheme.typography.labelSmall, color = Mac.blue)
                }
            }

            // Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (hasMemory) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Mac.blue)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("M", style = MaterialTheme.typography.labelSmall,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (expression.isNotEmpty()) {
                    Text(expression, style = MaterialTheme.typography.bodySmall,
                        color = subtle, maxLines = 2, textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = display,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = fontSize, fontWeight = FontWeight.Light
                    ),
                    color = text,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)

            // Keypad
            val numBg = if (isDark) Color(0xFF3A3A3C) else Color(0xFFFFFFFF)
            val opBg  = if (isDark) Color(0xFF2C2C2E) else Color(0xFFD1D1D6)
            val acBg  = if (isDark) Color(0xFF48484A) else Color(0xFFAEAEB2)

            val standardRows = listOf(
                listOf("MC" to acBg, "MR" to acBg, "M+" to acBg, "M-" to acBg, "MS" to acBg),
                listOf("AC" to acBg, "+/-" to acBg, "%" to acBg, "÷" to Mac.orange),
                listOf("7" to numBg, "8" to numBg, "9" to numBg, "×" to Mac.orange),
                listOf("4" to numBg, "5" to numBg, "6" to numBg, "-" to Mac.orange),
                listOf("1" to numBg, "2" to numBg, "3" to numBg, "+" to Mac.orange),
                listOf("0" to numBg, "." to numBg, "⌫" to numBg, "=" to Mac.orange),
            )

            val scientificRows = listOf(
                listOf("sin" to opBg, "cos" to opBg, "tan" to opBg, "sin⁻¹" to opBg, "cos⁻¹" to opBg, "tan⁻¹" to opBg),
                listOf("ln" to opBg, "log₁₀" to opBg, "log₂" to opBg, "√" to opBg, "∛" to opBg, "xʸ" to opBg),
                listOf("n!" to opBg, "eˣ" to opBg, "10ˣ" to opBg, "x²" to opBg, "x³" to opBg, "1/x" to opBg),
                listOf("π" to opBg, "e" to opBg, "MC" to acBg, "MR" to acBg, "M+" to acBg, "MS" to acBg),
            )

            val rows = if (isScientific) scientificRows + standardRows.drop(1) else standardRows

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { (label, bg) ->
                            val isSpecial = bg == Mac.orange
                            CalcButton(
                                label = label,
                                bg = bg,
                                textColor = if (isSpecial) Color.White else text,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { handleInput(label) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    label: String,
    bg: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = Spring.StiffnessHigh))

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CALENDAR  — Real month navigation, event creation, week view
// ─────────────────────────────────────────────────────────────────────────────

data class CalEvent(
    val id: Int,
    val title: String,
    val time: String,
    val endTime: String,
    val color: Color,
    val day: Int,
    val month: Int,
    val year: Int,
    val isAllDay: Boolean = false,
    val location: String = ""
)

@Composable
fun CalendarScreen(isDark: Boolean) {
    val bg     = if (isDark) Mac.surfaceDark  else Mac.surfaceLight
    val panel  = if (isDark) Mac.panelDark    else Mac.panelLight
    val text   = if (isDark) Mac.textPrimaryDark else Mac.textPrimaryLight
    val subtle = if (isDark) Mac.textSecondaryDark else Mac.textSecondaryLight
    val sidebar= if (isDark) Mac.sidebarDark  else Mac.sidebarLight

    val systemCal = remember { java.util.Calendar.getInstance() }
    var displayMonth by remember { mutableStateOf(systemCal.get(java.util.Calendar.MONTH)) }
    var displayYear  by remember { mutableStateOf(systemCal.get(java.util.Calendar.YEAR)) }
    var selectedDay  by remember { mutableStateOf(systemCal.get(java.util.Calendar.DAY_OF_MONTH)) }
    var viewMode     by remember { mutableStateOf("Month") } // Month | Week | Day
    var showAddEvent by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }

    val today = java.util.Calendar.getInstance()
    val todayDay   = today.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(java.util.Calendar.MONTH)
    val todayYear  = today.get(java.util.Calendar.YEAR)

    val monthNames = listOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")
    val dayHeaders = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    var events by remember {
        mutableStateOf(
            listOf(
                CalEvent(1, "Team Standup",      "9:00",  "9:30",  Mac.blue,   todayDay, todayMonth, todayYear),
                CalEvent(2, "Design Review",     "11:00", "12:00", Mac.purple, todayDay, todayMonth, todayYear),
                CalEvent(3, "Lunch Break",       "12:30", "13:30", Mac.green,  todayDay, todayMonth, todayYear, location = "Cafeteria"),
                CalEvent(4, "Sprint Planning",   "14:00", "15:30", Mac.orange, todayDay, todayMonth, todayYear),
                CalEvent(5, "1:1 with Manager",  "16:00", "16:30", Mac.pink,   todayDay, todayMonth, todayYear),
                CalEvent(6, "Project Deadline",  "",      "",      Mac.red,    (todayDay + 2).coerceIn(1, 28), todayMonth, todayYear, isAllDay = true),
                CalEvent(7, "Team Offsite",      "",      "",      Mac.teal,   (todayDay + 5).coerceIn(1, 28), todayMonth, todayYear, isAllDay = true),
                CalEvent(8, "Q3 Planning",       "10:00", "11:00", Mac.yellow, (todayDay + 1).coerceIn(1, 28), todayMonth, todayYear),
            )
        )
    }

    val selectedEvents = events.filter {
        it.day == selectedDay && it.month == displayMonth && it.year == displayYear
    }

    // Grid helpers
    val firstDayOfWeek = java.util.Calendar.getInstance().apply {
        set(displayYear, displayMonth, 1)
    }.get(java.util.Calendar.DAY_OF_WEEK) - 1

    val daysInMonth = java.util.Calendar.getInstance().apply {
        set(displayYear, displayMonth, 1)
    }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    val cells = (0 until firstDayOfWeek).map { null } +
            (1..daysInMonth).map { it } +
            (0 until (7 - (firstDayOfWeek + daysInMonth) % 7) % 7).map { null }

    fun eventDotsForDay(day: Int): List<Color> = events
        .filter { it.day == day && it.month == displayMonth && it.year == displayYear && !it.isAllDay }
        .take(3).map { it.color }

    fun allDayForDay(day: Int): CalEvent? = events
        .firstOrNull { it.day == day && it.month == displayMonth && it.year == displayYear && it.isAllDay }

    Row(modifier = Modifier.fillMaxSize().background(bg)) {

        // ── Left sidebar: mini-calendar + calendars list ──────────────────
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(sidebar)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add event button
            Button(
                onClick = { showAddEvent = true },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mac.blue),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(FluentIcon.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Event", style = MaterialTheme.typography.labelMedium)
            }

            // View mode
            GlassCard(isDark, Modifier.fillMaxWidth()) {
                Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("Month", "Week", "Day").forEach { mode ->
                        val active = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Mac.blue else Color.Transparent)
                                .pointerInput(Unit) { detectTapGestures(onTap = { viewMode = mode }) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode, style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else subtle)
                        }
                    }
                }
            }

            // Calendar sources
            SectionHeader("My Calendars", isDark, Modifier.padding(0.dp))
            listOf(
                Triple("Personal", Mac.blue,   true),
                Triple("Work",     Mac.green,  true),
                Triple("Birthdays",Mac.orange, true),
                Triple("Holidays", Mac.red,    false),
            ).forEach { (name, color, checked) ->
                var isChecked by remember { mutableStateOf(checked) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isChecked) color else Color.Transparent)
                            .border(1.5.dp, color, RoundedCornerShape(3.dp))
                            .pointerInput(Unit) { detectTapGestures(onTap = { isChecked = !isChecked }) }
                    )
                    Text(name, style = MaterialTheme.typography.bodySmall, color = text)
                }
            }
        }

        // ── Main calendar area ────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(panel)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${monthNames[displayMonth]} $displayYear",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = text
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    displayMonth = todayMonth; displayYear = todayYear; selectedDay = todayDay
                }) {
                    Text("Today", style = MaterialTheme.typography.labelMedium, color = Mac.blue)
                }
                Row {
                    IconButton(onClick = {
                        if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
                    }, modifier = Modifier.size(34.dp)) {
                        Icon(FluentIcon.ChevronLeft, null, tint = subtle, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = {
                        if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
                    }, modifier = Modifier.size(34.dp)) {
                        Icon(FluentIcon.ChevronRight, null, tint = subtle, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)

            // Day-of-week headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(panel)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayHeaders.forEach { d ->
                    Text(d, style = MaterialTheme.typography.labelSmall,
                        color = subtle, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center)
                }
            }

            HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)

            // Calendar grid
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFF9F9F9))
            ) {
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        week.forEach { day ->
                            val isToday   = day == todayDay && displayMonth == todayMonth && displayYear == todayYear
                            val isSelected = day == selectedDay && day != null
                            val dots      = if (day != null) eventDotsForDay(day) else emptyList()
                            val allDay    = if (day != null) allDayForDay(day) else null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.3.dp, if (isDark) Color(0x18FFFFFF) else Color(0x18000000))
                                    .background(
                                        when {
                                            isSelected && !isToday -> Mac.blue.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .pointerInput(day) {
                                        detectTapGestures(onTap = { if (day != null) selectedDay = day })
                                    }
                                    .padding(4.dp)
                            ) {
                                Column {
                                    // Day number
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isToday    -> Mac.blue
                                                    isSelected -> Mac.blue.copy(alpha = 0.15f)
                                                    else       -> Color.Transparent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (day != null) {
                                            Text(
                                                day.toString(),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = when {
                                                    isToday -> Color.White
                                                    isSelected -> Mac.blue
                                                    else -> text
                                                }
                                            )
                                        }
                                    }

                                    // All-day event pill
                                    if (allDay != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(allDay.color.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(allDay.title, style = MaterialTheme.typography.labelSmall,
                                                color = allDay.color, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                fontSize = 9.sp)
                                        }
                                    }

                                    // Event dots
                                    if (dots.isNotEmpty()) {
                                        Spacer(Modifier.height(3.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            dots.forEach { c ->
                                                Box(Modifier.size(5.dp).clip(CircleShape).background(c))
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

        // ── Right panel: selected day events ─────────────────────────────
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(if (isDark) Mac.panelDark else Mac.panelLight)
                .padding(16.dp)
        ) {
            Text(
                text = if (selectedDay == todayDay && displayMonth == todayMonth && displayYear == todayYear)
                    "Today, ${monthNames[displayMonth]} $selectedDay"
                else "${monthNames[displayMonth]} $selectedDay, $displayYear",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = text
            )

            Spacer(Modifier.height(12.dp))

            if (selectedEvents.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No events", style = MaterialTheme.typography.bodySmall, color = subtle)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedEvents.forEach { ev ->
                        GlassCard(isDark, Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier.width(3.dp).height(40.dp)
                                        .clip(RoundedCornerShape(2.dp)).background(ev.color)
                                )
                                Column {
                                    Text(ev.title, style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold), color = text, maxLines = 1)
                                    if (ev.isAllDay) {
                                        Text("All day", style = MaterialTheme.typography.labelSmall, color = subtle)
                                    } else {
                                        Text("${ev.time} – ${ev.endTime}",
                                            style = MaterialTheme.typography.labelSmall, color = subtle)
                                    }
                                    if (ev.location.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(FluentIcon.Location, null,
                                                modifier = Modifier.size(10.dp), tint = subtle)
                                            Text(ev.location, style = MaterialTheme.typography.labelSmall, color = subtle)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // Add event inline
            if (showAddEvent) {
                GlassCard(isDark, Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("New Event", style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold), color = text)
                        OutlinedTextField(
                            value = newEventTitle,
                            onValueChange = { newEventTitle = it },
                            placeholder = { Text("Event title", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { showAddEvent = false; newEventTitle = "" },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel", style = MaterialTheme.typography.labelSmall) }
                            Button(
                                onClick = {
                                    if (newEventTitle.isNotEmpty()) {
                                        val newId = (events.maxOfOrNull { it.id } ?: 0) + 1
                                        events = events + CalEvent(
                                            newId, newEventTitle, "12:00", "13:00",
                                            Mac.blue, selectedDay, displayMonth, displayYear
                                        )
                                        newEventTitle = ""
                                        showAddEvent = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(6.dp)
                            ) { Text("Add", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showAddEvent = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(FluentIcon.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Event", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PHOTOS  — Album grid, photo detail, favorite, delete, search
// ─────────────────────────────────────────────────────────────────────────────

data class PhotoItem1(
    val id: Int,
    val color: Color,
    val secondaryColor: Color,
    val label: String,
    val date: String,
    val isFavorite: Boolean = false,
    val album: String = "Camera Roll"
)

@Composable
fun PhotosScreen(isDark: Boolean) {
    val bg     = if (isDark) Mac.surfaceDark  else Mac.surfaceLight
    val panel  = if (isDark) Mac.panelDark    else Mac.panelLight
    val text   = if (isDark) Mac.textPrimaryDark else Mac.textPrimaryLight
    val subtle = if (isDark) Mac.textSecondaryDark else Mac.textSecondaryLight
    val sidebar= if (isDark) Mac.sidebarDark  else Mac.sidebarLight

    val photos = remember {
        val palette = listOf(
            Color(0xFF0A2463) to Color(0xFF3E92CC),
            Color(0xFF1B4332) to Color(0xFF52B788),
            Color(0xFF6A0572) to Color(0xFFDE5AF0),
            Color(0xFF7B2D00) to Color(0xFFE07B39),
            Color(0xFF1A1A2E) to Color(0xFF6B8CFF),
            Color(0xFF4A0E0E) to Color(0xFFE85D04),
            Color(0xFF003049) to Color(0xFF457B9D),
            Color(0xFF264653) to Color(0xFF2A9D8F),
            Color(0xFF370617) to Color(0xFFF48C06),
            Color(0xFF10002B) to Color(0xFF7B2FBE),
            Color(0xFF013220) to Color(0xFF3CB371),
            Color(0xFF1C0221) to Color(0xFFC77DFF),
            Color(0xFF240046) to Color(0xFF9D4EDD),
            Color(0xFF0D1B2A) to Color(0xFF00B4D8),
            Color(0xFF1A0000) to Color(0xFFD00000),
            Color(0xFF001219) to Color(0xFF94D2BD),
        )
        val dates = listOf("Today", "Today", "Today", "Yesterday", "Yesterday",
            "May 12", "May 12", "May 11", "May 10", "May 9",
            "May 8", "May 7", "May 6", "May 5", "May 4", "May 3")
        val albums = listOf("Camera Roll","Screenshots","Favorites","Camera Roll","Screenshots",
            "Camera Roll","Camera Roll","Favorites","Camera Roll","Screenshots",
            "Camera Roll","Camera Roll","Camera Roll","Favorites","Screenshots","Camera Roll")
        palette.mapIndexed { i, (c1, c2) ->
            PhotoItem1(i, c1, c2, "Photo ${i+1}", dates[i], i % 5 == 0, albums[i])
        }
    }

    var photoState by remember { mutableStateOf(photos) }
    var selectedPhoto by remember { mutableStateOf<PhotoItem1?>(null) }
    var selectedTab by remember { mutableStateOf("Library") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf("All") }

    val tabs = listOf("Library", "Albums", "Favorites")
    val albums = listOf("All") + photos.map { it.album }.distinct()

    val displayed = photoState.filter { photo ->
        (selectedAlbum == "All" || photo.album == selectedAlbum) &&
                (searchQuery.isEmpty() || photo.label.contains(searchQuery, ignoreCase = true) ||
                        photo.date.contains(searchQuery, ignoreCase = true)) &&
                (selectedTab != "Favorites" || photo.isFavorite)
    }

    fun groupByDate(list: List<PhotoItem1>) = list.groupBy { it.date }

    Row(modifier = Modifier.fillMaxSize().background(bg)) {

        // Sidebar
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(sidebar)
                .padding(top = 8.dp)
        ) {
            // Tabs
            tabs.forEach { tab ->
                val active = selectedTab == tab
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Mac.blue.copy(alpha = 0.15f) else Color.Transparent)
                        .pointerInput(tab) { detectTapGestures(onTap = { selectedTab = tab }) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (tab) {
                            "Library"   -> FluentIcon.ImageMultiple
                            "Favorites" -> FluentIcon.Favorite
                            else        -> FluentIcon.ImageMultiple
                        },
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (active) Mac.blue else subtle
                    )
                    Text(tab, style = MaterialTheme.typography.bodySmall,
                        color = if (active) Mac.blue else text,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }

            if (selectedTab == "Albums") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isDark) Mac.borderDark else Mac.borderLight
                )
                SectionHeader("Albums", isDark, Modifier.padding(0.dp))
                albums.forEach { album ->
                    val active = selectedAlbum == album
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) Mac.blue.copy(alpha = 0.12f) else Color.Transparent)
                            .pointerInput(album) { detectTapGestures(onTap = { selectedAlbum = album }) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(album, style = MaterialTheme.typography.bodySmall,
                            color = if (active) Mac.blue else text)
                        Text(
                            photoState.count { it.album == album || album == "All" }.toString(),
                            style = MaterialTheme.typography.labelSmall, color = subtle
                        )
                    }
                }
            }
        }

        // Main content
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(panel)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSearching) {
                    GlassCard(isDark, Modifier.weight(1f).height(32.dp)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(FluentIcon.Search, null, tint = subtle, modifier = Modifier.size(14.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = text),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("Search photos…",
                                        style = MaterialTheme.typography.bodySmall, color = subtle)
                                    inner()
                                }
                            )
                        }
                    }
                    IconButton(onClick = { isSearching = false; searchQuery = "" }, Modifier.size(32.dp)) {
                        Icon(FluentIcon.Close, null, tint = subtle, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text("${displayed.size} Photos", style = MaterialTheme.typography.labelMedium, color = subtle)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { isSearching = true }, Modifier.size(34.dp)) {
                        Icon(FluentIcon.Search, null, tint = subtle, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)

            // Photo grid or detail
            if (selectedPhoto != null) {
                val photo = selectedPhoto!!
                Row(modifier = Modifier.fillMaxSize()) {
                    // Large preview
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Gradient photo placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.85f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.radialGradient(listOf(photo.secondaryColor, photo.color)))
                        ) {
                            Icon(FluentIcon.Image, null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.align(Alignment.Center).size(80.dp))
                        }
                    }
                    // Info panel
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .background(panel)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { selectedPhoto = null }, Modifier.size(32.dp)) {
                            Icon(FluentIcon.ArrowBack, null, tint = subtle, modifier = Modifier.size(18.dp))
                        }
                        Text(photo.label, style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold), color = text)
                        Text(photo.date, style = MaterialTheme.typography.bodySmall, color = subtle)
                        Text("Album: ${photo.album}", style = MaterialTheme.typography.bodySmall, color = subtle)

                        HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)

                        // Actions
                        listOf(
                            FluentIcon.Favorite to if (photo.isFavorite) "Remove Favorite" else "Add to Favorites",
                            FluentIcon.Share to "Share",
                            FluentIcon.ArrowDownload to "Save to Device",
                            FluentIcon.Delete to "Delete"
                        ).forEach { (icon, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .pointerInput(label) {
                                        detectTapGestures(onTap = {
                                            if (label.contains("Favorite")) {
                                                photoState = photoState.map {
                                                    if (it.id == photo.id) it.copy(isFavorite = !it.isFavorite) else it
                                                }
                                                selectedPhoto = photoState.find { it.id == photo.id }
                                            } else if (label == "Delete") {
                                                photoState = photoState.filter { it.id != photo.id }
                                                selectedPhoto = null
                                            }
                                        })
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null,
                                    tint = if (label == "Delete") Mac.red
                                    else if (label.contains("Favorite") && photo.isFavorite) Mac.pink
                                    else Mac.blue,
                                    modifier = Modifier.size(16.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall,
                                    color = if (label == "Delete") Mac.red else text)
                            }
                        }
                    }
                }
            } else {
                // Grid view grouped by date
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(bg),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupByDate(displayed).forEach { (date, photosInGroup) ->
                        item {
                            Text(date, style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold), color = text)
                        }
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(90.dp),
                                modifier = Modifier.fillMaxWidth().height(
                                    (ceil(photosInGroup.size / 4.0) * 96).dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(photosInGroup) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.radialGradient(listOf(photo.secondaryColor, photo.color))
                                            )
                                            .pointerInput(photo) {
                                                detectTapGestures(onTap = { selectedPhoto = photo })
                                            }
                                    ) {
                                        Icon(FluentIcon.Image, null,
                                            tint = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.align(Alignment.Center).size(24.dp))
                                        if (photo.isFavorite) {
                                            Icon(FluentIcon.Favorite, null,
                                                tint = Mac.pink,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                                    .padding(4.dp).size(12.dp))
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
}

// ─────────────────────────────────────────────────────────────────────────────
//  TASK MANAGER  — Real live-updating charts, kill process, sortable columns
// ─────────────────────────────────────────────────────────────────────────────

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val cpu: Float,
    val mem: Float,    // MB
    val disk: Float,   // MB/s
    val net: Float,    // KB/s
    val status: String = "Running",
    val type: String = "App"   // App | Background | System
)

@Composable
fun TaskManagerScreen(isDark: Boolean) {
    val windowRuntime = LocalWindowRuntime.current
    val bg     = if (isDark) Mac.surfaceDark  else Mac.surfaceLight
    val panel  = if (isDark) Mac.panelDark    else Mac.panelLight
    val text   = if (isDark) Mac.textPrimaryDark else Mac.textPrimaryLight
    val subtle = if (isDark) Mac.textSecondaryDark else Mac.textSecondaryLight

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Processes", "Performance", "Energy", "Network")

    // Live CPU/Mem history
    var cpuHistory  by remember { mutableStateOf(List(60) { (15..45).random().toFloat() }) }
    var memHistory  by remember { mutableStateOf(List(60) { (40..60).random().toFloat() }) }
    var netHistory  by remember { mutableStateOf(List(60) { (0..80).random().toFloat() }) }
    var diskHistory by remember { mutableStateOf(List(60) { (0..30).random().toFloat() }) }

    var processes by remember {
        mutableStateOf(listOf(
            ProcessInfo(1,   "System Idle",           0.3f,   8f,  0f,   0f, "Running", "System"),
            ProcessInfo(2,   "bluebird Launcher",        3.1f, 156f,  1.2f, 12f, "Running", "App"),
            ProcessInfo(3,   "Google Chrome",        18.4f, 624f,  3.8f,240f, "Running", "App"),
            ProcessInfo(104, "Chrome Helper",         6.2f, 210f,  0.4f, 80f, "Running", "App"),
            ProcessInfo(4,   "Spotify",               2.1f, 180f,  0f,  45f, "Running", "App"),
            ProcessInfo(5,   "File Explorer",         0.8f,  52f,  0.1f, 0f, "Running", "App"),
            ProcessInfo(6,   "Settings",              0.2f,  40f,  0f,   0f, "Running", "App"),
            ProcessInfo(7,   "Antimalware Service",   4.8f, 102f,  2.4f, 0f, "Running", "Background"),
            ProcessInfo(8,   "Windows Audio",         0.1f,  28f,  0f,   0f, "Running", "System"),
            ProcessInfo(9,   "Search Indexer",        5.2f,  88f,  8.4f, 0f, "Running", "Background"),
            ProcessInfo(10,  "Windows Update",        0.0f,  34f,  0f,   8f, "Running", "Background"),
            ProcessInfo(11,  "Task Manager",          1.0f,  38f,  0f,   0f, "Running", "App"),
            ProcessInfo(12,  "OneDrive",              0.4f,  76f,  1.0f,32f, "Running", "Background"),
            ProcessInfo(13,  "Runtime Broker",        0.2f,  22f,  0f,   0f, "Running", "System"),
            ProcessInfo(14,  "Cortana",               0.1f,  44f,  0f,   4f, "Running", "Background"),
        ))
    }

    // Animate CPU values
    LaunchedEffect(windowRuntime.isMinimized) {
        while (isActive) {
            if (windowRuntime.isMinimized) {
                // Minimized Task Manager keeps its state but stops generating
                // fake telemetry/recomposition work until restored.
                delay(5_000)
                continue
            }
            delay(1000)
            processes = processes.map { p ->
                val delta = (-3..3).random().toFloat()
                p.copy(cpu = (p.cpu + delta).coerceIn(0f, 99f))
            }
            val newCpu = processes.sumOf { it.cpu.toDouble() }.toFloat() /
                    processes.size.toFloat() * 2f
            cpuHistory = (cpuHistory.drop(1) + newCpu.coerceIn(0f,100f))
            memHistory = (memHistory.drop(1) + ((memHistory.last() + (-2..2).random()).toFloat().coerceIn(30f,90f)))
            netHistory = (netHistory.drop(1) + ((netHistory.last() + (-10..15).random()).toFloat().coerceIn(0f,100f)))
            diskHistory = (diskHistory.drop(1) + ((diskHistory.last() + (-5..8).random()).toFloat().coerceIn(0f,100f)))
        }
    }

    val currentCpu = cpuHistory.last()
    val currentMem = memHistory.last()

    var sortCol by remember { mutableStateOf("cpu") }
    var sortAsc  by remember { mutableStateOf(false) }

    fun sortedProcs(): List<ProcessInfo> {
        val comparator: Comparator<ProcessInfo> = when (sortCol) {
            "name" -> compareBy { it.name }
            "cpu"  -> compareBy { it.cpu }
            "mem"  -> compareBy { it.mem }
            "disk" -> compareBy { it.disk }
            "net"  -> compareBy { it.net }
            else   -> compareBy { it.pid }
        }
        return if (sortAsc) processes.sortedWith(comparator)
        else processes.sortedWith(comparator).reversed()
    }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {

        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(panel)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = selectedTab == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Mac.blue.copy(alpha = 0.15f) else Color.Transparent)
                        .pointerInput(i) { detectTapGestures(onTap = { selectedTab = i }) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab, style = MaterialTheme.typography.labelMedium,
                        color = if (active) Mac.blue else subtle,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.weight(1f))
            // Live indicator
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Mac.green))
                Text("Live", style = MaterialTheme.typography.labelSmall, color = Mac.green)
            }
        }

        HorizontalDivider(color = if (isDark) Mac.borderDark else Mac.borderLight, thickness = 0.5.dp)

        when (selectedTab) {
            0 -> ProcessesTab(isDark, sortedProcs(), sortCol, sortAsc, text, subtle,
                onSort = { col ->
                    if (sortCol == col) sortAsc = !sortAsc else { sortCol = col; sortAsc = false }
                },
                onKill = { pid -> processes = processes.filter { it.pid != pid } }
            )
            1 -> PerformanceTab2(isDark, cpuHistory, memHistory, diskHistory, netHistory,
                currentCpu, currentMem, processes.size, text, subtle)
            2 -> EnergyTab(isDark, processes, text, subtle)
            3 -> NetworkTab(isDark, netHistory, text, subtle)
        }
    }
}

@Composable
private fun ProcessesTab(
    isDark: Boolean,
    procs: List<ProcessInfo>,
    sortCol: String, sortAsc: Boolean,
    text: Color, subtle: Color,
    onSort: (String) -> Unit,
    onKill: (Int) -> Unit
) {
    var selected by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(if (isDark) Color(0xFF252525) else Color(0xFFECECEC))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Composable
            fun SortHeader(label: String, col: String, weight: Float) {
                Row(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .pointerInput(col) { detectTapGestures(onTap = { onSort(col) }) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (col == "name") Arrangement.Start else Arrangement.End
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (sortCol == col) if (isDark) Mac.textPrimaryDark else Mac.textPrimaryLight
                        else subtle,
                        fontWeight = if (sortCol == col) FontWeight.SemiBold else FontWeight.Normal)
                    if (sortCol == col) {
                        Icon(
                            if (sortAsc) FluentIcon.ArrowUpward else FluentIcon.ArrowDownward,
                            null, tint = Mac.blue, modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            SortHeader("Process Name", "name", 3f)
            SortHeader("CPU", "cpu", 1f)
            SortHeader("Memory", "mem", 1f)
            SortHeader("Disk", "disk", 1f)
            SortHeader("Network", "net", 1f)
            Spacer(Modifier.width(50.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(procs, key = { it.pid }) { proc ->
                val isSelected = selected == proc.pid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            if (isSelected) Mac.blue.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .pointerInput(proc.pid) { detectTapGestures(onTap = { selected = proc.pid }) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type icon
                    Icon(
                        when (proc.type) {
                            "System" -> FluentIcon.Settings
                            "Background" -> FluentIcon.Refresh
                            else -> FluentIcon.Apps
                        },
                        null,
                        tint = when (proc.type) {
                            "System" -> subtle
                            "Background" -> Mac.orange
                            else -> Mac.blue
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(proc.name, style = MaterialTheme.typography.bodySmall,
                        color = text, modifier = Modifier.weight(3f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)

                    // CPU bar + value
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(30.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6))) {
                            Box(Modifier.fillMaxHeight()
                                .fillMaxWidth(proc.cpu / 100f)
                                .background(
                                    when {
                                        proc.cpu > 50 -> Mac.red
                                        proc.cpu > 20 -> Mac.orange
                                        else -> Mac.green
                                    }
                                ))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f%%".format(proc.cpu), style = MaterialTheme.typography.labelSmall,
                            color = when {
                                proc.cpu > 50 -> Mac.red
                                proc.cpu > 20 -> Mac.orange
                                else -> text
                            })
                    }

                    Text("%.0f MB".format(proc.mem), style = MaterialTheme.typography.labelSmall,
                        color = text, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text("%.1f MB/s".format(proc.disk), style = MaterialTheme.typography.labelSmall,
                        color = text, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text("%.0f KB/s".format(proc.net), style = MaterialTheme.typography.labelSmall,
                        color = text, modifier = Modifier.weight(1f), textAlign = TextAlign.End)

                    // End task button
                    if (isSelected && proc.type != "System") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Mac.red.copy(alpha = 0.15f))
                                .pointerInput(proc.pid) { detectTapGestures(onTap = { onKill(proc.pid) }) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("End Task", style = MaterialTheme.typography.labelSmall, color = Mac.red)
                        }
                    } else {
                        Spacer(Modifier.width(50.dp))
                    }
                }
                HorizontalDivider(
                    color = if (isDark) Color(0x0AFFFFFF) else Color(0x0A000000),
                    thickness = 0.5.dp
                )
            }
        }

        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(if (isDark) Color(0xFF252525) else Color(0xFFECECEC))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("${procs.size} processes", style = MaterialTheme.typography.labelSmall, color = subtle)
            Text("CPU: ${"%.0f".format(procs.sumOf { it.cpu.toDouble() } / procs.size * 2)}%",
                style = MaterialTheme.typography.labelSmall, color = subtle)
            Text("Memory: ${"%.0f".format(procs.sumOf { it.mem.toDouble() })} MB",
                style = MaterialTheme.typography.labelSmall, color = subtle)
        }
    }
}

@Composable
private fun PerformanceTab2(
    isDark: Boolean,
    cpuHistory: List<Float>,
    memHistory: List<Float>,
    diskHistory: List<Float>,
    netHistory: List<Float>,
    currentCpu: Float,
    currentMem: Float,
    processCount: Int,
    text: Color,
    subtle: Color
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: metric list
        Column(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight()
                .background(if (isDark) Mac.sidebarDark else Mac.sidebarLight)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple("CPU",    "%.0f%%".format(currentCpu), Mac.blue),
                Triple("Memory", "%.0f%%".format(currentMem), Mac.green),
                Triple("Disk",   "${diskHistory.last().toInt()}%", Mac.orange),
                Triple("Network","${netHistory.last().toInt()} KB/s", Mac.purple),
                Triple("GPU",    "12%",  Mac.pink),
            ).forEach { (label, value, color) ->
                GlassCard(isDark, Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = subtle)
                        }
                        Text(value, style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold), color = text)
                    }
                }
            }
        }

        // Right: charts
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CPU chart
            PerfChart(
                isDark = isDark,
                title = "CPU Utilization",
                subtitle = "%.1f%%".format(currentCpu),
                history = cpuHistory,
                lineColor = Mac.blue,
                text = text, subtle = subtle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerfStat2("Processes", processCount.toString(), text, subtle, Modifier.weight(1f))
                PerfStat2("Threads", "1,248", text, subtle, Modifier.weight(1f))
                PerfStat2("Handles", "42,816", text, subtle, Modifier.weight(1f))
                PerfStat2("Uptime", "3:14:22", text, subtle, Modifier.weight(1f))
            }

            PerfChart(
                isDark = isDark,
                title = "Memory Usage",
                subtitle = "%.1f%%".format(currentMem),
                history = memHistory,
                lineColor = Mac.green,
                text = text, subtle = subtle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerfStat2("In Use", "6.2 GB", text, subtle, Modifier.weight(1f))
                PerfStat2("Available", "9.8 GB", text, subtle, Modifier.weight(1f))
                PerfStat2("Total", "16 GB", text, subtle, Modifier.weight(1f))
                PerfStat2("Cached", "4.1 GB", text, subtle, Modifier.weight(1f))
            }

            PerfChart(
                isDark = isDark,
                title = "Disk Activity",
                subtitle = "${diskHistory.last().toInt()}%",
                history = diskHistory,
                lineColor = Mac.orange,
                text = text, subtle = subtle
            )

            PerfChart(
                isDark = isDark,
                title = "Network",
                subtitle = "${netHistory.last().toInt()} KB/s",
                history = netHistory,
                lineColor = Mac.purple,
                text = text, subtle = subtle
            )
        }
    }
}

@Composable
private fun PerfChart(
    isDark: Boolean,
    title: String,
    subtitle: String,
    history: List<Float>,
    lineColor: Color,
    text: Color,
    subtle: Color
) {
    GlassCard(isDark) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold), color = text)
                Text(subtitle, style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold), color = lineColor)
            }
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) Color(0x0CFFFFFF) else Color(0x06000000))
            ) {
                val pts = history
                val step = size.width / (pts.size - 1)

                // Fill
                val fillPath = Path()
                pts.forEachIndexed { i, v ->
                    val x = i * step
                    val y = size.height * (1f - v / 100f)
                    if (i == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
                }
                fillPath.lineTo(size.width, size.height)
                fillPath.lineTo(0f, size.height)
                fillPath.close()
                drawPath(fillPath, brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0f))
                ))

                // Line
                val linePath = Path()
                pts.forEachIndexed { i, v ->
                    val x = i * step
                    val y = size.height * (1f - v / 100f)
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, color = lineColor, style = Stroke(width = 1.5f))

                // Grid lines
                listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                    drawLine(
                        color = lineColor.copy(alpha = 0.1f),
                        start = Offset(0f, size.height * frac),
                        end   = Offset(size.width, size.height * frac),
                        strokeWidth = 0.5f
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("60s ago", style = MaterialTheme.typography.labelSmall, color = subtle)
                Text("Now",     style = MaterialTheme.typography.labelSmall, color = subtle)
            }
        }
    }
}

@Composable
private fun PerfStat2(label: String, value: String, text: Color, subtle: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = subtle)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = text)
    }
}

@Composable
private fun EnergyTab(isDark: Boolean, procs: List<ProcessInfo>, text: Color, subtle: Color) {
    val bg = if (isDark) Mac.surfaceDark else Mac.surfaceLight
    Column(Modifier.fillMaxSize().background(bg)) {
        SectionHeader("Energy Impact", isDark)
        val sorted = procs.sortedByDescending { it.cpu + it.mem / 50 }
        LazyColumn(Modifier.fillMaxSize()) {
            items(sorted) { proc ->
                val energy = ((proc.cpu * 2 + proc.mem / 50)).coerceIn(0f, 100f)
                Row(
                    Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(proc.name, style = MaterialTheme.typography.bodySmall,
                        color = text, modifier = Modifier.weight(3f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val barColor = when {
                        energy > 60 -> Mac.red
                        energy > 30 -> Mac.orange
                        else -> Mac.green
                    }
                    Box(Modifier.weight(2f).height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(energy / 100f)
                            .clip(RoundedCornerShape(3.dp)).background(barColor))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        energy > 60 -> "High"
                        energy > 30 -> "Medium"
                        else -> "Low"
                    }, style = MaterialTheme.typography.labelSmall, color = barColor,
                        modifier = Modifier.width(50.dp))
                }
                HorizontalDivider(color = if (isDark) Color(0x0AFFFFFF) else Color(0x0A000000), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun NetworkTab(isDark: Boolean, netHistory: List<Float>, text: Color, subtle: Color) {
    val bg = if (isDark) Mac.surfaceDark else Mac.surfaceLight
    Column(Modifier.fillMaxSize().background(bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Network Activity", isDark, Modifier.padding(0.dp))

        val interfaces = listOf(
            Triple("Wi-Fi (wlan0)",  netHistory.last(),  Mac.blue),
            Triple("Ethernet (eth0)", 0f,               Mac.green),
            Triple("Loopback (lo)",   0.5f,             Mac.orange),
        )
        interfaces.forEach { (name, speed, color) ->
            GlassCard(isDark, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(FluentIcon.Wifi, null, tint = color, modifier = Modifier.size(16.dp))
                            Text(name, style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold), color = text)
                        }
                        Text("%.1f KB/s".format(speed), style = MaterialTheme.typography.labelMedium, color = color)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column {
                            Text("↑ Sent", style = MaterialTheme.typography.labelSmall, color = subtle)
                            Text("${(speed * 0.4f).toInt()} KB/s", style = MaterialTheme.typography.labelMedium, color = text)
                        }
                        Column {
                            Text("↓ Received", style = MaterialTheme.typography.labelSmall, color = subtle)
                            Text("${(speed * 0.6f).toInt()} KB/s", style = MaterialTheme.typography.labelMedium, color = text)
                        }
                        Column {
                            Text("Packets", style = MaterialTheme.typography.labelSmall, color = subtle)
                            Text("${(speed * 2).toInt()}/s", style = MaterialTheme.typography.labelMedium, color = text)
                        }
                    }
                }
            }
        }

        GlassCard(isDark, Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Live Throughput", style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold), color = text)
                Spacer(Modifier.height(8.dp))
                PerfChart(isDark, "Network", "${netHistory.last().toInt()} KB/s",
                    netHistory, Mac.purple, text, subtle)
            }
        }
    }
}

// Kept helper for old references
fun formatResult(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.10f".format(value).trimEnd('0').trimEnd('.')
