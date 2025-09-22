package com.engfred.musicplayer.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engfred.musicplayer.R
import com.engfred.musicplayer.core.ui.components.SoundWaveLoading

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun SplashScreen() {
    val splashBackgroundColor = Color(0xFF2B2B5F)

    val activity = LocalActivity.current

    // Lock orientation when entering splash
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // Reset orientation when leaving splash
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackgroundColor)
            .systemBarsPadding()
    ) {
        // Centered logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(274.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(100.dp)),
            contentScale = ContentScale.Crop
        )

        // Bottom section: loader + text
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoundWaveLoading(
                barMaxHeight = 25.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Professional copyright line with symbol
            Text(
                text = "© 2025 Engineer Fred",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}
