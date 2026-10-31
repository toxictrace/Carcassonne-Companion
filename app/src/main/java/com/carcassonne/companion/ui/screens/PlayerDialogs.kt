package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.*
import com.carcassonne.companion.ui.theme.*

// ─── Avatar Picker Section ────────────────────────────────────────────────────
@Composable
fun AvatarPickerSection(
    name: String,
    color: String,
    avatarPath: String?,
    onPickGallery: () -> Unit,
    onPickCamera: () -> Unit,
    onRemoveAvatar: () -> Unit,
    size: Dp = 88.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            PlayerAvatar(name.ifBlank { "?" }, color, size = size, avatarPath = avatarPath)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CarcGreen)
                    .border(2.dp, CarcBg, CircleShape)
                    .clickable { onPickGallery() },
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPickGallery) {
                Text(stringResource(R.string.gallery_label), fontSize = 12.sp, color = CarcGreen)
            }
            TextButton(onClick = onPickCamera) {
                Text(stringResource(R.string.camera_label), fontSize = 12.sp, color = CarcGreen)
            }
            if (avatarPath != null) {
                TextButton(onClick = onRemoveAvatar) {
                    Text(stringResource(R.string.remove_btn), fontSize = 12.sp, color = CarcRed)
                }
            }
        }
    }
}

// ─── Permission-aware launcher helpers ───────────────────────────────────────
@Composable
fun rememberGalleryLauncher(
    onResult: (android.net.Uri?) -> Unit
): Pair<() -> Unit, androidx.activity.result.ActivityResultLauncher<String>> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(), onResult
    )
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launcher.launch("image/*") }

    val open: () -> Unit = {
        val perm = if (android.os.Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_IMAGES
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        val state = androidx.core.content.ContextCompat.checkSelfPermission(context, perm)
        if (state == android.content.pm.PackageManager.PERMISSION_GRANTED)
            launcher.launch("image/*")
        else
            permLauncher.launch(perm)
    }
    return Pair(open, launcher)
}

@Composable
fun rememberCameraLauncher(
    onImageSaved: (java.io.File) -> Unit
): Pair<() -> Unit, () -> Unit> {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tempFile by remember { mutableStateOf<java.io.File?>(null) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success -> if (success) tempFile?.let { onImageSaved(it) } }

    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = com.carcassonne.companion.util.ImageUtils.createTempImageFile(context)
            tempFile = file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            cameraLauncher.launch(uri)
        }
    }

    val open: () -> Unit = {
        val perm = android.Manifest.permission.CAMERA
        val state = androidx.core.content.ContextCompat.checkSelfPermission(context, perm)
        if (state == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val file = com.carcassonne.companion.util.ImageUtils.createTempImageFile(context)
            tempFile = file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            cameraLauncher.launch(uri)
        } else {
            permLauncher.launch(perm)
        }
    }
    return Pair(open, { tempFile = null })
}

// ─── Add Player Dialog ────────────────────────────────────────────────────────
@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("red") }
    var avatarPath by remember { mutableStateOf<String?>(null) }

    val (openGallery, _) = rememberGalleryLauncher { uri ->
        uri?.let {
            val saved = com.carcassonne.companion.util.ImageUtils.saveImageFromUri(context, it, 0)
            if (saved != null) {
                com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
                avatarPath = saved
            }
        }
    }
    val (openCamera, _) = rememberCameraLauncher { file ->
        com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
        avatarPath = file.absolutePath
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        title = { Text(stringResource(R.string.add_new_player), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarPickerSection(
                    name = name, color = color, avatarPath = avatarPath,
                    onPickGallery = openGallery,
                    onPickCamera = openCamera,
                    onRemoveAvatar = {
                        com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
                        avatarPath = null
                    }
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.player_name_hint), color = CarcText3) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                        focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.preferred_color), fontSize = 13.sp, color = CarcText2)
                Spacer(Modifier.height(8.dp))
                MeepleColorPicker(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) { onAdd(name.trim(), color, avatarPath); onDismiss() } },
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen, contentColor = CarcBg)
            ) { Text(stringResource(R.string.create_player), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CarcText3) }
        }
    )
}