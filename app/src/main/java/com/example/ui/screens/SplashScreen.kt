package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.EmeraldTealDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AbsenLemburkuSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 580, easing = FastOutSlowInEasing)
            )
        }
        delay(1700L)
        onFinished()
    }

    // Blended Green + White soft luminous background
    val splashGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFFFFFFF),
            0.34f to Color(0xFFECFDF7),
            0.68f to Color(0xFF99E2D0),
            1.0f to Color(0xFF0D9488)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(splashGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen_root")
    ) {
        // Soft decorative mint-white radial aura behind center logo
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Centered Logo + "by Rafline Dev"
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(164.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(38.dp),
                        ambientColor = EmeraldTealDark.copy(alpha = 0.28f),
                        spotColor = EmeraldTealDark.copy(alpha = 0.28f)
                    ),
                shape = RoundedCornerShape(38.dp),
                color = Color(0xFFF5FBFB),
                border = BorderStroke(3.dp, Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_absen_lemburku_logo),
                        contentDescription = "Absen Lemburku",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, EmeraldTeal.copy(alpha = 0.25f)),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "by Rafline Dev",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    color = EmeraldTealDark
                )
            }
        }
    }
}
