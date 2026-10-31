package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcCard2
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcText

data class CompareSection(val label: String, val enabled: Boolean)

@Composable
fun CompareSectionPickerDialog(
    sections: SnapshotStateList<CompareSection>,
    onSectionsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    fun updateSection(index: Int, enabled: Boolean) {
        sections[index] = sections[index].copy(enabled = enabled)
        val newMask = sections.foldIndexed(0) { idx, acc, section ->
            if (section.enabled) acc or (1 shl idx) else acc
        }
        onSectionsChange(newMask)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        title = { Text(stringResource(R.string.show_comparisons), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                sections.forEachIndexed { index, section ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { updateSection(index, !section.enabled) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = section.enabled,
                            onCheckedChange = { enabled -> updateSection(index, enabled) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CarcGreen,
                                uncheckedColor = CarcBorder
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(section.label, fontSize = 14.sp, color = CarcText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}
