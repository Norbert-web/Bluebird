package com.win11launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.win11launcher.ui.theme.Win11Colors
import java.text.SimpleDateFormat
import java.util.*

data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0
)

data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val type: Int // 1=inbox, 2=sent
)

@Composable
fun MessagesScreen(isDark: Boolean) {
    val context = LocalContext.current
    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val bgColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5)
    val surfaceBg = if (isDark) Color(0xFF252525) else Color(0xFFEEEEEE)

    var threads by remember { mutableStateOf(listOf<SmsThread>()) }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    var messages by remember { mutableStateOf(listOf<SmsMessage>()) }
    var composeText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var newRecipient by remember { mutableStateOf("") }
    var showNewMessage by remember { mutableStateOf(false) }

    // Load SMS threads
    LaunchedEffect(Unit) {
        try {
            val cr = context.contentResolver
            val cursor = cr.query(
                Telephony.Sms.Conversations.CONTENT_URI,
                null, null, null,
                Telephony.Sms.Conversations.DEFAULT_SORT_ORDER
            )
            val result = mutableListOf<SmsThread>()
            cursor?.use { c ->
                val threadIdCol = c.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID)
                val snippetCol = c.getColumnIndex(Telephony.Sms.Conversations.SNIPPET)
                val msgCountCol = c.getColumnIndex(Telephony.Sms.Conversations.MESSAGE_COUNT)

                while (c.moveToNext()) {
                    val threadId = if (threadIdCol >= 0) c.getLong(threadIdCol) else continue
                    val snippet = if (snippetCol >= 0) c.getString(snippetCol) ?: "" else ""

                    // Get address from thread
                    val smsCursor = cr.query(
                        Telephony.Sms.CONTENT_URI,
                        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
                        "${Telephony.Sms.THREAD_ID} = ?",
                        arrayOf(threadId.toString()),
                        "${Telephony.Sms.DATE} DESC LIMIT 1"
                    )
                    var address = ""
                    var timestamp = 0L
                    smsCursor?.use { sc ->
                        if (sc.moveToFirst()) {
                            val addrCol = sc.getColumnIndex(Telephony.Sms.ADDRESS)
                            val dateCol = sc.getColumnIndex(Telephony.Sms.DATE)
                            address = if (addrCol >= 0) sc.getString(addrCol) ?: "" else ""
                            timestamp = if (dateCol >= 0) sc.getLong(dateCol) else 0L
                        }
                    }

                    if (address.isNotEmpty()) {
                        // Try to resolve contact name
                        val contactName = resolveContactName(cr, address)
                        result.add(SmsThread(threadId, address, contactName, snippet, timestamp))
                    }
                }
            }
            threads = result.sortedByDescending { it.timestamp }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Load messages for selected thread
    LaunchedEffect(selectedThread) {
        val thread = selectedThread ?: return@LaunchedEffect
        try {
            val cr = context.contentResolver
            val cursor = cr.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(thread.threadId.toString()),
                "${Telephony.Sms.DATE} ASC"
            )
            val result = mutableListOf<SmsMessage>()
            cursor?.use { c ->
                val idCol = c.getColumnIndex(Telephony.Sms._ID)
                val addrCol = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = c.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = c.getColumnIndex(Telephony.Sms.DATE)
                val typeCol = c.getColumnIndex(Telephony.Sms.TYPE)
                while (c.moveToNext()) {
                    result.add(SmsMessage(
                        id = if (idCol >= 0) c.getLong(idCol) else 0,
                        address = if (addrCol >= 0) c.getString(addrCol) ?: "" else "",
                        body = if (bodyCol >= 0) c.getString(bodyCol) ?: "" else "",
                        timestamp = if (dateCol >= 0) c.getLong(dateCol) else 0,
                        type = if (typeCol >= 0) c.getInt(typeCol) else 1
                    ))
                }
            }
            messages = result
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun sendSms(to: String, body: String) {
        if (to.isBlank() || body.isBlank()) return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$to")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) } catch (e: Exception) { e.printStackTrace() }
    }

    Row(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // ── Thread list (left) ──
        Column(
            modifier = Modifier.width(260.dp).fillMaxHeight().background(surfaceBg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Message, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Messages", color = textColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showNewMessage = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(18.dp))
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("Search messages", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Win11Colors.AccentBlue,
                    unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Divider(color = textColor.copy(alpha = 0.1f), modifier = Modifier.padding(top = 4.dp))

            val filtered = threads.filter {
                searchQuery.isEmpty() ||
                (it.contactName ?: it.address).contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = textColor.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                        Text(if (threads.isEmpty()) "No messages\n(Grant SMS permission)" else "No results", color = textColor.copy(alpha = 0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { thread ->
                        ThreadItem(
                            thread = thread,
                            isSelected = selectedThread?.threadId == thread.threadId,
                            textColor = textColor,
                            isDark = isDark,
                            onClick = { selectedThread = thread }
                        )
                    }
                }
            }
        }

        // ── Message view (right) ──
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedThread == null && !showNewMessage) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, null, tint = textColor.copy(alpha = 0.15f), modifier = Modifier.size(80.dp))
                        Text("Select a conversation", color = textColor.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { showNewMessage = true }) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Message")
                        }
                    }
                }
            } else {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isDark) Color(0xFF252525) else Color(0xFFEEEEEE))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedThread != null) {
                        IconButton(onClick = { selectedThread = null; showNewMessage = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, null, tint = textColor, modifier = Modifier.size(18.dp))
                        }
                        val displayName = selectedThread!!.contactName ?: selectedThread!!.address
                        Box(
                            modifier = Modifier.size(36.dp).background(Win11Colors.AccentBlue.copy(0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(displayName.firstOrNull()?.toString()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, color = textColor, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(selectedThread!!.address, color = textColor.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${selectedThread!!.address}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Call, null, tint = Win11Colors.SuccessGreen, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        // New message header
                        Icon(Icons.Default.Edit, null, tint = textColor)
                        OutlinedTextField(
                            value = newRecipient,
                            onValueChange = { newRecipient = it },
                            modifier = Modifier.weight(1f).height(40.dp),
                            placeholder = { Text("To: phone number or name", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Win11Colors.AccentBlue,
                                unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )
                    }
                }

                // Messages
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedThread != null) {
                        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                        items(messages) { msg ->
                            val isSent = msg.type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                                         msg.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                            MessageBubble(msg.body, isSent, sdf.format(Date(msg.timestamp)), isDark, textColor)
                        }
                    }
                }

                // Compose area
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isDark) Color(0xFF252525) else Color(0xFFEEEEEE))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = composeText,
                        onValueChange = { composeText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message", fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Win11Colors.AccentBlue,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(if (composeText.isNotBlank()) Win11Colors.AccentBlue else textColor.copy(alpha = 0.2f), CircleShape)
                            .clickable {
                                val to = selectedThread?.address ?: newRecipient
                                if (composeText.isNotBlank()) {
                                    sendSms(to, composeText)
                                    composeText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadItem(thread: SmsThread, isSelected: Boolean, textColor: Color, isDark: Boolean, onClick: () -> Unit) {
    val displayName = thread.contactName ?: thread.address
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (isSelected) Win11Colors.AccentBlue.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Win11Colors.AccentBlue.copy(0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(displayName.firstOrNull()?.toString()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, color = textColor, fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (thread.timestamp > 0) {
                    Text(sdf.format(Date(thread.timestamp)), color = textColor.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }
            Text(thread.lastMessage, color = textColor.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MessageBubble(body: String, isSent: Boolean, time: String, isDark: Boolean, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isSent) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 4.dp,
                        bottomEnd = if (isSent) 4.dp else 16.dp
                    ))
                    .background(if (isSent) Win11Colors.AccentBlue else if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(body, color = if (isSent) Color.White else textColor, fontSize = 13.sp)
            }
            Text(time, color = textColor.copy(alpha = 0.4f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}

private fun resolveContactName(cr: android.content.ContentResolver, phoneNumber: String): String? {
    return try {
        val uri = Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = cr.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
        var name: String? = null
        cursor?.use {
            if (it.moveToFirst()) {
                val col = it.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (col >= 0) name = it.getString(col)
            }
        }
        name
    } catch (e: Exception) { null }
}
