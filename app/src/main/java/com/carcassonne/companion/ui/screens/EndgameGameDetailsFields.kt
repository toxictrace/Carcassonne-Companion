package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcGreenDeep
import com.carcassonne.companion.ui.theme.CarcText
import com.carcassonne.companion.ui.theme.CarcText3

@Composable
fun EndgameGameDetailsFields(
    gameName: String,
    onGameNameChange: (String) -> Unit,
    gameNotes: String,
    onGameNotesChange: (String) -> Unit
) {
    OutlinedTextField(
        value = gameName, onValueChange = onGameNameChange,
        label = { Text(stringResource(R.string.game_name_hint), color = CarcText3) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
            focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
        )
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = gameNotes,
        onValueChange = onGameNotesChange,
        label = { Text(stringResource(R.string.notes_label), color = CarcText3) },
        placeholder = { Text(stringResource(R.string.notes_placeholder), color = CarcText3, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 4,
        supportingText = { Text("${gameNotes.length}/150", color = CarcText3, fontSize = 11.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
            focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
        )
    )
}
