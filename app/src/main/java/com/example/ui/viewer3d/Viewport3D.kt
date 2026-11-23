package com.example.ui.viewer3d

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.Hardware3DRenderProfile
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.PaletteConstraintType
import com.example.model.model3d.Face3D
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.Vector3
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.theme.ViewportBg
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class RenderMode(val label: String) {
    SHADED("Shaded"),
    WIREFRAME("Wireframe"),
    NORMALS("Normals"),
    POINTS("Points")
}

@Composable
fun Viewport3D(
    mesh: Mesh3D,
    modifier: Modifier = Modifier,
    activeTexture: Bitmap? = null,
    hardwareSpec: ConsoleHardwareSpec? = null,
    hardwareSimulationEnabled: Boolean = false,
    onToggleHardwareSimulation: (() -> Unit)? = null,
    onExportObj: (() -> Unit)? = null,
    externalYawRadians: Float? = null,
    externalPitchRadians: Float? = null,
    externalZoom: Float? = null,
    externalLightAngle: Float? = null,
    initialAutoRotate: Boolean = true,
    onCameraOrientationChanged: ((yawRad: Float, pitchRad: Float, zoom: Float) -> Unit)? = null
) {
    var yaw by remember { mutableFloatStateOf(externalYawRadians ?: 0.7f) }
    var pitch by remember { mutableFloatStateOf(externalPitchRadians ?: 0.4f) }
    var zoom by remember { mutableFloatStateOf(externalZoom ?: 1.3f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var isAutoRotate by remember { mutableStateOf(initialAutoRotate) }
    var renderMode by remember { mutableStateOf(RenderMode.SHADED) }
    var lightAngle by remember { mutableFloatStateOf(externalLightAngle ?: 0.8f) }

    val resolvedYaw = externalYawRadians ?: yaw
    val resolvedPitch = externalPitchRadians ?: pitch
    val resolvedZoom = externalZoom ?: zoom
    val resolvedLight = externalLightAngle ?: lightAngle

    val infiniteTransition = rememberInfiniteTransition(label = "turntable")
    val turntableYaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "turntableAngle"
    )

    val effectiveYaw = if (isAutoRotate) resolvedYaw + turntableYaw else resolvedYaw
    val activeHw = if (hardwareSimulationEnabled) hardwareSpec else null

    val bgTint = when (activeHw?.paletteType) {
        PaletteConstraintType.DMG_4_OLIVE -> Color(0xFF0F380F)
        PaletteConstraintType.VIRTUAL_BOY_4_RED -> Color(0xFF0A0000)
        else -> ViewportBg
    }

    Box(
        modifier = modifier
            .background(bgTint)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomFactor, _ ->
                    isAutoRotate = false
                    val nextZoom = (resolvedZoom * zoomFactor).coerceIn(0.4f, 4.0f)
                    val nextYaw = resolvedYaw + pan.x * 0.01f
                    val nextPitch = (resolvedPitch - pan.y * 0.01f).coerceIn(-1.5f, 1.5f)
                    zoom = nextZoom
                    panX += pan.x * 0.005f
                    panY += pan.y * 0.005f
                    yaw = nextYaw
                    pitch = nextPitch
                    onCameraOrientationChanged?.invoke(nextYaw, nextPitch, nextZoom)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("viewport_3d_canvas")) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panX * (width / 2f)
            val centerY = height / 2f + panY * (height / 2f)
            val scale = minOf(width, height) * 0.38f * resolvedZoom

            // Draw subtle 3D floor grid
            drawViewportGrid(centerX, centerY, scale, effectiveYaw, resolvedPitch, activeHw)

            if (mesh.vertices.isNotEmpty()) {
                val lx = cos(resolvedLight)
                val ly = 0.8f
                val lz = sin(resolvedLight)
                val lightDir = Vector3(lx, ly, lz).normalize()

                renderMesh(
                    mesh = mesh,
                    centerX = centerX,
                    centerY = centerY,
                    scale = scale,
                    yaw = effectiveYaw,
                    pitch = resolvedPitch,
                    renderMode = renderMode,
                    lightDir = lightDir,
                    hardwareSpec = activeHw
                )
            }

            // Draw CRT / LCD Scanline overlay when emulating retro hardware
            if (activeHw != null && activeHw.screenHeight <= 272) {
                val scanlineStep = 6f
                val scanColor = if (activeHw.paletteType == PaletteConstraintType.VIRTUAL_BOY_4_RED) {
                    Color(0x55000000)
                } else {
                    Color(0x28000000)
                }
                var sy = 0f
                while (sy < height) {
                    drawLine(scanColor, Offset(0f, sy), Offset(width, sy), strokeWidth = 1.5f)
                    sy += scanlineStep
                }
            }
        }

        // Top HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xCC090D16),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = if (activeHw != null) "${mesh.name} • ${activeHw.shortName}" else mesh.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (activeHw != null) AmberGold else CyanNeon
                    )
                    Text(
                        text = if (activeHw != null) {
                            "${activeHw.renderProfile3D.badge.take(42)} | V:${mesh.vertexCount} F:${mesh.faceCount}"
                        } else {
                            "V: ${mesh.vertexCount}  |  F: ${mesh.faceCount}  |  Zoom: ${String.format("%.1fx", zoom)}"
                        },
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onToggleHardwareSimulation != null && hardwareSpec != null) {
                    IconButton(
                        onClick = onToggleHardwareSimulation,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (hardwareSimulationEnabled) AmberGold else Color(0xCC1E293B),
                                CircleShape
                            )
                            .testTag("toggle_hw_sim_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Toggle Hardware Silicon Emulation",
                            tint = if (hardwareSimulationEnabled) Color(0xFF031B26) else AmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Turntable Auto-rotate
                IconButton(
                    onClick = { isAutoRotate = !isAutoRotate },
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isAutoRotate) CyanNeon else Color(0xCC1E293B), CircleShape)
                        .testTag("toggle_turntable")
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Auto Rotate",
                        tint = if (isAutoRotate) Color(0xFF031B26) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reset Camera
                IconButton(
                    onClick = {
                        yaw = 0.7f
                        pitch = 0.4f
                        zoom = 1.3f
                        panX = 0f
                        panY = 0f
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xCC1E293B), CircleShape)
                        .testTag("reset_camera")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Reset Camera",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onExportObj != null) {
                    IconButton(
                        onClick = onExportObj,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xCC1E293B), CircleShape)
                            .testTag("export_obj_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export OBJ",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Mode Switcher
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RenderMode.entries.forEach { mode ->
                val selected = renderMode == mode
                AssistChip(
                    onClick = { renderMode = mode },
                    label = {
                        Text(
                            text = mode.label,
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            color = if (selected) Color(0xFF031B26) else Color.White
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) CyanNeon else Color(0xDD0F172A),
                        labelColor = if (selected) Color(0xFF031B26) else Color.White
                    ),
                    modifier = Modifier.testTag("render_mode_${mode.name.lowercase()}")
                )
            }
        }
    }
}

