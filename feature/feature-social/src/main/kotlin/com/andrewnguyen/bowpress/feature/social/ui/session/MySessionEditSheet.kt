package com.andrewnguyen.bowpress.feature.social.ui.session

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityPhoto
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationTagPicker
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionListPlacement
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionTextField

/** Max photos per shared session — the API contract §4 cap. */
private const val MAX_PHOTOS = FriendSessionDetailViewModel.MAX_PHOTOS

/** Max description length — the server (migration 0039) slices to this. */
private const val MAX_DESCRIPTION = 1000

/**
 * Owner edit sheet for a shared session (Social Feed V2 §3 / §4).
 *
 * Edits the session **title**, sets/clears the **location** tag, and manages
 * the **multi-photo gallery** — photos add via the system photo picker
 * ([ActivityResultContracts.PickMultipleVisualMedia]) and remove by tapping a
 * tile. Title + location are committed together via [onSave]; photo add/remove
 * are applied immediately ([onAddPhotos] / [onRemovePhoto]) so the gallery is
 * always live and the gallery row reflects the server state.
 */
@Composable
fun MySessionEditSheet(
    sharedSessionId: String,
    initialTitle: String,
    initialDescription: String,
    initialLocation: SessionLocation?,
    photos: List<ActivityPhoto>,
    photoLoader: SessionPhotoLoader,
    isSaving: Boolean,
    onSearchHandles: suspend (String) -> List<HandleSuggestion>,
    onSave: (title: String, description: String, location: SessionLocation?) -> Unit,
    onAddPhotos: (List<android.net.Uri>) -> Unit,
    onRemovePhoto: (ActivityPhoto) -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean = false,
    // Optional — a caller without an owning context omits it; without it
    // the Delete affordance is hidden entirely so a non-owner sheet (if
    // one ever exists) can't even render the button.
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var location by remember { mutableStateOf(initialLocation) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // How many more photos the §4 cap allows. Bounds the picker so the archer
    // can't pick more than will fit; PickMultipleVisualMedia needs maxItems ≥ 2.
    val remainingSlots = (MAX_PHOTOS - photos.size).coerceAtLeast(0)
    // System photo picker — multi-select, images only. The contract is rebuilt
    // when the remaining capacity changes (the launcher's maxItems is fixed at
    // construction), so a later pick is always bounded to what still fits. The
    // ViewModel additionally trims the batch defensively.
    val photoPicker = rememberLauncherForActivityResult(
        remember(remainingSlots) {
            ActivityResultContracts.PickMultipleVisualMedia(
                maxItems = remainingSlots.coerceAtLeast(2),
            )
        },
    ) { uris ->
        if (uris.isNotEmpty()) onAddPhotos(uris)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppPaper),
        ) {
            // Header — Cancel / Save.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cancel",
                    style = interUI(13.sp, FontWeight.Medium),
                    color = AppInk3,
                    modifier = Modifier.clickable(
                        enabled = !isSaving && !isDeleting, onClick = onDismiss,
                    ),
                )
                Text(
                    text = "Edit session",
                    style = frauncesDisplay(16.sp),
                    color = AppInk,
                )
                val canSave = !isSaving && !isDeleting
                Text(
                    text = "Save",
                    style = interUI(13.sp, FontWeight.SemiBold),
                    color = if (canSave) AppPondDk else AppInk3,
                    modifier = Modifier
                        .clickable(enabled = canSave) { onSave(title, description, location) }
                        .testTag(TestTags.MySessionEditSave),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppLine))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // ── Title ──────────────────────────────────────────────────
                FieldLabel("SESSION NAME")
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = "Name this session",
                            style = frauncesDisplay(15.sp),
                            color = AppInk3,
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = frauncesDisplay(15.sp).copy(color = AppInk),
                        cursorBrush = SolidColor(AppPondDk),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.MySessionEditTitleField),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Clearing the name falls back to a generic headline.",
                    style = frauncesDisplay(11.5.sp),
                    color = AppInk3,
                )

                Spacer(Modifier.height(20.dp))

                // ── Description (migration 0039) ────────────────────────────
                FieldLabel("DESCRIPTION")
                Spacer(Modifier.height(8.dp))
                MentionTextField(
                    value = description,
                    onValueChange = {
                        // Live cap so the archer never silently loses text to
                        // the server's 1000-char slice.
                        description = if (it.length > MAX_DESCRIPTION) it.take(MAX_DESCRIPTION) else it
                    },
                    onSearch = onSearchHandles,
                    textStyle = frauncesDisplay(14.sp).copy(color = AppInk),
                    cursorColor = AppPondDk,
                    listPlacement = MentionListPlacement.Below,
                    fieldModifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.MySessionEditDescriptionField),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppLine)
                                .background(AppPaper)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            if (description.isEmpty()) {
                                Text(
                                    text = "Add a description… @mention a friend",
                                    style = frauncesDisplay(14.sp),
                                    color = AppInk3,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your caption on the feed. Type @ to mention a friend.",
                    style = frauncesDisplay(11.5.sp),
                    color = AppInk3,
                )

                Spacer(Modifier.height(20.dp))

                // ── Location ───────────────────────────────────────────────
                FieldLabel("LOCATION")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .clickable { showLocationPicker = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = location?.name ?: "Tag a location",
                        style = frauncesDisplay(15.sp),
                        color = if (location != null) AppInk else AppInk3,
                        modifier = Modifier.weight(1f),
                    )
                    if (location != null) {
                        Text(
                            text = "CLEAR",
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                            color = AppMaple,
                            modifier = Modifier.clickable { location = null },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Photos ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FieldLabel("PHOTOS")
                    Text(
                        text = "${photos.size} / $MAX_PHOTOS",
                        style = jetbrainsMono(9.5.sp),
                        color = AppInk3,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (photos.isEmpty()) {
                    Text(
                        text = "No photos yet — add up to $MAX_PHOTOS.",
                        style = frauncesDisplay(12.sp),
                        color = AppInk3,
                    )
                } else {
                    // Tap a tile to remove it.
                    DetailPhotoGallery(
                        sharedSessionId = sharedSessionId,
                        photos = photos,
                        loader = photoLoader,
                        onRemovePhoto = onRemovePhoto,
                    )
                }
                Spacer(Modifier.height(10.dp))
                val canAddMore = photos.size < MAX_PHOTOS && !isSaving
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (canAddMore) AppPondDk else AppLine)
                        .background(AppPaper2)
                        .clickable(enabled = canAddMore) {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        }
                        .padding(vertical = 12.dp)
                        .testTag(TestTags.MySessionEditAddPhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (photos.size >= MAX_PHOTOS) "PHOTO LIMIT REACHED" else "+ ADD PHOTOS",
                        style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                        color = if (canAddMore) AppPondDk else AppInk3,
                    )
                }

                if (isSaving) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AppPondDk,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Saving…",
                            style = frauncesDisplay(12.sp),
                            color = AppInk3,
                        )
                    }
                }

                // ── Danger zone — Delete post (Social Feed V2 §3) ───────────
                // Only rendered when the caller supplied an onDelete handler.
                if (onDelete != null) {
                    Spacer(Modifier.height(28.dp))
                    FieldLabel("DANGER ZONE")
                    Spacer(Modifier.height(8.dp))
                    val canDelete = !isDeleting && !isSaving
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppMaple)
                            .clickable(enabled = canDelete) { showDeleteConfirm = true }
                            .padding(vertical = 12.dp)
                            .testTag(TestTags.MySessionEditDeletePost),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AppMaple,
                                )
                                Spacer(Modifier.size(8.dp))
                            }
                            Text(
                                text = if (isDeleting) "DELETING…" else "DELETE POST",
                                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                                color = AppMaple,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Removes the post from your feed and every friend's, " +
                            "club's, and league's feed. The underlying session in the " +
                            "Log tab is kept.",
                        style = frauncesDisplay(11.5.sp),
                        color = AppInk3,
                    )
                }
            }
        }
    }

    // §18 location picker — reused from the share flow.
    if (showLocationPicker) {
        LocationTagPicker(
            initial = location,
            onSave = { picked ->
                location = picked
                showLocationPicker = false
            },
            onRemove = { location = null },
            onDismiss = { showLocationPicker = false },
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this post?") },
            text = {
                Text(
                    "Removes the post from your feed and every friend's, club's, and " +
                        "league's feed. Your underlying session in the Log tab is kept.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = AppMaple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
        color = AppInk3,
    )
}
