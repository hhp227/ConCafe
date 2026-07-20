//
//  CommunityView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import SwiftUI
import Shared
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

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
                        ProgressView().tint(ConCafeColors.primary)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else if uiState.posts.isEmpty {
                    VStack {
                        Spacer()
                        Text(String(localized: String.LocalizationValue("community_empty"), table: "Localizable"))
                            .font(.subheadline)
                            .foregroundStyle(ConCafeColors.textMuted)
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    GeometryReader { geometry in
                        let columnCount = communityGridColumnCount(for: geometry.size.width)

                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(Array(stride(from: 0, to: uiState.posts.count, by: columnCount)), id: \.self) { rowStartIndex in
                                    let rowEndIndex = min(rowStartIndex + columnCount, uiState.posts.count)

                                    HStack(spacing: 12) {
                                        ForEach(rowStartIndex..<rowEndIndex, id: \.self) { index in
                                            let post = uiState.posts[index]

                                            CommunityPostCard(post: post) {
                                                onAction(.clickPost(postId: post.id))
                                            }
                                            .frame(maxWidth: .infinity)
                                        }
                                        ForEach(0..<(columnCount - (rowEndIndex - rowStartIndex)), id: \.self) { _ in
                                            Spacer()
                                                .frame(maxWidth: .infinity)
                                        }
                                    }
                                    .frame(maxWidth: .infinity)

                                    if let nativeAd = nativeAd(in: rowStartIndex..<rowEndIndex) {
                                        CommunityNativeAdCard(nativeAdHandle: nativeAd)
                                    }
                                }
                                if uiState.isLoadingMore {
                                    ProgressView()
                                        .tint(ConCafeColors.primary)
                                        .padding(.vertical, 12)
                                } else if uiState.hasNext {
                                    Color.clear
                                        .frame(height: 1)
                                        .onAppear { onAction(.loadMore) }
                                }
                                Spacer().frame(height: 80)
                            }
                        }
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
                    .background(ConCafeColors.primary)
                    .clipShape(Circle())
                    .shadow(color: ConCafeColors.primary.opacity(0.4), radius: 8, x: 0, y: 4)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: String.LocalizationValue("community_write_post"), table: "Localizable"))
            .padding(.trailing, 20)
            .padding(.bottom, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(ConCafeColors.surfaceVariant)
        .navigationTitle(String(localized: String.LocalizationValue("community_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.large)
    }

    private func nativeAd(for postIndex: Int) -> (any NativeAdHandle)? {
        let pageIndex = postIndex / Self.pageSize
        let indexInPage = postIndex % Self.pageSize
        guard indexInPage == Self.adInsertAfterIndex else { return nil }
        return uiState.nativeAds[Self.nativeAdSlotStart + Int32(pageIndex)]
    }

    private func nativeAd(in postIndexRange: Range<Int>) -> (any NativeAdHandle)? {
        for index in postIndexRange {
            if let nativeAd = nativeAd(for: index) {
                return nativeAd
            }
        }
        return nil
    }

    private func communityGridColumnCount(for width: CGFloat) -> Int {
        width >= 700 ? 2 : 1
    }

    private static let pageSize = 20
    private static let adInsertAfterIndex = 5
    private static let nativeAdSlotStart: Int32 = 100
}

private struct CommunityNativeAdCard: View {
    let nativeAdHandle: any NativeAdHandle

    var body: some View {
        Group {
            #if canImport(GoogleMobileAds)
            if let nativeAd = (nativeAdHandle as? IOSNativeAdHandle)?.nativeAd {
                CommunityNativeAdRepresentable(nativeAd: nativeAd)
            } else {
                placeholder
            }
            #else
            placeholder
            #endif
        }
        .frame(maxWidth: .infinity)
        .frame(height: 146)
        .background(Color.white)
    }

    private var placeholder: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HStack(spacing: 8) {
                    Circle()
                        .fill(LinearGradient(
                            colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        ))
                        .frame(width: 32, height: 32)
                        .overlay {
                            Text("AD")
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(ConCafeColors.primary)
                        }
                    Text("ConCafe")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(ConCafeColors.textSecondary)
                }
                Spacer()
                Text("광고")
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.outlineStrong)
            }
            .padding(.bottom, 10)
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(ConCafeColors.primaryContainer)
                .frame(height: 17)
                .padding(.trailing, 70)
            RoundedRectangle(cornerRadius: 5, style: .continuous)
                .fill(ConCafeColors.surfaceTint)
                .frame(height: 13)
                .padding(.top, 6)
                .padding(.trailing, 28)
            Divider()
                .overlay(ConCafeColors.primaryContainer.opacity(0.3))
                .padding(.vertical, 10)
            HStack {
                Text("자세히")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ConCafeColors.textPrimary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(ConCafeColors.primaryContainer)
                    .clipShape(Capsule())
                Spacer()
            }
        }
        .padding(16)
    }
}

