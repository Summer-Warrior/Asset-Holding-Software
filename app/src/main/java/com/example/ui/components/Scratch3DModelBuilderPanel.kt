package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.masterpack.TWENTY_COLOR_WHEEL
import com.example.model.model3d.PrimitiveShapeType
import com.example.model.model3d.Scratch3DBuilderEngine
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.viewer3d.Viewport3D
import java.util.Locale

/**
 * Hardware-Foundry Obsidian blank-slate 3D primitive/kitbash model sculptor.
 * Starts from 0 parts and lets creators compose original 3D `.OBJ` assets with live Viewport3D preview,
 * symmetric Mirror-X twins, 20-Color Wheel shading, and direct save to Studio & Room Vault.
 */
@Composable
fun Scratch3DModelBuilderPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val project = uiState.scratch3DProject
    val compiledMesh = remember(project) {
        Scratch3DBuilderEngine.compileProjectToMesh3D(project)
    }
    val selectedPart = project.selectedPart

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1120)),
        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("scratch_3d_builder_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "FROM-SCRATCH 3D PRIMITIVE SCULPTOR",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "${project.parts.size} parts • ${compiledMesh.vertexCount}v / ${compiledMesh.faceCount}f",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.startNewBlank3DProject("New Original 3D Model") },
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("scratch_3d_reset_blank_btn")
                ) {
                    Icon(Icons.Default.LayersClear, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Blank (0)", fontFamily = TechMono, fontSize = 10.sp)
                }
            }

            // Project Name Input
            OutlinedTextField(
                value = project.name,
                onValueChange = { viewModel.updateScratch3DProjectName(it) },
                singleLine = true,
                label = { Text("3D Model Name", fontFamily = TechMono, fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = EmeraldGreen,
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scratch_3d_name_field")
            )

            // Live 3D Viewport Preview (or Empty Blank-Slate Callout)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                if (project.parts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .testTag("scratch_3d_empty_callout"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "Blank 3D Scene",
                                tint = CyanNeon.copy(alpha = 0.65f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Blank 3D Scene — Tap a shape below to start building from scratch",
                                fontFamily = TechMono,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Combine Boxes, Spheres, Cylinders, Cones, Pyramids, Rings & Mirrored Wings",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Viewport3D(
                        mesh = compiledMesh,
                        activeTexture = null,
                        hardwareSpec = uiState.selectedHardware,
                        hardwareSimulationEnabled = false,
                        initialAutoRotate = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // + Add Shape Chip Row (All 7 Primitives)
            Text(
                text = "+ ADD PRIMITIVE SHAPE",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = CyanNeon
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimitiveShapeType.entries.forEach { shape ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.addScratch3DPart(shape) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text(shape.label, fontFamily = TechMono, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("scratch_3d_add_${shape.name.lowercase()}")
                    )
                }
            }

            // Added Parts List & Duplicate / Delete Controls
            if (project.parts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCENE PARTS (${project.parts.size})",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.duplicateSelectedScratch3DPart() },
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("scratch_3d_duplicate_part_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duplicate", fontFamily = TechMono, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.deleteSelectedScratch3DPart() },
                            border = BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("scratch_3d_delete_part_btn")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", fontFamily = TechMono, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    project.parts.forEach { part ->
                        val isSelected = part.id == selectedPart?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectScratch3DPart(part.id) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(part.colorLong))
                                )
                            },
                            label = {
                                Text(
                                    text = if (part.mirrorX) "${part.name} (2x)" else part.name,
                                    fontFamily = TechMono,
                                    fontSize = 10.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("scratch_3d_part_chip_${part.id}")
                        )
                    }
                }
            }

            // Selected Part Transform Controls
            if (selectedPart != null) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSFORM: ${selectedPart.name.uppercase(Locale.US)}",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Mirror X",
                                    fontFamily = TechMono,
                                    fontSize = 10.sp,
                                    color = CyanNeon
                                )
                                Switch(
                                    checked = selectedPart.mirrorX,
                                    onCheckedChange = { enabled ->
                                        viewModel.updateSelectedScratch3DPart { it.copy(mirrorX = enabled) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF031B26),
                                        checkedTrackColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("scratch_3d_mirror_x_switch")
                                )
                            }
                        }

                        // Position X / Y / Z
                        TransformSliderRow(
                            label = "Pos X",
                            value = selectedPart.posX,
                            range = -1.5f..1.5f,
                            formattedValue = String.format(Locale.US, "%.2f", selectedPart.posX),
                            accent = CyanNeon,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(posX = v) } }
                        )
                        TransformSliderRow(
                            label = "Pos Y",
                            value = selectedPart.posY,
                            range = -1.5f..1.5f,
                            formattedValue = String.format(Locale.US, "%.2f", selectedPart.posY),
                            accent = CyanNeon,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(posY = v) } }
                        )
                        TransformSliderRow(
                            label = "Pos Z",
                            value = selectedPart.posZ,
                            range = -1.5f..1.5f,
                            formattedValue = String.format(Locale.US, "%.2f", selectedPart.posZ),
                            accent = CyanNeon,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(posZ = v) } }
                        )

                        // Scale W / H / D
                        TransformSliderRow(
                            label = "Scale W",
                            value = selectedPart.scaleX,
                            range = 0.1f..1.8f,
                            formattedValue = String.format(Locale.US, "%.2f", selectedPart.scaleX),
                            accent = EmeraldGreen,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(scaleX = v) } }
                        )
                        TransformSliderRow(
                            label = "Scale H",
                            value = selectedPart.scaleY,
                            range = 0.1f..1.8f,
                            formattedValue = String.format(Locale.US, "%.2f", selectedPart.scaleY),
                            accent = EmeraldGreen,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(scaleY = v) } }
                        )
                        TransformSliderRow(
                            label = "Scale D",
                            value = selectedPart.scaleZ,
                            range = 0.1f..1.8f,
                            formattedValue = String.format(Locale.US, "%.2f", selectedPart.scaleZ),
                            accent = EmeraldGreen,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(scaleZ = v) } }
                        )

                        // Rotate Y
                        TransformSliderRow(
                            label = "Rot Y",
                            value = selectedPart.rotYDeg,
                            range = 0f..360f,
                            formattedValue = "${selectedPart.rotYDeg.toInt()}°",
                            accent = AmberGold,
                            onValueChange = { v -> viewModel.updateSelectedScratch3DPart { it.copy(rotYDeg = v) } }
                        )

                        // 20-Color Wheel Swatch Strip
                        Text(
                            text = "PART COLOR (20-COLOR WHEEL)",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TWENTY_COLOR_WHEEL.forEach { entry ->
                                val argbLong = entry.colorInt.toLong() and 0xFFFFFFFFL
                                val isColorSelected = selectedPart.colorLong == argbLong
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(entry.colorInt))
                                        .border(
                                            width = if (isColorSelected) 3.dp else 1.dp,
                                            color = if (isColorSelected) Color.White else CardBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateSelectedScratch3DPart { it.copy(colorLong = argbLong) }
                                        }
                                        .testTag("scratch_3d_color_${entry.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Primary Emerald Bake & Save CTA
            Button(
                onClick = { viewModel.commitScratch3DProjectToStudioAndVault() },
                enabled = project.parts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = Color(0xFF031B12),
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("scratch_3d_bake_save_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Bake & Save Brand-New 3D Model (.OBJ)",
                    fontFamily = TechMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TransformSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formattedValue,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = accent,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End
        )
    }
}
