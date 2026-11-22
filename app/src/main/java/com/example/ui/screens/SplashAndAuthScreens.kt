package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.GoogleSignInButton
import com.example.data.firebase.attemptAutoSignIn
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapGold
import com.example.ui.theme.SnapYellow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.72f) }
    val alpha = remember { Animatable(0f) }
    val pulse = rememberInfiniteTransition(label = "splash_pulse")
    val ringScale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ring"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(450))
        delay(950)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_splash_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.42f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianBlack.copy(alpha = 0.45f), ObsidianBlack.copy(alpha = 0.85f))
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(136.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(SnapYellow.copy(alpha = 0.16f))
                    .border(2.dp, SnapYellow.copy(alpha = 0.65f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nilesh_logo),
                    contentDescription = "NILESH SNAPCHAT logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "NILESH SNAPCHAT",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SnapYellow
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = CharcoalElevated.copy(alpha = 0.85f),
                modifier = Modifier.alpha(alpha.value)
            ) {
                Text(
                    text = "CAMERA • 1000+ FILTERS • PN POINTS",
                    style = MaterialTheme.typography.labelMedium.copy(color = PureWhite),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val cm = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = cm,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_auth_banner),
                    contentDescription = "NILESH SNAPCHAT social banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.88f))
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nilesh_logo),
                        contentDescription = "App icon",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Column {
                        Text(
                            text = "NILESH SNAPCHAT",
                            style = MaterialTheme.typography.headlineMedium.copy(color = SnapYellow)
                        )
                        Text(
                            text = "Snap, Chat, Earn +1000 PN Points",
                            style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.85f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Your Social Camera Studio",
                style = MaterialTheme.typography.headlineMedium.copy(color = PureWhite),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sign in securely with Google to sync your PN Points, 24h Stories, Notes, Chats, and Private Profile.",
                style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.72f)),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AuthFeatureRow(
                    icon = Icons.Filled.CameraAlt,
                    tint = SnapYellow,
                    title = "1,076+ Live Camera Filters",
                    subtitle = "DSLR, iPhone HDR, Golden Hour, Anime, Dog/Cat & Indian Festivals"
                )
                AuthFeatureRow(
                    icon = Icons.Filled.Bolt,
                    tint = SnapGold,
                    title = "+1,000 PN Points Per Snap",
                    subtitle = "Real-time animated wallet, history ledger & leaderboard"
                )
                AuthFeatureRow(
                    icon = Icons.Filled.AutoAwesome,
                    tint = NeonCyan,
                    title = "560+ AI Simulated Indian Friends",
                    subtitle = "Smart Hinglish chats, auto snaps & 24-hour interactive stories"
                )
                AuthFeatureRow(
                    icon = Icons.Filled.Lock,
                    tint = SnapCoral,
                    title = "MY SAVED SNAPS Private Vault",
                    subtitle = "Encrypted sandboxed gallery & permission-based Snap Map"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!errorMessage.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SnapCoral.copy(alpha = 0.16f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            GoogleSignInButton(
                onAuthSuccess = onAuthSuccess,
                onAuthError = { msg -> errorMessage = msg }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "AI simulated profiles are clearly labeled 'AI Sim' inside the app.",
                style = MaterialTheme.typography.labelSmall.copy(color = PureWhite.copy(alpha = 0.55f)),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f))
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(color = PureWhite))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.68f)))
            }
        }
    }
}
