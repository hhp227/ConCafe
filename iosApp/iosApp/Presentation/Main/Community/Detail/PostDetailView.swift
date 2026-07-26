//
//  PostDetailView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import SwiftUI
import Combine
import Shared
import UIKit

struct PostDetailView: View {
    let postId: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: PostDetailViewModel

    @State private var eventCancellable: AnyCancellable?

    var body: some View {
        PostDetailContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(String(localized: String.LocalizationValue("post_detail_screen_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                postMenuButton
            }
        }
        .alert(String(localized: String.LocalizationValue("post_detail_delete_title"), table: "Localizable"), isPresented: Binding(
            get: { viewModel.uiState.isDeleteConfirmVisible },
            set: { if !$0 { viewModel.onAction(.dismissDeleteConfirm) } }
        )) {
            Button(String(localized: String.LocalizationValue("community_action_delete"), table: "Localizable"), role: .destructive) { viewModel.onAction(.confirmDelete) }
            Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"), role: .cancel) { viewModel.onAction(.dismissDeleteConfirm) }
        } message: {
            Text(String(localized: String.LocalizationValue("post_detail_delete_message"), table: "Localizable"))
        }
        .onAppear {
            eventCancellable = viewModel.eventPublisher.sink { event in
                handleEvent(event)
            }
        }
        .onDisappear {
            eventCancellable?.cancel()
            eventCancellable = nil
        }
    }

    private func handleEvent(_ event: PostDetailViewEvent) {
        switch event {
        case .navigateBack:
            onNavigationAction(.navigateBack)
        case .navigateToPicture(let imageUrl):
            onNavigationAction(.navigateToPicture(imageUrl: imageUrl))
        case .navigateToPostEdit(let postId):
            onNavigationAction(.navigateToPostEdit(postId: postId))
        }
    }

    private var postMenuButton: some View {
        let isVisible = !viewModel.uiState.isLoading && viewModel.uiState.post != nil

        return Menu {
            if viewModel.uiState.isOwner {
                Button(String(localized: String.LocalizationValue("community_action_edit"), table: "Localizable")) {
                    viewModel.onAction(.clickEdit)
                }
                Button(role: .destructive) {
                    viewModel.onAction(.clickDelete)
                } label: {
                    Label(String(localized: String.LocalizationValue("community_action_delete"), table: "Localizable"), systemImage: "trash")
                }
            } else {
                Button {
                    viewModel.onAction(.clickReport)
                } label: {
                    Label(
                        String(localized: String.LocalizationValue("community_action_report"), table: "Localizable"),
                        systemImage: "exclamationmark.bubble"
                    )
                }
                Button(role: .destructive) {
                    viewModel.onAction(.clickBlock)
                } label: {
                    Label(String(localized: String.LocalizationValue("community_action_block"), table: "Localizable"), systemImage: "person.crop.circle.badge.xmark")
                }
            }
        } label: {
            Image(systemName: "ellipsis.circle")
                .foregroundStyle(.primary)
        }
        .disabled(!isVisible)
        .opacity(isVisible ? 1 : 0)
    }

    init(postId: String, onNavigationAction: @escaping (NavigationAction) -> Void) {
        self.postId = postId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: PostDetailViewModel(postId: postId))
    }
}

private struct PostDetailContentView: View {
    let uiState: PostDetailUiState

    let onAction: (PostDetailAction) -> Void

    @FocusState private var isCommentFocused: Bool

    @State private var localEditText: String = ""

