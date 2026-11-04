package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app.model.repository.AiChatRepository

/**
 * Factory khởi tạo AiCompanionViewModel với dependency injection cần thiết
 */
class AiCompanionViewModelFactory(
    private val repository: AiChatRepository,
    private val playerManager: PlayerManager = PlayerManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiCompanionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiCompanionViewModel(repository, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
