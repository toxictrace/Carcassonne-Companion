package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

// ─── Restore From Folder Dialog ───────────────────────────────────────────────
  @Composable
  fun RestoreFromFolderDialog(
      files: List<DocumentFile>,
      onSelect: (DocumentFile) -> Unit,
      onDismiss: () -> Unit
  ) {
      var confirmFile by remember { mutableStateOf<DocumentFile?>(null) }
      if (confirmFile != null) {
          AlertDialog(
              onDismissRequest = { confirmFile = null },
              title = { Text(stringResource(R.string.restore_confirm_title)) },
              text = { Text(stringResource(R.string.restore_confirm_msg, confirmFile!!.name ?: ""), color = CarcText2) },
              confirmButton = {
                  TextButton(onClick = { onSelect(confirmFile!!); confirmFile = null; onDismiss() }) {
                      Text(stringResource(R.string.restore_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                  }
              },
              dismissButton = { TextButton(onClick = { confirmFile = null }) { Text(stringResource(R.string.cancel)) } },
              containerColor = CarcCard2
          )
      } else {
          AlertDialog(
              onDismissRequest = onDismiss,
              title = { Text(stringResource(R.string.restore_pick_title)) },
              text = {
                  Column {
                      if (files.isEmpty()) {
                          Text(stringResource(R.string.restore_no_files_in_folder), color = CarcText2, fontSize = 14.sp)
                      } else {
                          Text(stringResource(R.string.backup_folder_files_hint), color = CarcText3, fontSize = 11.sp,
                              modifier = Modifier.padding(bottom = 12.dp))
                          files.forEach { doc ->
                              val modDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(java.util.Date(doc.lastModified()))
                              val displayName = doc.name?.substringBeforeLast(".") ?: ""
                              Row(
                                  modifier = Modifier.fillMaxWidth().clickable { confirmFile = doc }.padding(vertical = 10.dp, horizontal = 4.dp),
                                  verticalAlignment = Alignment.CenterVertically
                              ) {
                                  Text("💾", fontSize = 22.sp)
                                  Spacer(Modifier.width(12.dp))
                                  Column(Modifier.weight(1f)) {
                                      Text(displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                                      Text(modDate, fontSize = 11.sp, color = CarcText3)
                                  }
                              }
                              HorizontalDivider(color = CarcText3.copy(alpha = 0.15f))
                          }
                      }
                  }
              },
              confirmButton = {},
              dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
              containerColor = CarcCard2
          )
      }
  }
