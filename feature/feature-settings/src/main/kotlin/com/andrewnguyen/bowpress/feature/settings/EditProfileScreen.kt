package com.andrewnguyen.bowpress.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.bp.BPCard
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val initialName: String = "",
    val name: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean
        get() {
            val trimmed = name.trim()
            return trimmed.isNotEmpty() && !saving && trimmed != initialName.trim()
        }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state: MutableStateFlow<EditProfileUiState> = run {
        val initial = userRepository.currentUser.value?.name.orEmpty()
        MutableStateFlow(EditProfileUiState(initialName = initial, name = initial))
    }
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    fun save() {
        val name = _state.value.name.trim()
        if (name.isEmpty()) {
            _state.value = _state.value.copy(error = "Name can't be empty")
            return
        }
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { userRepository.updateProfile(name) }
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, saved = true)
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        saving = false,
                        error = t.message ?: "Couldn't save profile",
                    )
                }
        }
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        EditProfileHeader(onBack = onBack)

        Spacer(Modifier.height(8.dp))

        // Section label "Profile"
        Text(
            text = "Profile",
            style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 6.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                NameField(
                    value = state.name,
                    onChange = viewModel::onNameChange,
                )
            }
        }

        state.error?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = interUI(13.sp).copy(color = AppMaple),
                modifier = Modifier.padding(horizontal = 26.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                SaveRow(
                    enabled = state.canSave,
                    saving = state.saving,
                    onClick = viewModel::save,
                )
            }
        }
    }
}

@Composable
private fun EditProfileHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppInk)
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Edit Profile",
                style = frauncesDisplay(20.sp, italic = true).copy(color = AppInk),
                modifier = Modifier.padding(end = 48.dp),
            )
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = interUI(14.sp).copy(color = AppInk),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AppPond),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("edit_profile_name_field"),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = "Name",
                    style = interUI(14.sp).copy(color = AppInk3),
                )
            }
            inner()
        },
    )
}

@Composable
private fun SaveRow(
    enabled: Boolean,
    saving: Boolean,
    onClick: () -> Unit,
) {
    val color = if (enabled) AppPond else AppInk3
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("edit_profile_save_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (saving) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp),
                color = AppPond,
            )
        }
        Text(
            text = if (saving) "Saving…" else "Save",
            style = interUI(14.sp).copy(color = color),
        )
    }
}
