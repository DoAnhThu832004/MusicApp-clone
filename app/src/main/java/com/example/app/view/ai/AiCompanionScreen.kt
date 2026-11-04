package com.example.app.view.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.model.ai.AiPersona
import com.example.app.model.ai.MessageSender
import com.example.app.model.ai.MessageStatus
import com.example.app.model.ai.UiChatMessage
import com.example.app.model.response.Song
import com.example.app.viewmodel.AiCompanionViewModel

/**
 * Màn hình trò chuyện AI Music Companion (Giai đoạn 2 - MVP)
 * Tích hợp 3 Nhân cách âm nhạc (Persona), Đồng bộ bài hát đang nghe,
 * Bộ đệm 45ms chống Recomposition Storm và Nút dừng luồng trực tiếp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCompanionScreen(
    viewModel: AiCompanionViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by rememberSaveable { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Tự động cuộn xuống dưới cùng khi có tin nhắn mới hoặc chunk SSE đổ về
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Music Companion",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.currentPersona.avatarEmoji,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = uiState.currentPersona.displayName + " • " + uiState.currentPersona.roleDescription,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (uiState.currentSession != null) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Xóa cuộc trò chuyện",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Task 2B.4: Persona Selector Bar (3 Chips: DJ GenZ, Nhà Phê Bình, Tâm Giao)
            PersonaSelectorBar(
                selectedPersona = uiState.currentPersona,
                isStreaming = uiState.isStreaming,
                onPersonaSelected = { persona ->
                    viewModel.selectPersona(persona)
                }
            )

            // 2. Task 2B.5: Current Song Context Banner (Đồng bộ bài hát đang nghe từ PlayerManager)
            uiState.currentSong?.let { song ->
                SongContextBanner(
                    song = song,
                    isStreaming = uiState.isStreaming,
                    onAskAboutSong = {
                        val prompt = "Hãy chia sẻ cảm nhận và câu chuyện thú vị về bài hát \"${song.name}\" của nghệ sĩ ${song.artistName ?: "này"} nhé!"
                        viewModel.sendMessage(prompt)
                    }
                )
            }

            // 3. Thông báo lỗi & nút Thử lại an toàn (Safe Retry)
            if (uiState.errorMessage != null) {
                ErrorAlertCard(
                    message = uiState.errorMessage ?: "",
                    canRetry = uiState.canRetry,
                    onRetry = { viewModel.retryLastFailedPrompt() }
                )
            }

            // 4. Task 2B.6: Danh sách bong bóng chat (LazyColumn)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoadingHistory) {
                    EmptyChatWelcomeCard(
                        persona = uiState.currentPersona,
                        onSuggestionClick = { suggestion ->
                            inputText = suggestion
                            viewModel.sendMessage(suggestion)
                            inputText = ""
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatBubbleItem(
                                message = message,
                                persona = uiState.currentPersona
                            )
                        }
                    }
                }

                // Nút "Dừng phản hồi" nổi khi AI đang stream (AC-AI-08)
                if (uiState.isStreaming) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        StopStreamingPill(
                            onClick = { viewModel.stopStreaming() }
                        )
                    }
                }
            }

            // 5. Thanh nhập liệu tin nhắn kèm bộ đếm ký tự 0/1000
            ChatInputBar(
                text = inputText,
                onTextChange = { if (it.length <= 1000) inputText = it },
                isStreaming = uiState.isStreaming,
                persona = uiState.currentPersona,
                onSend = {
                    if (inputText.isNotBlank() && !uiState.isStreaming) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }
            )
        }
    }

    // Dialog xác nhận xóa cuộc trò chuyện
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Xóa cuộc trò chuyện?") },
            text = { Text(text = "Toàn bộ lịch sử tin nhắn trong phiên chat này sẽ bị xóa vĩnh viễn khỏi máy chủ.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteCurrentSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Xóa vĩnh viễn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Hủy")
                }
            }
        )
    }
}

/**
 * Task 2B.4: Thanh chuyển đổi 3 Persona (DJ GenZ, Nhà Phê Bình, Tâm Giao)
 */
