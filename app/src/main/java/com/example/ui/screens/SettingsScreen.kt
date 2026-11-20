package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.AppMode
import com.example.data.AppThemeMode
import com.example.data.UserRole
import com.example.ui.OvertimeUiState
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.StatusRejected

private enum class ActiveSettingsModal {
    NONE,
    PROFILE,
    BROWSER_LINK,
    THEME_LANGUAGE,
    TUTORIAL,
    ABOUT
}

@Composable
fun SettingsScreen(
    state: OvertimeUiState,
    onNavigateBack: () -> Unit,
    onSaveProfileAndCompany: (
        displayName: String,
        mode: AppMode,
        role: UserRole,
        companyName: String,
        companyCode: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onShareBrowserLinkWhatsApp: () -> Unit,
    onBrowserLinkCopied: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val strings = state.strings
    val localPrefs = state.localPrefs
    val profile = state.profile
    var activeModal by remember { mutableStateOf(ActiveSettingsModal.NONE) }

    val profileSubtitle = when {
        profile.mode == AppMode.PERSONAL -> "${profile.name} • ${strings.modePersonalBadge}"
        profile.isConnectedToCompany -> {
            val roleText = if (profile.role == UserRole.ADMIN) strings.roleAdmin else strings.roleEmployee
            "${profile.companyName.ifBlank { profile.companyCode }} • $roleText (${profile.companyCode})"
        }
        else -> {
            val roleText = if (profile.role == UserRole.ADMIN) strings.roleAdmin else strings.roleEmployee
            "${profile.name} • $roleText (${strings.companyNotConnectedBannerTitle})"
        }
    }

    val themeSubtitle = buildString {
        append(if (localPrefs.themeMode == AppThemeMode.DARK) strings.themeDarkLabel else strings.themeLightLabel)
        append(" • ")
        append(if (localPrefs.language == AppLanguage.INDONESIAN) strings.langIndonesianLabel else strings.langEnglishLabel)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Back Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onNavigateBack,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = strings.backButtonDesc,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.settingsSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ConnectionStatusBadge(isOnline = state.isOnline, strings = strings)
                }
            }

            // Active Google Account Card with Sign Out
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldTeal.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = strings.authAccountLabel,
                                    tint = EmeraldTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.authAccountLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (profile.email.isNotBlank()) {
                                    Text(
                                        text = profile.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_sign_out_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StatusRejected.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.authSignOutButton,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 1. Profil & Koneksi Perusahaan
            item {
                SettingsClickableCard(
                    icon = Icons.Rounded.Person,
                    iconTint = EmeraldTeal,
                    title = strings.cardProfileTitle,
                    subtitle = profileSubtitle,
                    onClick = { activeModal = ActiveSettingsModal.PROFILE },
                    modifier = Modifier.testTag("settings_card_profile")
                )
            }

            // 2. Link Browser (Web / iOS) + Share ke WhatsApp
            item {
                SettingsClickableCard(
                    icon = Icons.Rounded.Language,
                    iconTint = Color(0xFF16A34A),
                    title = strings.cardBrowserLinkTitle,
                    subtitle = strings.cardBrowserLinkSubtitle,
                    onClick = { activeModal = ActiveSettingsModal.BROWSER_LINK },
                    modifier = Modifier.testTag("settings_card_browser_link")
                )
            }

            // 2. Tema & Bahasa
            item {
                SettingsClickableCard(
                    icon = Icons.Rounded.Palette,
                    iconTint = AmberAccent,
                    title = strings.cardThemeLangTitle,
                    subtitle = themeSubtitle,
                    onClick = { activeModal = ActiveSettingsModal.THEME_LANGUAGE },
                    modifier = Modifier.testTag("settings_card_theme_language")
                )
            }

            // 3. Tutorial Penggunaan
            item {
                SettingsClickableCard(
                    icon = Icons.Rounded.MenuBook,
                    iconTint = Color(0xFF0EA5E9),
                    title = strings.cardTutorialTitle,
                    subtitle = strings.cardTutorialSubtitle,
                    onClick = { activeModal = ActiveSettingsModal.TUTORIAL },
                    modifier = Modifier.testTag("settings_card_tutorial")
                )
            }

            // 4. Tentang Aplikasi
            item {
                SettingsClickableCard(
                    icon = Icons.Rounded.Info,
                    iconTint = Color(0xFF8B5CF6),
                    title = strings.cardAboutTitle,
                    subtitle = strings.cardAboutSubtitle,
                    onClick = { activeModal = ActiveSettingsModal.ABOUT },
                    modifier = Modifier.testTag("settings_card_about")
                )
            }
        }
    }

    when (activeModal) {
        ActiveSettingsModal.PROFILE -> {
            ProfileSettingsModal(
                currentProfile = profile,
                strings = strings,
                isBusy = state.isBusy,
                onDismiss = { if (!state.isBusy) activeModal = ActiveSettingsModal.NONE },
                onSave = { displayName, mode, role, companyName, companyCode ->
                    onSaveProfileAndCompany(displayName, mode, role, companyName, companyCode) { success ->
                        if (success) {
                            activeModal = ActiveSettingsModal.NONE
                        }
                    }
                }
            )
        }
        ActiveSettingsModal.BROWSER_LINK -> {
            BrowserLinkModal(
                profile = profile,
                strings = strings,
                onShareWhatsApp = onShareBrowserLinkWhatsApp,
                onLinkCopied = onBrowserLinkCopied,
                onDismiss = { activeModal = ActiveSettingsModal.NONE }
            )
        }
        ActiveSettingsModal.THEME_LANGUAGE -> {
            ThemeLanguageModal(
                currentTheme = localPrefs.themeMode,
                currentLanguage = localPrefs.language,
                strings = strings,
                onSelectTheme = onSelectTheme,
                onSelectLanguage = onSelectLanguage,
                onDismiss = { activeModal = ActiveSettingsModal.NONE }
            )
        }
        ActiveSettingsModal.TUTORIAL -> {
            TutorialModal(
                strings = strings,
                onDismiss = { activeModal = ActiveSettingsModal.NONE }
            )
        }
        ActiveSettingsModal.ABOUT -> {
            AboutAppModal(
                strings = strings,
                onDismiss = { activeModal = ActiveSettingsModal.NONE }
            )
        }
        ActiveSettingsModal.NONE -> Unit
    }
}

@Composable
private fun SettingsClickableCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
