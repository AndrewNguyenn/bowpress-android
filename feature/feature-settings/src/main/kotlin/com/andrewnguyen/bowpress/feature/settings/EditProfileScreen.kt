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
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.bp.BPCard
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.network.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Edit Profile state — matches the iOS surface (display name + @handle on
 * the social profile). The auth-user `name` shown on AccountScreen is
 * separate and not touched here, same as iOS.
 *
 * Handle / display-name renames are safe across friend, club, and league
 * relationships: all of those FK on `users(id)` (stable UUID), and every
 * read surface hydrates handle + displayName from the social profile on
 * read — no fan-out sync step is needed.
 */
data class EditProfileUiState(
    val loaded: Boolean = false,
    val initialDisplayName: String = "",
    val displayName: String = "",
    val initialHandle: String = "",
    val handle: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val displayNameTrimmed: String get() = displayName.trim()
    val handleTrimmed: String get() = handle.trim()

    val canSave: Boolean
        get() {
            if (!loaded || saving) return false
            if (displayNameTrimmed.isEmpty()) return false
            if (!isValidHandle(handleTrimmed)) return false
            return displayNameTrimmed != initialDisplayName.trim() ||
                handleTrimmed != initialHandle.trim()
        }
}

/**
 * Mirrors the server's `^[a-z0-9._]{3,30}$` rule (matches iOS
 * `EditProfileView.sanitizeHandle`): forces lowercase, drops disallowed
 * characters, and caps the length as the archer types.
 */
internal fun sanitizeHandle(raw: String): String {
    val allowed = "abcdefghijklmnopqrstuvwxyz0123456789._".toSet()
    return buildString {
        for (ch in raw.lowercase()) {
            if (ch in allowed) append(ch)
            if (length >= MAX_HANDLE_LEN) break
        }
    }
}

internal fun isValidHandle(handle: String): Boolean {
    if (handle.length !in MIN_HANDLE_LEN..MAX_HANDLE_LEN) return false
    if (handle.startsWith('.') || handle.endsWith('.')) return false
    return true
}

private const val MIN_HANDLE_LEN = 3
private const val MAX_HANDLE_LEN = 30

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _state: MutableStateFlow<EditProfileUiState> = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Paint instantly from cache — matches iOS, which reads
            // LocalStore before awaiting the API. Save stays disabled until
            // either a cached or fresh profile lands.
            socialRepository.getCachedMyProfile()?.let { cached -> seedFromProfile(cached) }

            // Then refresh against the server so the baseline used for the
            // diff-vs-send logic in [save] reflects the authoritative state.
            // On failure we keep the cached values — iOS does the same with
            // `try? await api.socialMe()`.
            runCatching { socialRepository.refreshMyProfile() }
                .onSuccess { fresh -> rebaseFromFresh(fresh) }
                .onFailure { err ->
                    if (!_state.value.loaded) {
                        _state.value = _state.value.copy(
                            loaded = true,
                            error = err.message ?: "Couldn't load profile",
                        )
                    }
                }
        }
    }

    private fun seedFromProfile(profile: SocialProfile) {
        _state.value = _state.value.copy(
            loaded = true,
            initialDisplayName = profile.displayName,
            displayName = profile.displayName,
            initialHandle = profile.handle,
            handle = profile.handle,
        )
    }

    /**
     * Rebase the diff-vs-send baseline onto a freshly-fetched profile, and
     * only update the visible fields when the archer hasn't already started
     * editing — a slow refresh must not clobber an in-flight edit.
     */
    private fun rebaseFromFresh(profile: SocialProfile) {
        val current = _state.value
        val displayName =
            if (current.displayName == current.initialDisplayName) profile.displayName
            else current.displayName
        val handle =
            if (current.handle == current.initialHandle) profile.handle
            else current.handle
        _state.value = current.copy(
            loaded = true,
            initialDisplayName = profile.displayName,
            initialHandle = profile.handle,
            displayName = displayName,
            handle = handle,
        )
    }

    /** Clear the one-shot `saved` event before navigating, so a recomposition can't re-fire `onBack`. */
    fun onSavedConsumed() {
        _state.value = _state.value.copy(saved = false)
    }

    fun onDisplayNameChange(value: String) {
        _state.value = _state.value.copy(displayName = value)
    }

    fun onHandleChange(value: String) {
        // Sanitize as the archer types — mirrors iOS so an invalid character
        // never lands in state and the field can't go out of sync with what
        // the server will accept.
        _state.value = _state.value.copy(handle = sanitizeHandle(value))
    }

    fun save() {
        val current = _state.value
        val displayName = current.displayNameTrimmed
        val handle = current.handleTrimmed
        if (displayName.isEmpty()) {
            _state.value = current.copy(error = "Display name can't be empty")
            return
        }
        if (!isValidHandle(handle)) {
            _state.value = current.copy(error = HANDLE_FORMAT_MESSAGE)
            return
        }

        // Only send fields that actually changed — matches iOS and lets the
        // server skip the handle-uniqueness check when only the display
        // name moved.
        val handleArg = handle.takeIf { it != current.initialHandle.trim() }
        val displayNameArg = displayName.takeIf { it != current.initialDisplayName.trim() }

        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                socialRepository.updateMyProfile(
                    handle = handleArg,
                    displayName = displayNameArg,
                )
            }
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, saved = true)
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        saving = false,
                        error = friendlyMessage(t),
                    )
                }
        }
    }

    private fun friendlyMessage(t: Throwable): String {
        if (t is ApiException) {
            when (t.status) {
                409 -> return "That handle is already taken — try another."
                422 -> return HANDLE_FORMAT_MESSAGE
            }
        }
        return t.message ?: "Couldn't save profile"
    }

    private companion object {
        const val HANDLE_FORMAT_MESSAGE =
            "Handle must be 3–30 characters: lowercase letters, numbers, dots and underscores."
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSavedConsumed()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        EditProfileHeader(onBack = onBack)

        Spacer(Modifier.height(8.dp))

        SectionLabel("Display name")
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                DisplayNameField(
                    value = state.displayName,
                    onChange = viewModel::onDisplayNameChange,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel("Handle")
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                HandleField(
                    value = state.handle,
                    onChange = viewModel::onHandleChange,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "3–30 characters: lowercase letters, numbers, dots and underscores. " +
                    "This is how friends find and tag you.",
                style = interUI(12.sp).copy(color = AppInk3),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
        modifier = Modifier.padding(horizontal = 26.dp, vertical = 6.dp),
    )
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
private fun DisplayNameField(
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
            .testTag(TestTags.EditProfileNameField),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = "Display name",
                    style = interUI(14.sp).copy(color = AppInk3),
                )
            }
            inner()
        },
    )
}

@Composable
private fun HandleField(
    value: String,
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "@",
            style = interUI(14.sp).copy(color = AppInk3),
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = interUI(14.sp).copy(color = AppInk),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppPond),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.EditProfileHandleField),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "handle",
                        style = interUI(14.sp).copy(color = AppInk3),
                    )
                }
                inner()
            },
        )
    }
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
            .testTag(TestTags.EditProfileSaveButton),
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