@Composable
fun PersonaSelectorBar(
    selectedPersona: AiPersona,
    isStreaming: Boolean,
    onPersonaSelected: (AiPersona) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AiPersona.entries.toTypedArray()) { persona ->
            val isSelected = persona == selectedPersona
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = !isStreaming) { onPersonaSelected(persona) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = persona.avatarEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = persona.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Task 2B.5: Banner hiển thị ngữ cảnh bài hát đang nghe từ PlayerManager
 */
@Composable
fun SongContextBanner(
    song: Song,
    isStreaming: Boolean,
    onAskAboutSong: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Đang phát",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = song.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artistName ?: "Nghệ sĩ chưa rõ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isStreaming) { onAskAboutSong() }
            ) {
                Text(
                    text = "Hỏi bài này ✨",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Task 2B.6: Bong bóng chat hiển thị tin nhắn User và tin nhắn AI với con trỏ gõ '▌'
 */
@Composable
fun ChatBubbleItem(
    message: UiChatMessage,
    persona: AiPersona
) {
    val isUser = message.sender == MessageSender.USER

    // Hiệu ứng nhấp nháy cho con trỏ '▌' khi AI đang sinh văn bản
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = persona.avatarEmoji, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            val bubbleShape = if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
            }

            val bubbleColor = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            val textColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Nội dung tin nhắn
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = message.content,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        // Con trỏ gõ nhấp nháy '▌' khi đang stream
                        if (message.isStreaming) {
                            Text(
                                text = "▌",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }

                    // Nhãn trạng thái đã dừng hoặc lỗi
                    if (message.status == MessageStatus.INTERRUPTED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[Đã dừng phản hồi]",
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    } else if (message.status == MessageStatus.FAILED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[Lỗi phản hồi]",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Nút nổi "Dừng phản hồi" khi đang stream
 */
@Composable
fun StopStreamingPill(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Dừng",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Dừng phản hồi",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Task 2B.6: Thanh nhập liệu tin nhắn kèm bộ đếm 0/1000 ký tự
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isStreaming: Boolean,
    persona: AiPersona,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Trò chuyện với ${persona.displayName}...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank() && !isStreaming,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (text.isNotBlank() && !isStreaming) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                ) {
                    if (isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bộ đếm ký tự 0/1000
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val isNearLimit = text.length >= 900
                Text(
                    text = "${text.length}/1000",
                    fontSize = 10.sp,
                    color = if (isNearLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Thẻ chào mừng khi chưa có tin nhắn nào trong phiên
 */
@Composable
fun EmptyChatWelcomeCard(
    persona: AiPersona,
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = when (persona) {
        AiPersona.GENZ_DJ -> listOf(
            "Gợi ý cho tui vài bài rap/hiphop đang hot trend nhất! 🔥",
            "Bài hát nào đang viral trên mạng xã hội dạo này?",
            "Tui thích giai điệu tươi vui, gợi ý vài bài nghe chill nha."
        )
        AiPersona.MUSIC_CRITIC -> listOf(
            "Phân tích phong cách hòa âm phối khí của nhạc Pop hiện đại.",
            "Vì sao bài hát này lại được đánh giá cao về chuyên môn nghệ thuật?",
            "Chia sẻ về nguồn gốc lịch sử của thể loại nhạc Jazz & Blues."
        )
        AiPersona.HEALING_FRIEND -> listOf(
            "Hôm nay tâm trạng hơi mệt mỏi, muốn nghe chút gì đó bình yên... 🌿",
            "Gợi ý cho mình một bài hát nhẹ nhàng xoa dịu nỗi buồn.",
            "Cần một danh sách bài hát êm dịu để tập trung học tập."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = persona.avatarEmoji, fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chào bạn! Tôi là ${persona.displayName}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = persona.roleDescription,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Gợi ý câu hỏi:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSuggestionClick(suggestion) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = suggestion,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Thẻ cảnh báo lỗi kèm nút Thử lại
 */
@Composable
fun ErrorAlertCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Cảnh báo lỗi",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (canRetry) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Thử lại",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thử lại",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
