package com.example.app.view.admin.report

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.app.model.response.Report
import com.example.app.view.general.ConfirmDialog
import com.example.app.viewmodel.AlbumViewModel
import com.example.app.viewmodel.ArtistViewModel
import com.example.app.viewmodel.PlaylistViewModel
import com.example.app.viewmodel.ReportViewModel
import com.example.app.viewmodel.SongViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DetailReportScreen(
    report: Report,
    reportViewModel: ReportViewModel,
    songViewModel: SongViewModel,
    albumViewModel: AlbumViewModel,
    playlistViewModel: PlaylistViewModel,
    artistViewModel: ArtistViewModel
) {
    val songState by songViewModel.songState.collectAsState()
    val albumState by albumViewModel.albumState.collectAsState()
    val playlistState by playlistViewModel.playlistState.collectAsState()
    val artistState by artistViewModel.artistState.collectAsState()

    val density = LocalDensity.current
    val revealSizeDp = 176.dp
    val maxRevealPx = with(density) { -revealSizeDp.toPx() }
    val snapThreshold = maxRevealPx / 2
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val targetData = remember(
        report.targetId, report.targetType,
        songState.songs, albumState.albums,
        playlistState.playlists, artistState.artists
    ) {
        var extractedName = "ID: ${report.targetId}"
        var extractedImageUrl: String? = null
        var isFound = false
        when (report.targetType.lowercase()) {
            "song" -> {
                songState.songs?.find { it.id == report.targetId }?.let {
                    extractedName = it.name; extractedImageUrl = it.imageUrl; isFound = true
                }
            }
            "album" -> {
                albumState.albums?.find { it.id == report.targetId }?.let {
                    extractedName = it.name; extractedImageUrl = it.imageUrlA; isFound = true
                }
            }
            "playlist" -> {
                playlistState.playlists?.find { it.id == report.targetId }?.let {
                    extractedName = it.title; extractedImageUrl = it.imageUrlP; isFound = true
                }
            }
            "artist" -> {
                artistState.artists?.find { it.id == report.targetId }?.let {
                    extractedName = it.name; extractedImageUrl = it.imageUrlAr; isFound = true
                }
            }
        }
        TargetUIData(name = extractedName, imageUrl = extractedImageUrl, isFound = isFound)
    }

    // Status colors
    val statusColor = when (report.status) {
        "PENDING"     -> Color(0xFFFF9800)
        "IN_PROGRESS" -> Color(0xFF2196F3)
        "RESOLVED"    -> Color(0xFF4CAF50)
        else          -> Color.Gray
    }
    val statusLabel = when (report.status) {
        "PENDING"     -> "Chờ xử lý"
        "IN_PROGRESS" -> "Đang xử lý"
        "RESOLVED"    -> "Đã xử lý"
        else          -> report.status
    }
    val typeColor = when (report.targetType.lowercase()) {
        "song"     -> Color(0xFF6C63FF)
        "album"    -> Color(0xFF00BCD4)
        "playlist" -> Color(0xFFE91E63)
        "artist"   -> Color(0xFF4CAF50)
        else       -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterEnd
    ) {
        val progress = (offsetX.value / maxRevealPx).coerceIn(0f, 1f)

        // Action buttons revealed on swipe
        Row(
            modifier = Modifier
                .width(revealSizeDp)
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            if (report.status == "PENDING") {
                // → IN_PROGRESS button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(0.75f + 0.25f * progress)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .then(
                            Modifier.let { m ->
                                m
                            }
                        )
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            reportViewModel.updateReport(report.id, "IN_PROGRESS")
                            Toast.makeText(context, "Đã chuyển sang đang xử lý", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = "Xử lý",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Xử lý", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                // → RESOLVED button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(0.75f + 0.25f * progress)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            reportViewModel.updateReport(report.id, "RESOLVED")
                            Toast.makeText(context, "Đã đánh dấu hoàn thành", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Hoàn thành",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Xong", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            } else if (report.status == "IN_PROGRESS") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(0.75f + 0.25f * progress)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            reportViewModel.updateReport(report.id, "RESOLVED")
                            Toast.makeText(context, "Đã đánh dấu hoàn thành", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Hoàn thành",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Xong", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            } else if (report.status == "RESOLVED") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(0.75f + 0.25f * progress)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFEF5350), Color(0xFFC62828))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            reportViewModel.deleteReport(report.id)
                            Toast.makeText(context, "Đã xóa báo cáo", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text("Xóa", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Main card content (swipeable)
        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newVal = (offsetX.value + delta).coerceIn(maxRevealPx * 1.1f, 0f)
                        scope.launch { offsetX.snapTo(newVal) }
                    },
                    onDragStopped = {
                        val targetOffset = if (offsetX.value < snapThreshold) maxRevealPx else 0f
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = targetOffset,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (targetData.imageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(targetData.imageUrl),
                            contentDescription = null,
                            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    // Type badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(typeColor.copy(alpha = 0.92f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = report.targetType.uppercase(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Top row: reporter + status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = report.username,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Target ID
                    Text(
                        text = "ID: ${report.targetId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.3.sp
                    )

                    // Target name (tên bài hát / album / playlist / artist)
                    if (targetData.isFound) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = targetData.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Đang tải...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Issue type chip & AI badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = report.issueType,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE57373),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (report.description.contains("[AI Auto-Flag")) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF6C63FF).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🤖 AI Tự Động Phát Hiện",
                                    color = Color(0xFF6C63FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Description
                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private data class TargetUIData(
    val name: String,
    val imageUrl: String?,
    val isFound: Boolean
)