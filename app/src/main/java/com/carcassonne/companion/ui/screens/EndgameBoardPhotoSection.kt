package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcText3

@Composable
fun EndgameBoardPhotoSection(
    photoPath: String?,
    onPhotoSaved: (String?) -> Unit
) {
    Text(
        stringResource(R.string.board_photo_btn),
        fontSize = 13.sp,
        color = CarcText3,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    GamePhotoBox(
        photoPath = photoPath,
        editable = true,
        onPhotoSaved = { path -> onPhotoSaved(if (path.isEmpty()) null else path) }
    )
    Spacer(Modifier.height(12.dp))
}
