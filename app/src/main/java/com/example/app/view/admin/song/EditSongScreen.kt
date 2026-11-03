package com.example.app.view.admin.song

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import com.example.app.model.response.Song
import com.example.app.view.general.DateDialog
import com.example.app.viewmodel.SongViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect

@Composable
fun EditSongScreen(
    songViewModel: SongViewModel,
    songToEdit: Song,
    songId : String
) {
    val songState by songViewModel.songState.collectAsState()
    LaunchedEffect(Unit) {
        songViewModel.getGenres()
    }
    val selectedGenres = remember(songToEdit) { 
        mutableStateListOf<String>().apply {
            songToEdit.genres?.let { genres ->
                addAll(genres.map { it.id })
            }
        }
    }
    var showGenreDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LazyColumn() {
        item {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var nameSong by remember(songToEdit) {
                    mutableStateOf(songToEdit?.name ?: "")
                }
                var description by remember(songToEdit) { mutableStateOf(songToEdit?.description ?: "") }
                var isAiEnriching by remember { mutableStateOf(false) }
                var duration by remember(songToEdit) { mutableStateOf(songToEdit?.duration.toString() ?: "") }
                var releasedDate by remember(songToEdit) { mutableStateOf(songToEdit?.releasedDate ?: "") }
                var status by remember(songToEdit) { mutableStateOf(songToEdit?.status ?: "") }
                var type by remember(songToEdit) { mutableStateOf(songToEdit?.type ?: "") }
                var showDatePicker by remember { mutableStateOf(false) }
                if (showDatePicker) {
                    DateDialog(
                        onDateSelected = { formattedDate ->
                            releasedDate = formattedDate
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
                Text(
                    text = stringResource(R.string.chinh_sua_bai_hat),
                    fontSize = 20.sp,
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = nameSong,
                    onValueChange = { nameSong = it},
                    label = {
                        Text(
                            text = stringResource(R.string.ten),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.MusicNote, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mo_ta),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = {
                            isAiEnriching = true
                            songViewModel.enrichSongMetadata(
                                songId = songId,
                                overwrite = true,
                                onSuccess = { enriched ->
                                    isAiEnriching = false
                                    description = enriched.description ?: description
                                    enriched.genres?.let { aiGenres ->
                                        selectedGenres.clear()
                                        selectedGenres.addAll(aiGenres.map { it.id })
                                    }
                                    Toast.makeText(context, "✨ AI đã tạo mô tả và gợi ý thể loại!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    isAiEnriching = false
                                    Toast.makeText(context, "Không thể tạo gợi ý AI: $msg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = !isAiEnriching,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAiEnriching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Đang sinh gợi ý...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "AI Gợi ý mô tả & Thể loại", fontSize = 12.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it},
                    maxLines = 4,
                    label = {
                        Text(
                            text = stringResource(R.string.mo_ta),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it},
                    label = {
                        Text(
                            text = stringResource(R.string.trang_thai),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it},
                    label = {
                        Text(
                            text = stringResource(R.string.thoi_gian),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Timer, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = releasedDate,
                    onValueChange = { releasedDate = it},
                    label = {
                        Text(
                            text = stringResource(R.string.ngay_phat_hanh),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.DateRange, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it},
                    label = {
                        Text(
                            text = stringResource(R.string.ngay_phat_hanh),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = if (selectedGenres.isEmpty()) "Chưa chọn thể loại" else "${selectedGenres.size} thể loại đã chọn",
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            text = "Thể loại",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { showGenreDialog = true },
                    shape = RoundedCornerShape(15.dp)
                )

                if (showGenreDialog) {
                    AlertDialog(
                        onDismissRequest = { showGenreDialog = false },
                        title = { Text(text = "Chọn thể loại") },
                        text = {
                            LazyColumn {
                                val genres = songState.genres ?: emptyList()
                                items(genres) { genre ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (selectedGenres.contains(genre.id)) {
                                                    selectedGenres.remove(genre.id)
                                                } else {
                                                    selectedGenres.add(genre.id)
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedGenres.contains(genre.id),
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    selectedGenres.add(genre.id)
                                                } else {
                                                    selectedGenres.remove(genre.id)
                                                }
                                            }
                                        )
                                        Text(text = genre.name, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showGenreDialog = false }) {
                                Text("Xong")
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.padding(top = 8.dp))

                Button(
                    onClick = {
                        if(nameSong.isEmpty() || description.isEmpty() || duration.isEmpty() || releasedDate.isEmpty() || type.isEmpty() || status.isEmpty()) {
                            val errorMessage = context.getString(R.string.thong_bao_khong_de_trong)
                            Toast.makeText(
                                context,
                                errorMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            songViewModel.updateSong(songId, nameSong, description,status, duration.toInt(), releasedDate, type, selectedGenres.toList())
                            val successMessage = context.getString(R.string.cap_nhap_bai_hat_thanh_cong)
                            Toast.makeText(
                                context,
                                successMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                            nameSong = ""
                            description = ""
                            duration = ""
                            releasedDate = ""
                            type = ""
                            status = ""
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.xac_nhan),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}