    var body: some View {
        Group {
            if uiState.isLoading || uiState.isDeleting {
                VStack(spacing: 0) {
                    ShimmerCardListSkeleton(itemCount: 1, imageHeight: 180)
                    ShimmerListSkeleton(itemCount: 4, avatarSize: 32)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else if uiState.post == nil {
                Text(String(localized: String.LocalizationValue("post_detail_load_failed"), table: "Localizable"))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                mainContent
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemGroupedBackground))
        .sheet(isPresented: Binding(
            get: { uiState.editingCommentId != nil },
            set: { if !$0 { onAction(.dismissEditComment) } }
        )) {
            editCommentSheet
        }
        .sheet(isPresented: Binding(
            get: { uiState.isReportSheetVisible },
            set: { if !$0 { onAction(.dismissReportSheet) } }
        )) {
            reportSheet
        }
        .onAppear {
            isCommentFocused = false
            dismissKeyboard()
        }
    }

    private var reportSheet: some View {
        let reportTypes = [
            String(localized: String.LocalizationValue("community_report_type_spam"), table: "Localizable"),
            String(localized: String.LocalizationValue("community_report_type_abuse"), table: "Localizable"),
            String(localized: String.LocalizationValue("community_report_type_sexual"), table: "Localizable"),
            String(localized: String.LocalizationValue("community_report_type_privacy"), table: "Localizable"),
            String(localized: String.LocalizationValue("community_report_type_other"), table: "Localizable")
        ]
        return VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("community_report_sheet_title"), table: "Localizable"))
                .font(.headline)
            ForEach(reportTypes, id: \.self) { type in
                Button {
                    onAction(.selectReportType(type))
                } label: {
                    HStack {
                        Image(systemName: uiState.selectedReportType == type ? "largecircle.fill.circle" : "circle")
                        Text(type)
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
            }
            Button {
                onAction(.submitReport)
            } label: {
                if uiState.isSubmittingReport {
                    ProgressView()
                        .tint(.white)
                        .frame(maxWidth: .infinity)
                } else {
                    Text(String(localized: String.LocalizationValue("community_report_submit"), table: "Localizable"))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.vertical, 10)
            .background(ConCafeColors.primary)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .disabled(uiState.selectedReportType == nil || uiState.isSubmittingReport)
        }
        .padding(20)
        .compatMediumSheetDetent()
    }

    private var editCommentSheet: some View {
        VStack(alignment: .leading, spacing: 12) {
            ConCafeFormEditor(
                label: String(localized: String.LocalizationValue("post_detail_comment_edit_title"), table: "Localizable"),
                text: $localEditText,
                placeholder: String(localized: String.LocalizationValue("post_detail_comment_edit_placeholder"), table: "Localizable")
            )
                .onAppear {
                    localEditText = uiState.editCommentText
                }
            HStack {
                Spacer()
                Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable")) {
                    onAction(.dismissEditComment)
                }
                .foregroundStyle(.secondary)
                Button {
                    onAction(.confirmEditComment(content: localEditText))
                } label: {
                    if uiState.isUpdatingComment {
                        ProgressView().tint(.white).scaleEffect(0.8)
                            .frame(width: 40, height: 20)
                    } else {
                        Text(String(localized: String.LocalizationValue("community_action_edit"), table: "Localizable"))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(ConCafeColors.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                }
                .disabled(localEditText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || uiState.isUpdatingComment)
            }
        }
        .padding(20)
        .background(Color(uiColor: .systemGroupedBackground))
        .compatMediumSheetDetent()
        .compatPresentationDragIndicator()
    }

    private func dismissKeyboard() {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .endEditing(true)
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    if let post = uiState.post {
                        PostBodyView(
                            post: post,
                            isLiked: uiState.isLiked,
                            onLike: { onAction(.clickLike) },
                            onImageClick: { url in onAction(.clickImage(url)) }
                        )
                    }
                    commentSection
                }
                .frame(maxWidth: .infinity, alignment: .topLeading)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            commentInputBar
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .bottom) {
            if let message = uiState.errorMessage {
                VStack {
                    Spacer()
                    HStack {
                        Text(message)
                            .font(.subheadline)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) { onAction(.dismissError) }
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(ConCafeColors.textPrimary.opacity(0.9))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .padding(.horizontal, 16)
                    .padding(.bottom, 100)
                }
            }
        }
    }

    private var commentSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            Divider()
                .overlay(ConCafeColors.primaryContainer.opacity(0.4))
                .padding(.horizontal, 16)
            HStack(spacing: 6) {
                Image(systemName: "bubble.left")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(String(format: String(localized: String.LocalizationValue("community_post_comment_count"), table: "Localizable"), locale: Locale.current, "\(uiState.commentCount)"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            if uiState.isLoadingComments {
                ShimmerListSkeleton(itemCount: 3, avatarSize: 32)
            } else {
                if uiState.isLoadingMoreComments {
                    ProgressView()
                        .tint(ConCafeColors.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                } else if uiState.hasMoreComments {
                    Button {
                        onAction(.loadMoreComments)
                    } label: {
                        Text(String(localized: String.LocalizationValue("post_detail_load_more_comments"), table: "Localizable"))
                            .font(.system(size: 13))
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                    .buttonStyle(.plain)
                }
                ForEach(uiState.comments, id: \.id) { comment in
                    CommentItemView(
                        comment: comment,
                        isMine: comment.userId == uiState.currentUserId,
                        onEdit: { onAction(.clickEditComment(commentId: comment.id)) },
                        onDelete: { onAction(.clickDeleteComment(commentId: comment.id)) },
                        onReport: { onAction(.clickReportComment(commentId: comment.id)) },
                        onBlock: { onAction(.clickBlockComment(commentId: comment.id)) }
                    )
                }
            }
            Spacer().frame(height: 16)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    private var commentInputBar: some View {
        VStack(spacing: 0) {
            Divider()
                .overlay(ConCafeColors.primaryContainer.opacity(0.2))
            HStack(spacing: 8) {
                CompatVerticalTextField(placeholder: String(localized: String.LocalizationValue("post_detail_comment_placeholder"), table: "Localizable"), text: Binding(
                    get: { uiState.commentText },
                    set: { onAction(.changeCommentText($0)) }
                ))
                .focused($isCommentFocused)
                .font(.system(size: 14))
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color(uiColor: .secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(ConCafeColors.primaryContainer.opacity(0.6), lineWidth: 1)
                )
                Button {
                    isCommentFocused = false
                    onAction(.clickSendComment)
                } label: {
                    Group {
                        if uiState.isSendingComment {
                            ProgressView().tint(.white).scaleEffect(0.8)
                        } else {
                            Image(systemName: "arrow.up")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(.white)
                        }
                    }
                    .frame(width: 36, height: 36)
                    .background(uiState.canSendComment ? ConCafeColors.primary : ConCafeColors.primaryContainer)
                    .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .disabled(!uiState.canSendComment || uiState.isSendingComment)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color(uiColor: .secondarySystemBackground))
            .compatSafeAreaBottomPadding()
        }
        .frame(maxWidth: .infinity)
    }
}

private struct PostBodyView: View {
    let post: CommunityPost

    let isLiked: Bool

    let onLike: () -> Void

    let onImageClick: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 10) {
                Circle()
                    .fill(LinearGradient(
                        colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(width: 40, height: 40)
                    .overlay {
                        Text(String(post.userNickname.prefix(1).isEmpty ? "?" : post.userNickname.prefix(1)))
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(ConCafeColors.primary)
                    }
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.userNickname.isEmpty ? String(localized: String.LocalizationValue("community_anonymous"), table: "Localizable") : post.userNickname)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                    Text(post.displayDate)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            Text(post.title)
                .font(.title3.weight(.bold))
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .padding(.top, 14)
            if !post.content.isEmpty {
                Text(post.content)
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .lineSpacing(6)
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
            }
            let imageUrls = post.imageUrls as? [String] ?? []

            if !imageUrls.isEmpty {
                VStack(spacing: 8) {
                    ForEach(Array(imageUrls.enumerated()), id: \.offset) { _, imageUrl in
                        AsyncImage(url: URL(string: imageUrl)) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFit()
                            default:
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(ConCafeColors.surfaceTint)
                                    .aspectRatio(16 / 9, contentMode: .fit)
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .onTapGesture { onImageClick(imageUrl) }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)
            }
            HStack(spacing: 20) {
                Button(action: onLike) {
                    HStack(spacing: 4) {
                        Image(systemName: isLiked ? "heart.fill" : "heart")
                            .font(.system(size: 16))
                            .foregroundStyle(isLiked ? ConCafeColors.primary : .secondary)
                        Text(String(format: String(localized: String.LocalizationValue("community_post_like_count"), table: "Localizable"), locale: Locale.current, "\(post.likeCount)"))
                            .font(.system(size: 13))
                            .foregroundStyle(isLiked ? ConCafeColors.primary : .secondary)
                    }
                }
                .buttonStyle(.plain)
                HStack(spacing: 4) {
                    Image(systemName: "bubble.left")
                        .font(.system(size: 15))
                        .foregroundStyle(.secondary)
                    Text(String(format: String(localized: String.LocalizationValue("community_post_comment_count"), table: "Localizable"), locale: Locale.current, "\(post.commentCount)"))
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 16)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(Color(uiColor: .secondarySystemBackground))
    }
}

private struct CommentItemView: View {
    let comment: Comment

    let isMine: Bool

    let onEdit: () -> Void

    let onDelete: () -> Void

    let onReport: () -> Void

    let onBlock: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Circle()
                    .fill(LinearGradient(
                        colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(width: 28, height: 28)
                    .overlay {
                        Text(String(comment.userNickname.prefix(1).isEmpty ? "?" : comment.userNickname.prefix(1)))
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(ConCafeColors.primary)
                    }
                Text(comment.userNickname.isEmpty ? String(localized: String.LocalizationValue("community_anonymous"), table: "Localizable") : comment.userNickname)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                Spacer()
                Text(comment.displayDate)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Menu {
                    if isMine {
                        Button(String(localized: String.LocalizationValue("community_action_edit"), table: "Localizable"), action: onEdit)
                        Button(role: .destructive, action: onDelete) {
                            Label(String(localized: String.LocalizationValue("community_action_delete"), table: "Localizable"), systemImage: "trash")
                        }
                    } else {
                        Button(action: onReport) {
                            Label(
                                String(localized: String.LocalizationValue("community_action_report"), table: "Localizable"),
                                systemImage: "exclamationmark.bubble"
                            )
                        }
                        Button(role: .destructive, action: onBlock) {
                            Label(String(localized: String.LocalizationValue("community_action_block"), table: "Localizable"), systemImage: "person.crop.circle.badge.xmark")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
            }
            Text(comment.content)
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .lineSpacing(4)
                .padding(.leading, 36)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}