private fun DrawScope.drawViewportGrid(
    centerX: Float,
    centerY: Float,
    scale: Float,
    yaw: Float,
    pitch: Float,
    hardwareSpec: ConsoleHardwareSpec?
) {
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val cosP = cos(pitch)
    val sinP = sin(pitch)

    val gridHalfSize = 1.5f
    val gridSteps = 8
    val step = (gridHalfSize * 2f) / gridSteps
    val groundY = -0.9f

    val gridColor = when (hardwareSpec?.paletteType) {
        PaletteConstraintType.DMG_4_OLIVE -> Color(0x448BAC0F)
        PaletteConstraintType.VIRTUAL_BOY_4_RED -> Color(0x55AA0000)
        else -> Color(0x1F38BDF8)
    }
    val axisColorX = Color(0x40EF4444)
    val axisColorZ = Color(0x4010B981)

    fun projectPoint(gx: Float, gy: Float, gz: Float): Offset? {
        val x1 = gx * cosY + gz * sinY
        val z1 = -gx * sinY + gz * cosY
        val y2 = gy * cosP - z1 * sinP
        val z2 = gy * sinP + z1 * cosP

        val dist = 3.2f - z2
        if (dist <= 0.2f) return null
        val proj = 2.4f / dist
        return Offset(centerX + x1 * scale * proj, centerY - y2 * scale * proj)
    }

    for (i in 0..gridSteps) {
        val coord = -gridHalfSize + i * step
        val p1 = projectPoint(coord, groundY, -gridHalfSize)
        val p2 = projectPoint(coord, groundY, gridHalfSize)
        if (p1 != null && p2 != null) {
            val strokeColor = if (i == gridSteps / 2) axisColorZ else gridColor
            drawLine(strokeColor, p1, p2, strokeWidth = 1f)
        }

        val p3 = projectPoint(-gridHalfSize, groundY, coord)
        val p4 = projectPoint(gridHalfSize, groundY, coord)
        if (p3 != null && p4 != null) {
            val strokeColor = if (i == gridSteps / 2) axisColorX else gridColor
            drawLine(strokeColor, p3, p4, strokeWidth = 1f)
        }
    }
}

