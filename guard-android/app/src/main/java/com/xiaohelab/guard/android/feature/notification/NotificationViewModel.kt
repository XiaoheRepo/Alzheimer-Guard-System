package com.xiaohelab.guard.android.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.Notification
import com.xiaohelab.guard.android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

sealed interface NotificationUiEffect {
    data class ShowToast(val message: String) : NotificationUiEffect
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state

    private val _effect = MutableSharedFlow<NotificationUiEffect>()
    val effect: SharedFlow<NotificationUiEffect> = _effect

    init {
        observeLocal()
        fetchRemote()
    }

    private fun observeLocal() {
        notificationRepository.observeNotifications()
            .onEach { list -> _state.update { it.copy(notifications = list) } }
            .launchIn(viewModelScope)
    }

    fun fetchRemote(cursor: String? = null) {
        if (cursor == null) {
            _state.update { it.copy(loading = true, error = null) }
        } else {
            _state.update { it.copy(loadingMore = true) }
        }
        viewModelScope.launch {
            when (val result = notificationRepository.fetchNotifications(cursor, limit = 20)) {
                is ApiResult.Success -> {
                    val (_, nextCursor) = result.data
                    _state.update { s ->
                        s.copy(
                            loading = false,
                            loadingMore = false,
                            nextCursor = nextCursor,
                            hasMore = nextCursor != null
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, loadingMore = false, error = result.message) }
            }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.hasMore && !_state.value.loadingMore) fetchRemote(cursor)
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markRead(notificationId)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (notificationRepository.markAllRead()) {
                is ApiResult.Success -> _effect.emit(NotificationUiEffect.ShowToast("已全部标为已读"))
                is ApiResult.Failure -> {}
            }
        }
    }
}
