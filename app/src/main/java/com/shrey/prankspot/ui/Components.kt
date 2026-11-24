package com.shrey.prankspot.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

fun qrBitmap(text: String): ImageBitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"))
    val bmp = createBitmap(m.width, m.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until m.width) for (y in 0 until m.height) {
        bmp[x, y] = if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return bmp.asImageBitmap()
}

@Composable
fun QrCode(text: String, size: Dp = 200.dp, modifier: Modifier = Modifier) {
    val bmp = remember(text) { qrBitmap(text) }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        Image(bmp, contentDescription = "QR code", modifier = Modifier.size(size), filterQuality = FilterQuality.None)
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
fun InfoNote(text: String, emoji: String = "💡", modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(emoji)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/** Renders the exact page guests will see, straight from memory (no server needed). */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrankWebView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mediaPlaybackRequiresUserGesture = true
            }
        },
        update = { it.loadDataWithBaseURL("http://prankspot.local/", html, "text/html", "utf-8", null) },
    )
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun durationLabel(minutes: Int) = when {
    minutes == 0 -> "Unlimited"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/** A rough device type from the browser's user agent, shown in memory only, never stored. */
fun deviceKind(userAgent: String): String = when {
    "iPhone" in userAgent -> "iPhone"
    "iPad" in userAgent -> "iPad"
    "Android" in userAgent -> "Android device"
    "Windows" in userAgent -> "Windows PC"
    "Macintosh" in userAgent -> "Mac"
    "Linux" in userAgent -> "Linux device"
    else -> "Unknown device"
}

private val nickAdj = listOf("Sneaky", "Curious", "Sleepy", "Turbo", "Wobbly", "Giggly", "Mystery", "Fluffy", "Brave", "Snacky")
private val nickNoun = listOf("Penguin", "Llama", "Pickle", "Waffle", "Goblin", "Panda", "Yeti", "Noodle", "Walrus", "Taco")

/** A fun local nickname derived from the address; it says nothing about who the guest really is. */
fun nickname(ip: String): String {
    val h = ip.hashCode().let { if (it < 0) -it else it }
    return nickAdj[h % nickAdj.size] + " " + nickNoun[(h / nickAdj.size) % nickNoun.size]
}
