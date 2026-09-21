package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusWarning

@Composable
fun SettingsScreen(
    themeMode: String,
    isBiometricEnabled: Boolean,
    autoLockMinutes: Int,
    onThemeChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onAutoLockChange: (Int) -> Unit,
    onLockNow: () -> Unit,
    onExportEncryptedBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onImportBackup: (String) -> Unit,
    onOpenAutofillSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Title
            Text(
                text = "Settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Security
            Text(
                text = "Security",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Unlock",
                        subtitle = if (isBiometricEnabled) "Fingerprint & Face enabled" else "Disabled",
                        trailing = {
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = onBiometricToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandPrimary
                                ),
                                modifier = Modifier.testTag("toggle_biometrics_setting")
                            )
                        }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "Auto-lock Timeout",
                        subtitle = when (autoLockMinutes) {
                            0 -> "Immediately on background"
                            1 -> "1 minute"
                            5 -> "5 minutes"
                            else -> "$autoLockMinutes minutes"
                        },
                        onClick = { showAutoLockDialog = true }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "Lock Vault Now",
                        subtitle = "Immediately lock with master password",
                        onClick = onLockNow
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Data & Backup
            Text(
                text = "Backup & Export",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Save,
                        title = "Backup Vault",
                        subtitle = "Encrypted JSON or unencrypted CSV",
                        onClick = { showBackupDialog = true }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.Sync,
                        title = "Restore Backup",
                        subtitle = "Import credentials from JSON backup",
                        onClick = { showImportDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Appearance & System
            Text(
                text = "Preferences",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        title = "Appearance",
                        subtitle = when (themeMode) {
                            "dark" -> "Dark mode (Default)"
                            "light" -> "Light mode"
                            else -> "Follow system"
                        },
                        onClick = { showThemeDialog = true }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.SettingsSuggest,
                        title = "Autofill Settings",
                        subtitle = "Android system autofill provider",
                        onClick = onOpenAutofillSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: About
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About VaultKeep",
                    subtitle = "Version 1.0.0 • 100% Offline & Free",
                    onClick = { showAboutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        // Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Choose Appearance") },
                text = {
                    Column {
                        ThemeOptionItem(
                            label = "Dark Mode (Recommended)",
                            isSelected = themeMode == "dark",
                            onClick = {
                                onThemeChange("dark")
                                showThemeDialog = false
                            }
                        )
                        ThemeOptionItem(
                            label = "Light Mode",
                            isSelected = themeMode == "light",
                            onClick = {
                                onThemeChange("light")
                                showThemeDialog = false
                            }
                        )
                        ThemeOptionItem(
                            label = "Follow System",
                            isSelected = themeMode == "system",
                            onClick = {
                                onThemeChange("system")
                                showThemeDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Auto Lock Dialog
        if (showAutoLockDialog) {
            AlertDialog(
                onDismissRequest = { showAutoLockDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Auto-lock Timeout") },
                text = {
                    Column {
                        listOf(0 to "Immediately", 1 to "1 minute", 5 to "5 minutes", 15 to "15 minutes").forEach { (mins, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAutoLockChange(mins)
                                        showAutoLockDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = autoLockMinutes == mins,
                                    onClick = {
                                        onAutoLockChange(mins)
                                        showAutoLockDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(label, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAutoLockDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Backup Dialog
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Export Vault") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose how you want to export your vault data:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                showBackupDialog = false
                                onExportEncryptedBackup()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Encrypted JSON Backup (Safe)")
                        }

                        OutlinedButton(
                            onClick = {
                                showBackupDialog = false
                                onExportCsv()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Plaintext CSV (Warning: Unencrypted)")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Restore from JSON Backup") },
                text = {
                    Column {
                        Text(
                            text = "Paste the exported JSON backup text below to restore logins into your vault:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            placeholder = { Text("{\"version\":1, \"items\": [...]}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            maxLines = 6
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                onImportBackup(importText)
                                showImportDialog = false
                                importText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // About & Zero-Cost Architecture Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("About VaultKeep", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "VaultKeep is a 100% offline, privacy-first Android password manager.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zero API Keys Required:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                        Text(
                            text = "• Local AES-256-GCM encryption & PBKDF2 key derivation run directly on your phone hardware.\n" +
                                    "• Android BiometricPrompt hardware abstraction handles fingerprint and face recognition.\n" +
                                    "• Native Android AutofillService framework handles filling without cloud servers.\n" +
                                    "• 100% compliant with Google Play Store policies.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}

@Composable
private fun ThemeOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}
