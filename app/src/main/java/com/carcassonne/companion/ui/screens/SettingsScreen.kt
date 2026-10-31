package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.SettingsRow
import com.carcassonne.companion.ui.theme.*
import java.util.Locale

// ─── Settings Screen ─────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onBackup: () -> Unit,
    onRestore: () -> Unit = {},
    onPickBackupFolder: () -> Unit = {},
    backupFolderName: String? = null,
    onClearAll: () -> Unit,
    isDarkMode: Boolean = true,
    onDarkMode: (Boolean) -> Unit = {}
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_all_title)) },
            text = { Text(stringResource(R.string.clear_all_msg), color = CarcText2) },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text(stringResource(R.string.delete_all), color = CarcRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = CarcCard2
        )
    }

    if (showLanguageDialog) {
        val currentLang = remember {
            val loc = AppCompatDelegate.getApplicationLocales()
            if (!loc.isEmpty) loc[0]?.language ?: Locale.getDefault().language else Locale.getDefault().language
        }
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column {
                    listOf("en" to "English", "ru" to "Русский").forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(code)
                                    )
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = code == currentLang,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CarcGreen)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, color = CarcText)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = CarcCard2
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(stringResource(R.string.settings_appearance), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        item {
            SettingsRow("🌙", stringResource(R.string.dark_mode), stringResource(R.string.dark_mode_sub),
                onClick = { onDarkMode(!isDarkMode) },
                trailing = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CarcBg, checkedTrackColor = CarcGreen)
                    )
                }
            )
        }
        item {
            SettingsRow("🌐", stringResource(R.string.language),
            run {
                val loc = AppCompatDelegate.getApplicationLocales()
                val lang = if (!loc.isEmpty) loc[0]?.language ?: Locale.getDefault().language else Locale.getDefault().language
                if (lang == "ru") "Русский" else "English"
            },
            onClick = { showLanguageDialog = true })
        }
        item {
            Text(stringResource(R.string.settings_data), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        }
        item {
            SettingsRow(
                "📁", stringResource(R.string.backup_folder),
                backupFolderName ?: stringResource(R.string.backup_folder_default),
                onClick = onPickBackupFolder,
                trailing = { Text("›", fontSize = 20.sp, color = CarcText2) }
            )
        }
        item { SettingsRow("☁️", stringResource(R.string.backup_data), stringResource(R.string.backup_sub), onClick = onBackup) }
        item { SettingsRow("📥", stringResource(R.string.restore_data), stringResource(R.string.restore_sub), onClick = onRestore) }
        item {
            SettingsRow(
                "🗑️", stringResource(R.string.clear_all), stringResource(R.string.clear_all_sub),
                onClick = { showClearDialog = true },
                iconBgColor = CarcRed.copy(alpha = 0.1f),
                trailing = { Text("›", fontSize = 20.sp, color = CarcRed) }
            )
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

