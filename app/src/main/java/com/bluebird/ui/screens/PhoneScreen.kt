package com.bluebird.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.bluebird.ui.theme.Win11Colors
import java.text.SimpleDateFormat
import java.util.*

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val initial: String = name.firstOrNull()?.toString()?.uppercase() ?: "?"
)

data class CallLogEntry(
    val number: String,
    val name: String?,
    val type: Int, // 1=incoming, 2=outgoing, 3=missed
    val duration: Long,
    val timestamp: Long
)

@Composable
fun PhoneScreen(isDark: Boolean) {
    val context = LocalContext.current
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val bgColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val surfaceBg = if (isDark) Color(0xFF252525) else Color(0xFFEEEEEE)

    var selectedTab by remember { mutableStateOf(0) }
    var dialNumber by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(listOf<Contact>()) }
    var callLog by remember { mutableStateOf(listOf<CallLogEntry>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Load contacts
    LaunchedEffect(Unit) {
        val cr = context.contentResolver
        val cursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        val result = mutableListOf<Contact>()
        cursor?.use {
            val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = if (idCol >= 0) it.getString(idCol) else ""
                val name = if (nameCol >= 0) it.getString(nameCol) ?: "Unknown" else "Unknown"
                val num = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                if (num.isNotEmpty()) result.add(Contact(id, name, num))
            }
        }
        contacts = result.distinctBy { it.phoneNumber }
    }

    // Load call log
    LaunchedEffect(Unit) {
        try {
            val cr = context.contentResolver
            val cursor = cr.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            )
            val result = mutableListOf<CallLogEntry>()
            cursor?.use {
                val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                val durCol = it.getColumnIndex(CallLog.Calls.DURATION)
                val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
                var count = 0
                while (it.moveToNext() && count < 100) {
                    result.add(CallLogEntry(
                        number = if (numCol >= 0) it.getString(numCol) ?: "" else "",
                        name = if (nameCol >= 0) it.getString(nameCol) else null,
                        type = if (typeCol >= 0) it.getInt(typeCol) else 0,
                        duration = if (durCol >= 0) it.getLong(durCol) else 0L,
                        timestamp = if (dateCol >= 0) it.getLong(dateCol) else 0L
                    ))
                    count++
                }
            }
            callLog = result
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun makeCall(number: String) {
        if (number.isBlank()) return
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) } catch (e: Exception) { e.printStackTrace() }
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Tab bar
        val tabs = listOf("Keypad", "Recents", "Contacts")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = surfaceBg,
            contentColor = Win11Colors.AccentBlue
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(label, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            // ── Keypad ──
            0 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.weight(1f))

                    // Number display
                    Text(
                        dialNumber.ifEmpty { "Enter number" },
                        color = if (dialNumber.isEmpty()) textColor.copy(alpha = 0.3f) else textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    // Keypad grid
                    val keys = listOf(
                        listOf("1", "2\nABC", "3\nDEF"),
                        listOf("4\nGHI", "5\nJKL", "6\nMNO"),
                        listOf("7\nPQRS", "8\nTUV", "9\nWXYZ"),
                        listOf("*", "0\n+", "#")
                    )
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                val digit = key.lines().first()
                                val sub = key.lines().getOrNull(1) ?: ""
                                Box(
                                    modifier = Modifier.size(72.dp)
                                        .clip(CircleShape)
                                        .background(surfaceBg)
                                        .clickable { dialNumber += digit },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(digit, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Light)
                                        if (sub.isNotEmpty()) Text(sub, color = textColor.copy(alpha = 0.4f), fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Bottom row: backspace + call + empty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.size(72.dp))

                        // Call button
                        Box(
                            modifier = Modifier.size(72.dp)
                                .background(Win11Colors.SuccessGreen, CircleShape)
                                .clickable { makeCall(dialNumber) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        // Backspace
                        Box(
                            modifier = Modifier.size(72.dp)
                                .clip(CircleShape)
                                .clickable { if (dialNumber.isNotEmpty()) dialNumber = dialNumber.dropLast(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backspace, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(Modifier.weight(1f))
                }
            }

            // ── Recents ──
            1 -> {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    if (callLog.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CallEnd, null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                                    Text("No recent calls", color = textColor.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                    items(callLog) { entry ->
                        CallLogItem(entry, textColor, surfaceBg, isDark, onCall = { makeCall(entry.number) })
                    }
                }
            }

            // ── Contacts ──
            2 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Search contacts") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Win11Colors.AccentBlue,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    val filtered = contacts.filter {
                        searchQuery.isEmpty() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery)
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PersonOff, null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                                Text(if (contacts.isEmpty()) "No contacts found\n(Grant contacts permission)" else "No results", color = textColor.copy(alpha = 0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                            items(filtered) { contact ->
                                ContactItem(contact, textColor, surfaceBg, onCall = { makeCall(contact.phoneNumber) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, textColor: Color, surfaceBg: Color, onCall: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { onCall() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Win11Colors.AccentBlue.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(contact.initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(contact.phoneNumber, color = textColor.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Call, null, tint = Win11Colors.SuccessGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CallLogItem(entry: CallLogEntry, textColor: Color, surfaceBg: Color, isDark: Boolean, onCall: () -> Unit) {
    val icon = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
        CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
        else -> Icons.Default.Call
    }
    val iconColor = when (entry.type) {
        CallLog.Calls.MISSED_TYPE -> Win11Colors.DangerRed
        CallLog.Calls.INCOMING_TYPE -> Win11Colors.SuccessGreen
        else -> Win11Colors.AccentBlue
    }
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { onCall() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name ?: entry.number, color = textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${sdf.format(Date(entry.timestamp))} · ${if (entry.duration > 0) formatDuration(entry.duration * 1000) else "No answer"}",
                color = textColor.copy(alpha = 0.5f), fontSize = 11.sp
            )
        }
        IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Call, null, tint = Win11Colors.SuccessGreen, modifier = Modifier.size(18.dp))
        }
    }
}
