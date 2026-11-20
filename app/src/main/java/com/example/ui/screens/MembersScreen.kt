package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompanyMemberDoc
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.ui.OvertimeUiState
import com.example.ui.components.CompanyQrCodeDialog
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.QrCodeScannerDialog
import com.example.ui.components.TopRightSettingsButton
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.EmeraldTealDark
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun MembersScreen(
    state: OvertimeUiState,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onScanQrToJoin: (String) -> Unit,
    onCopyCompanyCode: () -> Unit,
    onInviteViaWhatsApp: () -> Unit,
    onPromoteToAdmin: (String) -> Unit,
    onDemoteToMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val strings = state.strings
    val profile = state.profile
    val context = LocalContext.current

    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    val adminUidSet = remember(profile.connectedAdminUid, profile.connectedAdminUids) {
        (listOf(profile.connectedAdminUid) + profile.connectedAdminUids)
            .filter { it.isNotBlank() }
            .toSet()
    }

    val adminMembers = remember(state.companyMembers, adminUidSet, profile.connectedAdminUid) {
        state.companyMembers
            .filter { it.userId in adminUidSet || it.role == UserRole.ADMIN.key }
            .sortedWith(
                compareByDescending<CompanyMemberDoc> { it.userId == profile.connectedAdminUid }
                    .thenBy { it.employeeName.lowercase() }
            )
    }

    val regularMembers = remember(state.companyMembers, adminUidSet) {
        state.companyMembers
            .filter { it.userId !in adminUidSet && it.role != UserRole.ADMIN.key }
            .sortedBy { it.employeeName.lowercase() }
    }

    val canPromoteMoreAdmins = adminMembers.size < UserProfile.MAX_ADMINS

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Top-Right Settings Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.membersTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.membersSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConnectionStatusBadge(isOnline = state.isOnline, strings = strings)
                        TopRightSettingsButton(
                            contentDescription = strings.navSettings,
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            if (!profile.isConnectedToCompany) {
                // Not connected to any company yet
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("members_not_connected_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AmberAccent.copy(alpha = 0.14f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.LinkOff,
                                        contentDescription = null,
                                        tint = AmberAccent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.membersNotConnectedTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.membersNotConnectedBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showQrScanner = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("members_scan_qr_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.scanQrButton, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("members_open_settings_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, EmeraldTeal)
                                ) {
                                    Text(strings.openProfileConnectButton, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Hero Company Team Summary Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("members_summary_hero_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(EmeraldTealDark, EmeraldTeal)
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.companyName.ifBlank { strings.modeGroupLabel },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${strings.companyCodeLabel}: ${profile.companyCode}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFEF3C7)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.18f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }

                                // Dual Count Pills: Admin (X/8) & Anggota (Unlimited)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = strings.roleAdmin,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${adminMembers.size} / ${UserProfile.MAX_ADMINS}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = strings.adminQuotaHint,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFEF3C7),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = strings.navMembers,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${regularMembers.size}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AllInclusive,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFEF3C7),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = strings.membersUnlimitedBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFEF3C7),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Action Buttons (QR Code, Copy Code, Invite WA)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { showQrCodeDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("members_show_qr_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.QrCode2,
                                                contentDescription = strings.showQrButton,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = strings.showQrButton,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(
                                                ClipData.newPlainText("Company Code", profile.companyCode)
                                            )
                                            onCopyCompanyCode()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("members_copy_code_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = strings.copyCodeButton,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = strings.copyCodeButton,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onInviteViaWhatsApp,
                                        shape = RoundedCornerShape(12.dp),
                                        color = AmberAccent,
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(44.dp)
                                            .testTag("members_invite_whatsapp_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Share,
                                                contentDescription = strings.inviteViaWhatsAppButton,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = strings.inviteViaWhatsAppButton,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 1: ADMINS (4-8 / max 8)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${strings.adminSectionTitle} (${adminMembers.size}/${UserProfile.MAX_ADMINS})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = strings.adminQuotaHint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent
                            )
                        }
                    }
                }

                items(adminMembers, key = { "admin_${it.userId}" }) { admin ->
                    val isPrimary = admin.userId == profile.connectedAdminUid
                    val isSelf = admin.userId == state.currentUserId
                    MemberRosterCard(
                        member = admin,
                        isAdmin = true,
                        isPrimaryAdmin = isPrimary,
                        isSelf = isSelf,
                        badgeLabel = if (isPrimary) strings.primaryAdminBadge else strings.coAdminBadge,
                        youLabel = strings.youBadge,
                        actionLabel = if (state.isAdminGroupMode && !isPrimary) strings.demoteToMemberButton else null,
                        actionIsPromote = false,
                        actionEnabled = !state.isBusy,
                        onActionClick = { onDemoteToMember(admin.userId) }
                    )
                }

                // SECTION 2: REGULAR MEMBERS (UNLIMITED)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Badge,
                                contentDescription = null,
                                tint = EmeraldTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${strings.membersSectionTitle} (${regularMembers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldTeal.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AllInclusive,
                                    contentDescription = null,
                                    tint = EmeraldTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = strings.membersUnlimitedBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldTeal
                                )
                            }
                        }
                    }
                }

                if (regularMembers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = strings.noMembersYetHint,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(regularMembers, key = { "member_${it.userId}" }) { member ->
                        val isSelf = member.userId == state.currentUserId
                        MemberRosterCard(
                            member = member,
                            isAdmin = false,
                            isPrimaryAdmin = false,
                            isSelf = isSelf,
                            badgeLabel = strings.roleEmployee,
                            youLabel = strings.youBadge,
                            actionLabel = if (state.isAdminGroupMode) strings.promoteToAdminButton else null,
                            actionIsPromote = true,
                            actionEnabled = !state.isBusy && canPromoteMoreAdmins,
                            onActionClick = { onPromoteToAdmin(member.userId) }
                        )
                    }
                }
            }
        }
    }

    if (showQrCodeDialog && profile.isConnectedToCompany) {
        CompanyQrCodeDialog(
            companyName = profile.companyName.ifBlank { profile.companyCode },
            companyCode = profile.companyCode,
            strings = strings,
            onCopyCode = onCopyCompanyCode,
            onDismiss = { showQrCodeDialog = false }
        )
    }

    if (showQrScanner) {
        QrCodeScannerDialog(
            strings = strings,
            onCodeScanned = { scannedCode ->
                showQrScanner = false
                onScanQrToJoin(scannedCode)
            },
            onDismiss = { showQrScanner = false }
        )
    }
}

@Composable
private fun MemberRosterCard(
    member: CompanyMemberDoc,
    isAdmin: Boolean,
    isPrimaryAdmin: Boolean,
    isSelf: Boolean,
    badgeLabel: String,
    youLabel: String,
    actionLabel: String?,
    actionIsPromote: Boolean,
    actionEnabled: Boolean,
    onActionClick: () -> Unit
) {
    val accent = if (isAdmin) AmberAccent else EmeraldTeal
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.userId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (isPrimaryAdmin) 1.5.dp else 1.dp,
            color = if (isPrimaryAdmin) AmberAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPrimaryAdmin) Icons.Rounded.VerifiedUser
                    else if (isAdmin) Icons.Rounded.AdminPanelSettings
                    else Icons.Rounded.Person,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.employeeName.ifBlank { "Anggota" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelf) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = youLabel,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badgeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }

            if (actionLabel != null) {
                FilledTonalButton(
                    onClick = onActionClick,
                    enabled = actionEnabled,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (actionIsPromote) AmberAccent.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (actionIsPromote) AmberAccent
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("member_role_action_${member.userId}")
                ) {
                    Icon(
                        imageVector = if (actionIsPromote) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