#if canImport(GoogleMobileAds)
private struct CommunityNativeAdRepresentable: UIViewRepresentable {
    let nativeAd: GADNativeAd

    func makeUIView(context: Context) -> GADNativeAdView {
        let nativeAdView = GADNativeAdView()
        let container = UIStackView()
        let topRow = UIStackView()
        let identityRow = UIStackView()
        let avatarLabel = UILabel()
        let advertiserLabel = UILabel()
        let adBadgeLabel = UILabel()
        let headlineLabel = UILabel()
        let bodyLabel = UILabel()
        let divider = UIView()
        let bottomRow = UIStackView()
        let callToActionButton = UIButton(type: .system)

        container.axis = .vertical
        container.spacing = 0
        container.translatesAutoresizingMaskIntoConstraints = false
        nativeAdView.addSubview(container)

        NSLayoutConstraint.activate([
            container.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor, constant: 16),
            container.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -16),
            container.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 16),
            container.bottomAnchor.constraint(lessThanOrEqualTo: nativeAdView.bottomAnchor, constant: -16)
        ])

        topRow.axis = .horizontal
        topRow.alignment = .center
        topRow.spacing = 8

        identityRow.axis = .horizontal
        identityRow.alignment = .center
        identityRow.spacing = 8

        avatarLabel.text = "AD"
        avatarLabel.font = .systemFont(ofSize: 11, weight: .bold)
        avatarLabel.textColor = UIColor(ConCafeColors.primary)
        avatarLabel.textAlignment = .center
        avatarLabel.backgroundColor = UIColor(ConCafeColors.surfaceTint)
        avatarLabel.layer.cornerRadius = 16
        avatarLabel.clipsToBounds = true
        avatarLabel.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            avatarLabel.widthAnchor.constraint(equalToConstant: 32),
            avatarLabel.heightAnchor.constraint(equalToConstant: 32)
        ])

        advertiserLabel.font = .systemFont(ofSize: 13, weight: .medium)
        advertiserLabel.textColor = UIColor(ConCafeColors.textSecondary)
        advertiserLabel.numberOfLines = 1

        adBadgeLabel.text = "광고"
        adBadgeLabel.font = .systemFont(ofSize: 11, weight: .regular)
        adBadgeLabel.textColor = UIColor(ConCafeColors.outlineStrong)
        adBadgeLabel.setContentHuggingPriority(.required, for: .horizontal)

        headlineLabel.font = .systemFont(ofSize: 15, weight: .semibold)
        headlineLabel.textColor = UIColor(ConCafeColors.textPrimary)
        headlineLabel.numberOfLines = 2
        headlineLabel.lineBreakMode = .byTruncatingTail

        bodyLabel.font = .systemFont(ofSize: 13, weight: .regular)
        bodyLabel.textColor = UIColor(ConCafeColors.textSecondary)
        bodyLabel.numberOfLines = 3
        bodyLabel.lineBreakMode = .byTruncatingTail

        divider.backgroundColor = UIColor(ConCafeColors.primaryContainer.opacity(0.3))
        divider.translatesAutoresizingMaskIntoConstraints = false
        divider.heightAnchor.constraint(equalToConstant: 1).isActive = true

        bottomRow.axis = .horizontal
        bottomRow.alignment = .center

        callToActionButton.titleLabel?.font = .systemFont(ofSize: 12, weight: .semibold)
        callToActionButton.setTitleColor(UIColor(ConCafeColors.textPrimary), for: .normal)
        callToActionButton.backgroundColor = UIColor(ConCafeColors.primaryContainer)
        callToActionButton.contentEdgeInsets = UIEdgeInsets(top: 6, left: 14, bottom: 6, right: 14)
        callToActionButton.layer.cornerRadius = 16
        callToActionButton.isUserInteractionEnabled = false

        identityRow.addArrangedSubview(avatarLabel)
        identityRow.addArrangedSubview(advertiserLabel)
        topRow.addArrangedSubview(identityRow)
        topRow.addArrangedSubview(UIView())
        topRow.addArrangedSubview(adBadgeLabel)

        bottomRow.addArrangedSubview(callToActionButton)
        bottomRow.addArrangedSubview(UIView())

        container.addArrangedSubview(topRow)
        container.setCustomSpacing(10, after: topRow)
        container.addArrangedSubview(headlineLabel)
        container.setCustomSpacing(4, after: headlineLabel)
        container.addArrangedSubview(bodyLabel)
        container.setCustomSpacing(12, after: bodyLabel)
        container.addArrangedSubview(divider)
        container.setCustomSpacing(10, after: divider)
        container.addArrangedSubview(bottomRow)

        nativeAdView.headlineView = headlineLabel
        nativeAdView.bodyView = bodyLabel
        nativeAdView.advertiserView = advertiserLabel
        nativeAdView.callToActionView = callToActionButton

        return nativeAdView
    }

    func updateUIView(_ nativeAdView: GADNativeAdView, context: Context) {
        let advertiser = nativeAd.advertiser?.trimmingCharacters(in: .whitespacesAndNewlines)
        let callToAction = nativeAd.callToAction?.trimmingCharacters(in: .whitespacesAndNewlines)

        (nativeAdView.headlineView as? UILabel)?.text = nativeAd.headline
        (nativeAdView.bodyView as? UILabel)?.text = nativeAd.body
        (nativeAdView.advertiserView as? UILabel)?.text = advertiser?.isEmpty == false ? advertiser : "ConCafe"
        (nativeAdView.callToActionView as? UIButton)?.setTitle(callToAction?.isEmpty == false ? callToAction : "자세히", for: .normal)
        nativeAdView.callToActionView?.isUserInteractionEnabled = false
        nativeAdView.nativeAd = nativeAd
    }
}
#endif

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
                                colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                                startPoint: .topLeading, endPoint: .bottomTrailing
                            ))
                            .frame(width: 32, height: 32)
                            .overlay {
                                Text(String(post.userNickname.prefix(1).isEmpty ? "?" : post.userNickname.prefix(1)))
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(ConCafeColors.primary)
                            }
                        Text(post.userNickname.isEmpty ? String(localized: String.LocalizationValue("community_anonymous"), table: "Localizable") : post.userNickname)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(ConCafeColors.textSecondary)
                    }
                    Spacer()
                    Text(post.displayDate)
                        .font(.caption)
                        .foregroundStyle(ConCafeColors.outlineStrong)
                }
                .padding(.bottom, 10)
                Text(post.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(ConCafeColors.textPrimary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if !post.content.isEmpty {
                    Text(post.content)
                        .font(.caption)
                        .foregroundStyle(ConCafeColors.textSecondary)
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
                                            .fill(ConCafeColors.surfaceTint)
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
                    .overlay(ConCafeColors.primaryContainer.opacity(0.3))
                    .padding(.vertical, 10)
                HStack(spacing: 14) {
                    HStack(spacing: 4) {
                        Image(systemName: "heart")
                            .font(.caption2)
                            .foregroundStyle(ConCafeColors.outlineStrong)
                        Text(String(format: String(localized: String.LocalizationValue("community_post_like_count"), table: "Localizable"), locale: Locale.current, "\(post.likeCount)"))
                            .font(.caption)
                            .foregroundStyle(ConCafeColors.textMuted)
                    }
                    HStack(spacing: 4) {
                        Image(systemName: "bubble.left")
                            .font(.caption2)
                            .foregroundStyle(ConCafeColors.outlineStrong)
                        Text(String(format: String(localized: String.LocalizationValue("community_post_comment_count"), table: "Localizable"), locale: Locale.current, "\(post.commentCount)"))
                            .font(.caption)
                            .foregroundStyle(ConCafeColors.textMuted)
                    }
                }
            }
            .padding(16)
            .background(Color.white)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationView {
        CommunityView(onNavigationAction: { _ in })
    }
}
