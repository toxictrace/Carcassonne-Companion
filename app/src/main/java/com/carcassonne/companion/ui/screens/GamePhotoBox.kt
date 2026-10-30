package com.carcassonne.companion.ui.screens

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.util.ImageUtils

// ─── Game Photo Box ───────────────────────────────────────────────────────────
// Универсальный блок для показа/выбора фото партии.
// editable=true — показывает кнопки камера/галерея; editable=false — только просмотр.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GamePhotoBox(
    photoPath: String?,
    editable: Boolean = false,
    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,
    onPhotoSaved: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentPath by remember(photoPath) { mutableStateOf(photoPath) }
    var showSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, currentPath) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (currentPath != null && java.io.File(currentPath!!).exists())
                BitmapFactory.decodeFile(currentPath!!)?.asImageBitmap()
            else null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val path = ImageUtils.saveGamePhotoFromUri(context, uri, System.currentTimeMillis().toInt())
            if (path != null) { currentPath = path; onPhotoSaved(path) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempCameraUri ?: return@rememberLauncherForActivityResult
            val path = ImageUtils.saveGamePhotoFromUri(context, uri, System.currentTimeMillis().toInt())
            if (path != null) { currentPath = path; onPhotoSaved(path) }
        }
    }

    val launchCamera = {
        try {
            val file = ImageUtils.createTempGamePhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) { e.printStackTrace() }
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (bitmap != null) Modifier else Modifier.height(100.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(CarcBg3)
            .then(if (editable) Modifier.clickable { showSheet = true } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val safeBitmap = bitmap
        if (safeBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = safeBitmap,
                contentDescription = "Game photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            // Полупрозрачный оверлей снизу если editable
            if (editable) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.change_photo), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗺️", fontSize = 36.sp)
                if (editable) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.add_board_photo), fontSize = 12.sp, color = CarcText3)
                }
            }
        }
    }

    if (showSheet && editable) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = CarcCard2
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                Text(stringResource(R.string.board_photo_title), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Камера
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(CarcCard)
                            .border(1.dp, CarcBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                showSheet = false
                                val perm = Manifest.permission.CAMERA
                                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, perm
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (granted) launchCamera() else cameraPermission.launch(perm)
                            }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", fontSize = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.camera_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // Галерея
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(CarcCard)
                            .border(1.dp, CarcBorder, RoundedCornerShape(12.dp))
                            .clickable { showSheet = false; galleryLauncher.launch("image/*") }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🖼️", fontSize = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.gallery_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (currentPath != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { currentPath = null; onPhotoSaved(""); showSheet = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.remove_photo), fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

