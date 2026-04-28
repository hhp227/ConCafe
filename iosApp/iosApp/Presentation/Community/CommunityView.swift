//
//  CommunityView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import SwiftUI
import Shared

struct CommunityView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = CommunityViewModel()

    var body: some View {
        CommunityContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToPostEdit:
                onNavigationAction(.navigateToPostEdit(postId: nil))
            case .navigateToPost(let postId):
                onNavigationAction(.navigateToPostDetail(postId: postId))
            }
        }
    }
}

private struct CommunityContentView: View {
    let uiState: CommunityUiState

    let onAction: (CommunityAction) -> Void

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Group {
                if uiState.isLoading {
                    VStack {
                        Spacer()
                        ProgressView().tint(Color(hex: "EF6797"))
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else if uiState.posts.isEmpty {
                    VStack {
                        Spacer()
                        Text("아직 게시글이 없습니다.\n첫 번째 게시글을 남겨보세요!")
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "8C7E87"))
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(uiState.posts, id: \.id) { post in
                                CommunityPostCard(post: post) {
                                    onAction(.clickPost(postId: post.id))
                                }
                            }
                            if uiState.isLoadingMore {
                                ProgressView()
                                    .tint(Color(hex: "EF6797"))
                                    .padding(.vertical, 12)
                            } else if uiState.hasNext {
                                Color.clear
                                    .frame(height: 1)
                                    .onAppear { onAction(.loadMore) }
                            }
                            Spacer().frame(height: 80)
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 12)
                    }
                }
            }
            Button {
                onAction(.clickWritePost)
            } label: {
                Image(systemName: "plus")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(Color(hex: "EF6797"))
                    .clipShape(Circle())
                    .shadow(color: Color(hex: "EF6797").opacity(0.4), radius: 8, x: 0, y: 4)
            }
            .buttonStyle(.plain)
            .padding(.trailing, 20)
            .padding(.bottom, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(hex: "F8F5F6"))
        .navigationTitle("커뮤니티")
        .navigationBarTitleDisplayMode(.large)
    }
}

private struct CommunityPostCard: View {
    let post: CommunityPost

    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    HStack(spacing: 8) {
                        Circle()
                            .fill(LinearGradient(
                                colors: [Color(hex: "FFE3EC"), Color(hex: "F8C5D7")],
                                startPoint: .topLeading, endPoint: .bottomTrailing
                            ))
                            .frame(width: 32, height: 32)
                            .overlay {
                                Text(String(post.userNickname.prefix(1).isEmpty ? "?" : post.userNickname.prefix(1)))
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(Color(hex: "EF6797"))
                            }
                        Text(post.userNickname.isEmpty ? "익명" : post.userNickname)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Color(hex: "665A63"))
                    }
                    Spacer()
                    Text(post.displayDate)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "B1A3AC"))
                }
                .padding(.bottom, 10)
                Text(post.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "2B2330"))
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if !post.content.isEmpty {
                    Text(post.content)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "665A63"))
                        .lineLimit(3)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 4)
                }
                let imageUrls = post.imageUrls as? [String] ?? []

                if !imageUrls.isEmpty {
                    HStack(spacing: 6) {
                        ForEach(Array(imageUrls.prefix(3).enumerated()), id: \.offset) { index, imageUrl in
                            let isOverflow = index == 2 && imageUrls.count > 3
                            ZStack {
                                AsyncImage(url: URL(string: imageUrl)) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image.resizable().scaledToFill()
                                    default:
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .fill(Color(hex: "FFE3EC"))
                                    }
                                }
                                .frame(width: 72, height: 72)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                if isOverflow {
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .fill(Color.black.opacity(0.42))
                                    Text("+\(imageUrls.count - 3)")
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(.white)
                                }
                            }
                            .frame(width: 72, height: 72)
                        }
                    }
                    .padding(.top, 10)
                }
                Divider()
                    .overlay(Color(hex: "FFD1DC").opacity(0.3))
                    .padding(.vertical, 10)
                HStack(spacing: 14) {
                    HStack(spacing: 4) {
                        Image(systemName: "heart")
                            .font(.caption2)
                            .foregroundStyle(Color(hex: "B1A3AC"))
                        Text("좋아요 \(post.likeCount)")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "8C7E87"))
                    }
                    HStack(spacing: 4) {
                        Image(systemName: "bubble.left")
                            .font(.caption2)
                            .foregroundStyle(Color(hex: "B1A3AC"))
                        Text("댓글 \(post.commentCount)")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "8C7E87"))
                    }
                }
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationView {
        CommunityView(onNavigationAction: { _ in })
    }
}
