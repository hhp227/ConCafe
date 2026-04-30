//
//  PostDetailView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import SwiftUI
import Combine
import Shared

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
        .navigationTitle("게시글")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                postMenuButton
            }
        }
        .alert("게시글 삭제", isPresented: Binding(
            get: { viewModel.uiState.isDeleteConfirmVisible },
            set: { if !$0 { viewModel.onAction(.dismissDeleteConfirm) } }
        )) {
            Button("삭제", role: .destructive) { viewModel.onAction(.confirmDelete) }
            Button("취소", role: .cancel) { viewModel.onAction(.dismissDeleteConfirm) }
        } message: {
            Text("이 게시글을 삭제하시겠습니까?")
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
                Button("수정") {
                    viewModel.onAction(.clickEdit)
                }
                Button(role: .destructive) {
                    viewModel.onAction(.clickDelete)
                } label: {
                    Label("삭제", systemImage: "trash")
                }
            } else {
                Button {
                    viewModel.onAction(.clickReport)
                } label: {
                    Label("신고하기", systemImage: "exclamationmark.bubble")
                }
            }
        } label: {
            Image(systemName: "ellipsis.circle")
                .foregroundStyle(Color(hex: "2B2330"))
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
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.post == nil {
                Text("게시글을 불러오지 못했습니다.")
                    .foregroundStyle(Color(hex: "8C7E87"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                mainContent
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(hex: "F8F5F6"))
        .sheet(isPresented: Binding(
            get: { uiState.editingCommentId != nil },
            set: { if !$0 { onAction(.dismissEditComment) } }
        )) {
            editCommentSheet
        }
    }

    private var editCommentSheet: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("댓글 수정")
                .font(.headline)
                .foregroundStyle(Color(hex: "2B2330"))
            TextEditor(text: $localEditText)
                .frame(minHeight: 80, maxHeight: 160)
                .padding(8)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: "FFD1DC"), lineWidth: 1)
                )
                .font(.system(size: 14))
                .onAppear {
                    localEditText = uiState.editCommentText
                }
            HStack {
                Spacer()
                Button("취소") {
                    onAction(.dismissEditComment)
                }
                .foregroundStyle(Color(hex: "8C7E87"))
                Button {
                    onAction(.confirmEditComment(content: localEditText))
                } label: {
                    if uiState.isUpdatingComment {
                        ProgressView().tint(.white).scaleEffect(0.8)
                            .frame(width: 40, height: 20)
                    } else {
                        Text("수정")
                            .foregroundStyle(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color(hex: "EF6797"))
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                }
                .disabled(localEditText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || uiState.isUpdatingComment)
            }
        }
        .padding(20)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
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
                        Button("닫기") { onAction(.dismissError) }
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(Color(hex: "2B2330").opacity(0.9))
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
                .overlay(Color(hex: "FFD1DC").opacity(0.4))
                .padding(.horizontal, 16)
            HStack(spacing: 6) {
                Image(systemName: "bubble.left")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "B1A3AC"))
                Text("댓글 \(uiState.commentCount)")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "665A63"))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            if uiState.isLoadingComments {
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
            } else {
                if uiState.isLoadingMoreComments {
                    ProgressView()
                        .tint(Color(hex: "EF6797"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                } else if uiState.hasMoreComments {
                    Button {
                        onAction(.loadMoreComments)
                    } label: {
                        Text("이전 댓글 더보기")
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "8C7E87"))
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
                        onReport: { onAction(.clickReportComment(commentId: comment.id)) }
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
                .overlay(Color(hex: "FFD1DC").opacity(0.2))
            HStack(spacing: 8) {
                CompatVerticalTextField(placeholder: "댓글을 입력하세요", text: Binding(
                    get: { uiState.commentText },
                    set: { onAction(.changeCommentText($0)) }
                ))
                .focused($isCommentFocused)
                .font(.system(size: 14))
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.6), lineWidth: 1)
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
                    .background(uiState.canSendComment ? Color(hex: "EF6797") : Color(hex: "FFD1DC"))
                    .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .disabled(!uiState.canSendComment || uiState.isSendingComment)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
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
                        colors: [Color(hex: "FFE3EC"), Color(hex: "F8C5D7")],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(width: 40, height: 40)
                    .overlay {
                        Text(String(post.userNickname.prefix(1).isEmpty ? "?" : post.userNickname.prefix(1)))
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.userNickname.isEmpty ? "익명" : post.userNickname)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color(hex: "2B2330"))
                    Text(post.displayDate)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "B1A3AC"))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            Text(post.title)
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
                .padding(.horizontal, 16)
                .padding(.top, 14)
            if !post.content.isEmpty {
                Text(post.content)
                    .font(.system(size: 15))
                    .foregroundStyle(Color(hex: "665A63"))
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
                                    .fill(Color(hex: "FFE3EC"))
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
                            .foregroundStyle(isLiked ? Color(hex: "EF6797") : Color(hex: "B1A3AC"))
                        Text("좋아요 \(post.likeCount)")
                            .font(.system(size: 13))
                            .foregroundStyle(isLiked ? Color(hex: "EF6797") : Color(hex: "8C7E87"))
                    }
                }
                .buttonStyle(.plain)
                HStack(spacing: 4) {
                    Image(systemName: "bubble.left")
                        .font(.system(size: 15))
                        .foregroundStyle(Color(hex: "B1A3AC"))
                    Text("댓글 \(post.commentCount)")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "8C7E87"))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 16)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(Color.white)
    }
}

private struct CommentItemView: View {
    let comment: Comment

    let isMine: Bool

    let onEdit: () -> Void

    let onDelete: () -> Void

    let onReport: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Circle()
                    .fill(LinearGradient(
                        colors: [Color(hex: "FFE3EC"), Color(hex: "F8C5D7")],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(width: 28, height: 28)
                    .overlay {
                        Text(String(comment.userNickname.prefix(1).isEmpty ? "?" : comment.userNickname.prefix(1)))
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                Text(comment.userNickname.isEmpty ? "익명" : comment.userNickname)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "2B2330"))
                Spacer()
                Text(comment.displayDate)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "B1A3AC"))
                Menu {
                    if isMine {
                        Button("수정", action: onEdit)
                        Button(role: .destructive, action: onDelete) {
                            Label("삭제", systemImage: "trash")
                        }
                    } else {
                        Button(action: onReport) {
                            Label("신고하기", systemImage: "exclamationmark.bubble")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: "B1A3AC"))
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
            }
            Text(comment.content)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: "665A63"))
                .lineSpacing(4)
                .padding(.leading, 36)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}
