package com.win11launcher.browser.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════
// FloatingKeyboard
// A compact custom keyboard that suppresses the Android system IME.
// readOnly=true on BasicTextField prevents IME from ever triggering.
// ═══════════════════════════════════════════════════════════════════════

private val kbRowsAlpha = listOf(
    listOf("q","w","e","r","t","y","u","i","o","p"),
    listOf("a","s","d","f","g","h","j","k","l"),
    listOf("⇧","z","x","c","v","b","n","m","⌫"),
    listOf("123","@","/","-","space",".","com","↵")
)
private val kbRowsNum = listOf(
    listOf("1","2","3","4","5","6","7","8","9","0"),
    listOf("!","@","#","\$","%","^","&","*","(",")"),
    listOf("+","=","_","[","]","{","}","\\","|","⌫"),
    listOf("ABC","<",">",";","\"","'",",",".","↵")
)

@Composable
fun FloatingKeyboard(
    currentText: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    isDark: Boolean
) {
    var isUppercase by remember { mutableStateOf(false) }
    var showNumeric  by remember { mutableStateOf(false) }
    val rows = if (showNumeric) kbRowsNum else kbRowsAlpha

    val accent    = Color(0xFF1A73E8)
    val keyBg     = if (isDark) Color(0xFF3C3C3C) else Color.White
    val specialBg = if (isDark) Color(0xFF252525) else Color(0xFFB8BEC8)
    val boardBg   = if (isDark) Color(0xFF1C1C1C) else Color(0xFFCDD0D8)
    val txtColor  = if (isDark) Color.White       else Color(0xFF111111)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(boardBg)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // ── Preview / suggestion bar ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(if (isDark) Color(0xFF282828) else Color(0xFFF0F2F5)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentText.ifEmpty { "Type something…" },
                color = if (currentText.isEmpty()) txtColor.copy(0.3f) else accent,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            // Domain shortcuts
            listOf(".com", ".org", ".net").forEach { sug ->
                Box(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.copy(0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTextChange(currentText + sug) }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) { Text(sug, fontSize = 9.sp, color = accent) }
            }
            // Paste from clipboard could go here in production (requires ClipboardManager)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardHide, null, tint = txtColor.copy(0.5f), modifier = Modifier.size(13.dp))
            }
        }

        // ── Key rows ─────────────────────────────────────────────────
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    val wt = when (key) {
                        "space"      -> 4f
                        "⇧","⌫"    -> 1.5f
                        "123","ABC"  -> 1.5f
                        "↵","com"    -> 1.6f
                        else         -> 1f
                    }
                    val bg = when (key) {
                        "↵"                                 -> accent
                        "⇧"                                 -> if (isUppercase) accent.copy(0.35f) else specialBg
                        in listOf("⌫","123","ABC","space")  -> specialBg
                        else                                -> keyBg
                    }
                    val label = when {
                        key == "space"                                    -> "space"
                        !showNumeric && isUppercase && key.length == 1
                                && key[0].isLetter()                      -> key.uppercase()
                        else                                              -> key
                    }

                    Box(
                        modifier = Modifier
                            .weight(wt)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                when (key) {
                                    "⇧"    -> isUppercase = !isUppercase
                                    "⌫"    -> if (currentText.isNotEmpty()) onTextChange(currentText.dropLast(1))
                                    "space" -> onTextChange("$currentText ")
                                    "↵"    -> { onSubmit(); onDismiss() }
                                    "123"  -> { showNumeric = true;  isUppercase = false }
                                    "ABC"  -> showNumeric = false
                                    "com"  -> onTextChange("$currentText.com")
                                    else   -> {
                                        val ch = if (!showNumeric && isUppercase) key.uppercase() else key
                                        onTextChange(currentText + ch)
                                        if (isUppercase && !showNumeric) isUppercase = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color      = if (key == "↵") Color.White else txtColor,
                            fontSize   = if (key in listOf("space","com")) 8.sp else 10.sp,
                            fontWeight = if (key == "↵") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
