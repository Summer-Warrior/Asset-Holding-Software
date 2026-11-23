package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.TechMono

@Composable
fun TextureStudioScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateTo3D: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showUvOverlay by remember { mutableStateOf(true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    viewModel.importCustomTexture(bitmap, "Imported Texture")
                }
            } catch (e: Exception) {
                // Handled in ViewModel
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "3D MODEL TEXTURE LAB",
            fontFamily = TechMono,
            fontSize = 12.sp,
            color = CyanNeon,
            letterSpacing = 1.sp
        )

        // Texture Map Stage with UV Overlay
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentTex = uiState.activeTexture
                if (currentTex != null) {
                    Box(modifier = Modifier.size(240.dp)) {
                        Image(
                            bitmap = currentTex.asImageBitmap(),
                            contentDescription = "Active 3D Texture Map",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .testTag("active_texture_display")
                        )

                        if (showUvOverlay) {
                            // Draw procedural UV wireframe overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Draw UV unwrapped triangle coordinates
                                drawLine(CyanNeon.copy(alpha = 0.5f), Offset(w * 0.1f, h * 0.1f), Offset(w * 0.5f, h * 0.9f), 1.5f)
                                drawLine(CyanNeon.copy(alpha = 0.5f), Offset(w * 0.5f, h * 0.9f), Offset(w * 0.9f, h * 0.2f), 1.5f)
                                drawLine(CyanNeon.copy(alpha = 0.5f), Offset(w * 0.9f, h * 0.2f), Offset(w * 0.1f, h * 0.1f), 1.5f)

                                drawLine(Color(0x80FF2A85), Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.8f), 1.5f)
                                drawLine(Color(0x80FF2A85), Offset(w * 0.8f, h * 0.8f), Offset(w * 0.5f, h * 0.3f), 1.5f)
                                drawLine(Color(0x80FF2A85), Offset(w * 0.5f, h * 0.3f), Offset(w * 0.2f, h * 0.8f), 1.5f)
                            }
                        }
                    }
                }

                // Header Tag
                Text(
                    text = "${uiState.activeTextureName} [UV Mapped]",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    color = CyanNeon,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color(0xCC0F172A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Action: Apply to Model & Toggle UV Overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("import_texture_btn")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import Texture", fontFamily = TechMono, fontSize = 12.sp)
            }

            Button(
                onClick = { onNavigateTo3D?.invoke() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("view_in_3d_btn")
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apply & View 3D", fontFamily = TechMono, fontSize = 12.sp)
            }
        }

        // Texture Preset Library
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PROCEDURAL TEXTURE PRESETS",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.texturePresets.forEach { preset ->
                        val isSelected = uiState.activeTextureName == preset.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.applyPresetTexture(preset) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) CyanNeon else CardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Image(
                                    bitmap = preset.bitmap.asImageBitmap(),
                                    contentDescription = preset.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = preset.name,
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                color = if (isSelected) CyanNeon else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
