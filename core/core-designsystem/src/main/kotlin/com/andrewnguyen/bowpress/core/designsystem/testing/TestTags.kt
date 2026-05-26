package com.andrewnguyen.bowpress.core.designsystem.testing

/**
 * Canonical testTag string constants. Every interactive element exercised by
 * a flow file (under `flows/`) must be tagged with one of these strings.
 *
 * Constants here MUST stay in sync with the iOS `AccessibilityIdentifiers`
 * file. Any change here is a contract change — update both platforms in the
 * same commit, or e2e flow tests will diverge.
 *
 * Convention:
 *  - snake_case
 *  - feature-prefixed where ambiguous (`paywall_…`, `email_auth_…`)
 *  - dynamic ids use `${prefix}_$id` interpolation at the call site
 *    (e.g. `bow_row_${bow.id}`); the prefix is captured here as
 *    `BowRowPrefix` so tests can compose `bow_row_<uuid>` reliably.
 */
object TestTags {

    // -----------------------------------------------------------------------
    // App scaffold / launch
    // -----------------------------------------------------------------------
    const val HydrationSplash: String = "hydration_splash"
    const val MainTabBar: String = "main_tab_bar"

    // -----------------------------------------------------------------------
    // Auth
    // -----------------------------------------------------------------------
    const val AuthContinueGoogle: String = "auth_continue_google"
    const val AuthContinueEmail: String = "auth_continue_email"

    const val EmailAuthName: String = "email_auth_name"
    const val EmailAuthEmail: String = "email_auth_email"
    const val EmailAuthPassword: String = "email_auth_password"
    const val EmailAuthConfirmPassword: String = "email_auth_confirm_password"
    const val EmailAuthSubmit: String = "email_auth_submit"
    const val EmailAuthError: String = "email_auth_error"

    const val VerifyCode: String = "verify_code"
    const val VerifySubmit: String = "verify_submit"
    const val VerifyResend: String = "verify_resend"
    const val VerifyError: String = "verify_error"

    // -----------------------------------------------------------------------
    // Equipment
    // -----------------------------------------------------------------------
    const val AddBowsButton: String = "add_bows_button"
    const val AddArrowsButton: String = "add_arrows_button"
    const val BowNameField: String = "bow_name_field"
    const val ArrowLabelField: String = "arrow_label_field"
    const val BowConfigLabelField: String = "bow_config_label_field"
    const val SaveConfigButton: String = "save_config_button"
    const val EditLatestButton: String = "edit_latest_button"
    /** Concat with bow id: `${BowRowPrefix}${bow.id}` */
    const val BowRowPrefix: String = "bow_row_"
    /** Concat with config id: `${HistoryRowPrefix}${config.id}` */
    const val HistoryRowPrefix: String = "history_row_"
    /** Concat with lowercase bow type name: `${BowTypeRowPrefix}compound` */
    const val BowTypeRowPrefix: String = "bow_type_"

    // -----------------------------------------------------------------------
    // Session
    // -----------------------------------------------------------------------
    const val SessionStartButton: String = "session_start_button"
    const val DistancePicker: String = "distance_picker"
    const val TargetFacePicker: String = "target_face_picker"
    const val TargetPlot: String = "target_plot"
    const val SessionNameField: String = "session_name_field"
    /** Concat with distance raw value: `${SessionDistanceRowPrefix}20yd` */
    const val SessionDistanceRowPrefix: String = "session_distance_row_"
    /** Concat with face raw value: `${SessionFaceRowPrefix}tenRing` */
    const val SessionFaceRowPrefix: String = "session_face_row_"

    // -----------------------------------------------------------------------
    // Analytics
    // -----------------------------------------------------------------------
    const val AnalyticsDashboardRoot: String = "analytics_dashboard_root"
    const val SuggestionsDashboardRoot: String = "suggestions_dashboard_root"
    const val SuggestionsEmptyState: String = "suggestions_dashboard_empty_state"
    const val SuggestionsUnreadBadge: String = "suggestions_dashboard_unread_badge"
    const val SuggestionCard: String = "suggestions_dashboard_suggestion_card"
    const val SuggestionApplyButton: String = "suggestion_apply_button"
    /** Concat with session id: `${SessionRowPrefix}${session.id}` */
    const val SessionRowPrefix: String = "session_row_"
    const val EditSessionButton: String = "edit_session_button"
    const val EditSessionNotes: String = "edit_session_notes"
    const val EditSessionTags: String = "edit_session_tags"

    // -----------------------------------------------------------------------
    // Subscription / paywall
    // -----------------------------------------------------------------------
    const val UpgradeBanner: String = "upgrade_banner"
    const val PaywallSheet: String = "paywall_sheet"
    const val PaywallMonthlyButton: String = "paywall_monthly_button"
    const val PaywallAnnualButton: String = "paywall_annual_button"
    const val PaywallRestoreButton: String = "paywall_restore_button"
    const val PaywallRedeemButton: String = "paywall_redeem_button"
    const val PaywallTermsButton: String = "paywall_terms_button"
    const val PaywallPrivacyButton: String = "paywall_privacy_button"
    const val PaywallAlreadySubscribedNote: String = "paywall_already_subscribed_note"

