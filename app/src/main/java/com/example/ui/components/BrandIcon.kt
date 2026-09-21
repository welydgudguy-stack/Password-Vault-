package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandFacebook
import com.example.ui.theme.BrandGithub
import com.example.ui.theme.BrandGoogle
import com.example.ui.theme.BrandInstagram
import com.example.ui.theme.BrandNetflix
import com.example.ui.theme.BrandTwitter
import com.example.ui.theme.BrandYoutube

@Composable
fun BrandIcon(
    title: String,
    website: String = "",
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val key = (title + " " + website).lowercase()

    val (bgColor, content) = when {
        key.contains("google") || key.contains("gmail") -> {
            Pair(BrandGoogle, BrandIconType.Text("G"))
        }
        key.contains("facebook") -> {
            Pair(BrandFacebook, BrandIconType.Text("f"))
        }
        key.contains("instagram") -> {
            Pair(BrandInstagram, BrandIconType.Icon(Icons.Default.Share))
        }
        key.contains("twitter") || key.contains("x.com") || key.contains(" x") -> {
            Pair(BrandTwitter, BrandIconType.Text("𝕏"))
        }
        key.contains("youtube") -> {
            Pair(BrandYoutube, BrandIconType.Icon(Icons.Default.PlayArrow))
        }
        key.contains("github") -> {
            Pair(BrandGithub, BrandIconType.Icon(Icons.Default.Code))
        }
        key.contains("netflix") -> {
            Pair(BrandNetflix, BrandIconType.Text("N"))
        }
        key.contains("mail") || key.contains("outlook") || key.contains("yahoo") -> {
            Pair(Color(0xFF0078D4), BrandIconType.Icon(Icons.Default.Email))
        }
        key.contains("movie") || key.contains("disney") || key.contains("hulu") || key.contains("spotify") -> {
            Pair(Color(0xFF1DB954), BrandIconType.Icon(Icons.Default.Movie))
        }
        else -> {
            val initial = title.firstOrNull()?.uppercase() ?: "K"
            val defaultColor = Color(0xFF4F46E5)
            Pair(defaultColor, BrandIconType.Text(initial))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        when (content) {
            is BrandIconType.Text -> {
                Text(
                    text = content.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45f).sp
                )
            }
            is BrandIconType.Icon -> {
                Icon(
                    imageVector = content.imageVector,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}

private sealed class BrandIconType {
    data class Text(val text: String) : BrandIconType()
    data class Icon(val imageVector: androidx.compose.ui.graphics.vector.ImageVector) : BrandIconType()
}