private fun DrawScope.renderMesh(
    mesh: Mesh3D,
    centerX: Float,
    centerY: Float,
    scale: Float,
    yaw: Float,
    pitch: Float,
    renderMode: RenderMode,
    lightDir: Vector3,
    hardwareSpec: ConsoleHardwareSpec?
) {
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val cosP = cos(pitch)
    val sinP = sin(pitch)

    val rotated = ArrayList<Vector3>(mesh.vertices.size)
    val projected = ArrayList<Offset?>(mesh.vertices.size)

    val camDistance = 3.5f
    val snapStep = hardwareSpec?.vertexSnapGrid ?: 0f

    for (v in mesh.vertices) {
        val x1 = v.x * cosY + v.z * sinY
        val z1 = -v.x * sinY + v.z * cosY
        val y2 = v.y * cosP - z1 * sinP
        val z2 = v.y * sinP + z1 * cosP

        val rotV = Vector3(x1, y2, z2)
        rotated.add(rotV)

        val dist = camDistance - z2
        if (dist > 0.1f) {
            val proj = 2.4f / dist
            var sx = centerX + x1 * scale * proj
            var sy = centerY - y2 * scale * proj

            // Simulate integer screen-space coordinate snap (e.g. PlayStation 1 GTE, Super FX, NDS)
            if (snapStep > 0f) {
                sx = (sx / snapStep).roundToInt() * snapStep
                sy = (sy / snapStep).roundToInt() * snapStep
            }
            projected.add(Offset(sx, sy))
        } else {
            projected.add(null)
        }
    }

    if (renderMode == RenderMode.POINTS) {
        val points = mutableListOf<Offset>()
        for (p in projected) {
            if (p != null) points.add(p)
        }
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = CyanNeon,
            strokeWidth = 4f
        )
        return
    }

    data class DrawableFace(
        val face: Face3D,
        val avgDepth: Float,
        val normalZ: Float,
        val worldNormal: Vector3,
        val faceIndex: Int
    )

    val faceStride = (mesh.faces.size / 6000).coerceAtLeast(1)
    val drawableFaces = ArrayList<DrawableFace>((mesh.faces.size / faceStride).coerceAtLeast(1))
    val forceWireframe = hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.NONE_2D_WIREFRAME ||
            hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.CX4_TRIG_WIREFRAME

    var idx = 0
    while (idx < mesh.faces.size) {
        val face = mesh.faces[idx]
        if (face.indices.size >= 3) {
            val i0 = face.indices[0]
            val i1 = face.indices[1]
            val i2 = face.indices[2]
            if (i0 in 0 until rotated.size && i1 in 0 until rotated.size && i2 in 0 until rotated.size) {
                val v0 = rotated[i0]
                val v1 = rotated[i1]
                val v2 = rotated[i2]

                val e1 = v1 - v0
                val e2 = v2 - v0
                val norm = e1.cross(e2).normalize()

                if (norm.z > -0.05f || renderMode == RenderMode.WIREFRAME || forceWireframe) {
                    val avgZ = (v0.z + v1.z + v2.z) / 3f
                    drawableFaces.add(DrawableFace(face, avgZ, norm.z, norm, idx))
                }
            }
        }
        idx += faceStride
    }

    drawableFaces.sortBy { it.avgDepth }

    val path = Path()
    val stippleStroke = Stroke(
        width = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
    )

    for (df in drawableFaces) {
        val face = df.face
        path.reset()
        var valid = true

        val firstPt = projected[face.indices[0]] ?: continue
        path.moveTo(firstPt.x, firstPt.y)

        for (i in 1 until face.indices.size) {
            val pt = projected[face.indices[i]]
            if (pt == null) {
                valid = false
                break
            }
            path.lineTo(pt.x, pt.y)
        }
        path.close()

        if (!valid) continue

        if (renderMode == RenderMode.WIREFRAME || forceWireframe) {
            val wireColor = when (hardwareSpec?.paletteType) {
                PaletteConstraintType.DMG_4_OLIVE -> Color(0xFF9BBC0F)
                PaletteConstraintType.VIRTUAL_BOY_4_RED -> Color(0xFFFF1A1A)
                PaletteConstraintType.NES_2C02_2BPP_54 -> Color(0xFF38CC6C)
                else -> if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.CX4_TRIG_WIREFRAME) MagentaNeon else CyanNeon
            }
            drawPath(path, wireColor.copy(alpha = 0.2f), style = Fill)
            drawPath(path, wireColor, style = Stroke(width = 1.8f))
            continue
        }

        when (renderMode) {
            RenderMode.SHADED -> {
                val diffuseRaw = max(0f, df.worldNormal.dot(lightDir))

                // Nintendo DS Toon-Table Stepped Cel-Shading
                val diffuse = if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.NDS_FIXED_TOON_2048) {
                    when {
                        diffuseRaw > 0.72f -> 1.0f
                        diffuseRaw > 0.35f -> 0.62f
                        else -> 0.28f
                    }
                } else {
                    diffuseRaw
                }

                // Specular highlight for 6th-gen and modern GPUs
                val specular = if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.POWERVR_TBDR_DREAMCAST ||
                    hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.PS2_EE_GS_EDRAM ||
                    hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.GAMECUBE_TEV_STAGES ||
                    hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.MODERN_PBR_SHADER
                ) {
                    diffuseRaw.pow(6f) * 0.35f
                } else 0f

                val ambient = 0.28f
                val lightFactor = (ambient + diffuse * 0.72f).coerceIn(0.15f, 1.0f)

                var rInt = (((face.color ushr 16) and 0xFF) * lightFactor + specular * 255f).toInt().coerceIn(0, 255)
                var gInt = (((face.color ushr 8) and 0xFF) * lightFactor + specular * 255f).toInt().coerceIn(0, 255)
                var bInt = ((face.color and 0xFF) * lightFactor + specular * 255f).toInt().coerceIn(0, 255)

                // N64 Distance Horizon Fog blending
                if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.N64_RCP_TMEM_3POINT) {
                    val fogFactor = ((0.5f - df.avgDepth) * 0.25f).coerceIn(0f, 0.45f)
                    rInt = (rInt * (1f - fogFactor) + 25 * fogFactor).toInt()
                    gInt = (gInt * (1f - fogFactor) + 40 * fogFactor).toInt()
                    bInt = (bInt * (1f - fogFactor) + 75 * fogFactor).toInt()
                }

                val shadedArgb = (0xFF shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
                val finalArgb = if (hardwareSpec != null) {
                    HardwareAssetSimulator.quantizeColorForHardware(shadedArgb, hardwareSpec.paletteType)
                } else {
                    shadedArgb
                }

                val drawColor = Color(finalArgb)
                drawPath(path, drawColor, style = Fill)

                // Super FX GSU / Sega SVP Bayer Crosshatch Dither simulation on mid-tones
                if ((hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT ||
                            hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.SVP_DSP_FLAT) &&
                    diffuse in 0.25f..0.75f
                ) {
                    drawPath(path, Color(0x44000000), style = stippleStroke)
                }

                // Sega Saturn VDP1 Screen-Door Mesh Stipple Transparency on every 3rd face
                if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.SATURN_VDP1_QUAD_MESH &&
                    df.faceIndex % 3 == 0
                ) {
                    drawPath(path, Color(0x66090D16), style = stippleStroke)
                }

                // NDS Toon Contour Marking or Standard Edge
                val edgeStroke = if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.NDS_FIXED_TOON_2048) {
                    Stroke(width = 2.0f)
                } else {
                    Stroke(width = 1f)
                }
                val edgeColor = if (hardwareSpec?.renderProfile3D == Hardware3DRenderProfile.NDS_FIXED_TOON_2048) {
                    Color(0xAA000000)
                } else {
                    Color(0x22000000)
                }
                drawPath(path, edgeColor, style = edgeStroke)
            }

            RenderMode.NORMALS -> {
                val nr = (df.worldNormal.x * 0.5f + 0.5f).coerceIn(0f, 1f)
                val ng = (df.worldNormal.y * 0.5f + 0.5f).coerceIn(0f, 1f)
                val nb = (df.worldNormal.z * 0.5f + 0.5f).coerceIn(0f, 1f)
                drawPath(path, Color(nr, ng, nb, 1f), style = Fill)
                drawPath(path, Color(0x33000000), style = Stroke(width = 0.8f))
            }

            RenderMode.WIREFRAME, RenderMode.POINTS -> {}
        }
    }
}