    // -----------------------------------------------------------------------
    // Settings
    // -----------------------------------------------------------------------
    const val SettingsSubscription: String = "settings_subscription"
    const val SettingsSignOut: String = "settings_sign_out"
    const val SettingsChangePassword: String = "settings_change_password"
    const val SettingsDeleteAccount: String = "settings_delete_account"
    const val SettingsNotificationsToggle: String = "settings_notifications_toggle"
    const val UnitSystemToggle: String = "unit_system_toggle"

    const val EditProfileNameField: String = "edit_profile_name_field"
    const val EditProfileSaveButton: String = "edit_profile_save_button"

    const val ChangePasswordCurrent: String = "change_password_current"
    const val ChangePasswordNew: String = "change_password_new"
    const val ChangePasswordConfirm: String = "change_password_confirm"
    const val ChangePasswordSubmit: String = "change_password_submit"

    const val DeleteAccountPassword: String = "delete_account_password"
    const val DeleteAccountConfirm: String = "delete_account_confirm"

    // -----------------------------------------------------------------------
    // Social
    // -----------------------------------------------------------------------
    const val SocialFeedRoot: String = "social_feed_root"
    /** New-user empty state (no friends AND no clubs AND no leagues). */
    const val SocialFeedNewUserEmpty: String = "social_feed_new_user_empty"
    /** Connected-but-quiet empty state (has connections but 72 h feed is empty). */
    const val SocialFeedQuietEmpty: String = "social_feed_quiet_empty"
    /** "Add a friend" CTA button inside the new-user empty state. */
    const val SocialEmptyAddFriend: String = "social_empty_add_friend"
    /** "Find a club" CTA button inside the new-user empty state. */
    const val SocialEmptyFindClub: String = "social_empty_find_club"
    const val SocialYouRoot: String = "social_you_root"
    const val SocialFriendsRoot: String = "social_friends_root"
    const val SocialFriendSearchField: String = "social_friend_search_field"
    const val SocialClubsRoot: String = "social_clubs_root"
    const val SocialLeaguesRoot: String = "social_leagues_root"
    const val SocialLeagueComposerName: String = "social_league_composer_name"
    const val SocialPrivacyRoot: String = "social_privacy_root"
    const val SocialTabBadge: String = "social_tab_badge"
    const val SocialClubInvitesSection: String = "social_club_invites_section"
    const val SocialLeagueInvitesSection: String = "social_league_invites_section"
    const val SocialMuteBlockAction: String = "social_mute_block_action"
    const val SocialBlocksRoot: String = "social_blocks_root"
    const val SocialSessionDetailRoot: String = "social_session_detail_root"
    const val SessionDetailDescription: String = "session_detail_description"
    const val SocialSessionTarget: String = "social_session_target"
    // §18 — activity-feed previews + location tagging.
    const val FeedRowLocationTag: String = "feed_row_location_tag"
    const val FeedRowPreview: String = "feed_row_preview"
    /** The per-end scorecard inside a range feed row's target preview. */
    const val FeedRowScorecard: String = "feed_row_scorecard"
    const val LocationMap: String = "location_map"
    const val LocationMapClose: String = "location_map_close"
    const val LocationMapRecenter: String = "location_map_recenter"
    const val LocationPickerMap: String = "location_picker_map"
    const val LocationPickerNameField: String = "location_picker_name_field"
    const val SessionNotesLocationRow: String = "session_notes_location_row"
    // Social Feed V2 — own-post edit + multi-photo gallery.
    const val MySessionEditButton: String = "my_session_edit_button"
    const val MySessionEditTitleField: String = "my_session_edit_title_field"
    const val MySessionEditDescriptionField: String = "my_session_edit_description_field"
    const val MySessionEditSave: String = "my_session_edit_save"
    const val MySessionEditAddPhoto: String = "my_session_edit_add_photo"
    const val MySessionEditDeletePost: String = "my_session_edit_delete_post"
    const val FeedRowPhotoGallery: String = "feed_row_photo_gallery"
    const val SessionDetailPhotoGallery: String = "session_detail_photo_gallery"
    // Section 4 — the count-flexed photo strip on the activity card.
    const val FeedRowPhotoStrip: String = "feed_row_photo_strip"
    // Social Feed V2 Part 2 — likes & comments.
    const val FeedRowLikeButton: String = "feed_row_like_button"
    const val FeedRowCommentButton: String = "feed_row_comment_button"
    // iOS parity (A5) — 3-dot overflow on the activity-card header.
    // Surfaces Edit / Delete for own Log-tab rows; Feed leaves it absent.
    const val FeedRowOverflow: String = "feed_row_overflow"
    const val CommentsRoot: String = "comments_root"
    const val CommentsComposeField: String = "comments_compose_field"
    const val CommentsSendButton: String = "comments_send_button"
    // Social Feed V2 Part 3 — comment threads & kudos.
    const val FeedRowKudos: String = "feed_row_kudos"
    const val CommentsSortRecent: String = "comments_sort_recent"
    const val CommentsSortTop: String = "comments_sort_top"
    const val CommentRowLikeButton: String = "comment_row_like_button"
    const val CommentRowReplyButton: String = "comment_row_reply_button"

    // Mentions — the @-autocomplete suggestion list (mentions contract §3.1).
    const val MentionSuggestionList: String = "mention_suggestion_list"
    /** Concat with the suggested handle: `${MentionSuggestionRowPrefix}sara.lin` */
    const val MentionSuggestionRowPrefix: String = "mention_suggestion_row_"
}
