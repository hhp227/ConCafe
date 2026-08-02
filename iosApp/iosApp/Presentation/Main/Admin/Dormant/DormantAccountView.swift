import SwiftUI
import Shared

struct DormantAccountView: View {
    @StateObject private var viewModel = DormantAccountViewModel()

    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        DormantAccountContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle("휴면계정 관리")
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            viewModel.uiState.confirmTarget?.dormant == true ? "휴면 전환" : "휴면 해제",
            isPresented: Binding(
                get: { viewModel.uiState.confirmTarget != nil },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.cancelDormantChange)
                    }
                }
            ),
            presenting: viewModel.uiState.confirmTarget
        ) { request in
            Button(request.dormant ? "전환" : "해제") {
                viewModel.onAction(.confirmDormantChange)
            }
            Button("취소", role: .cancel) {
                viewModel.onAction(.cancelDormantChange)
            }
        } message: { request in
            Text(
                request.dormant
                    ? "\(request.user.nickname) 계정을 휴면 상태로 전환할까요? 휴면 계정은 다시 로그인하면 자동으로 해제됩니다."
                    : "\(request.user.nickname) 계정의 휴면 상태를 해제할까요?"
            )
        }
    }

    init(onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }) {
        self.onNavigationAction = onNavigationAction
    }
}

private struct DormantAccountContentView: View {
    let uiState: DormantAccountUiState

    let onAction: (DormantAccountAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                filterRow
                content
                if let message = uiState.infoMessage {
                    infoMessage(message)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(ConCafeColors.background)
    }

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(uiState.filterChips) { chip in
                    Button {
                        onAction(.selectFilter(chip.filter))
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: chip.filter == .dormant ? "moon.zzz.fill" : "clock.fill")
                            Text(chip.label)
                        }
                        .font(.subheadline.weight(chip.isSelected ? .bold : .medium))
                        .foregroundStyle(chip.isSelected ? ConCafeColors.textPrimary : ConCafeColors.textSecondary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(chip.isSelected ? ConCafeColors.primaryContainer : ConCafeColors.surfaceVariant)
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if uiState.isLoading {
            ShimmerListSkeleton(itemCount: 8)
        } else if uiState.users.isEmpty {
            Text("\(uiState.selectedFilter.label) 목록이 없습니다.")
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(cardBackground)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        } else {
            ForEach(uiState.users, id: \.id) { user in
                accountCard(user)
            }
            if uiState.canLoadMore || uiState.isLoadingMore {
                Button {
                    onAction(.loadMore)
                } label: {
                    HStack {
                        if uiState.isLoadingMore {
                            ProgressView()
                        } else {
                            Text("더 불러오기")
                                .font(.subheadline.weight(.bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(ConCafeColors.surfaceVariant)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(uiState.isLoadingMore)
            }
        }
    }

    private func accountCard(_ user: User) -> some View {
        HStack(spacing: 12) {
            AsyncImage(url: resolvedRemoteImageUrl(user.profileImage)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Circle()
                    .fill(ConCafeColors.surfaceTint)
                    .overlay {
                        Text(String(user.nickname.prefix(1)))
                            .font(.headline.weight(.bold))
                            .foregroundStyle(ConCafeColors.primary)
                    }
            }
            .frame(width: 48, height: 48)
            .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(user.nickname)
                        .font(.subheadline.weight(.bold))
                        .lineLimit(1)
                    Text(user.dormant ? "휴면" : "휴면 예정")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(user.dormant ? ConCafeColors.error : ConCafeColors.warning)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(user.dormant ? ConCafeColors.errorContainer : ConCafeColors.warningContainer)
                        .clipShape(Capsule())
                }
                Text(user.email)
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.textSecondary)
                    .lineLimit(1)
                Text("마지막 로그인 \(lastLoginLabel(user))")
                    .font(.caption2)
                    .foregroundStyle(ConCafeColors.textMuted)
                if uiState.selectedFilter == .dormant {
                    Text("휴면 전환일 \(dormantAtLabel(user))")
                        .font(.caption2)
                        .foregroundStyle(ConCafeColors.textMuted)
                }
            }
            Spacer()
            Button {
                onAction(.requestDormantChange(user, !user.dormant))
            } label: {
                Text(uiState.selectedFilter == .dormant ? "휴면 해제" : "휴면 전환")
                    .font(.caption.weight(.bold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(ConCafeColors.surfaceVariant)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var cardBackground: Color {
        Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white })
    }

    private func infoMessage(_ message: String) -> some View {
        HStack {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.goldDeep)
            Spacer()
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(ConCafeColors.goldDeep)
        }
        .padding(14)
        .background(ConCafeColors.goldContainer)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func lastLoginLabel(_ user: User) -> String {
        let raw = user.lastLoginAt?.prefix(10) ?? ""
        return raw.isEmpty ? "기록 없음" : String(raw)
    }

    private func dormantAtLabel(_ user: User) -> String {
        let raw = user.dormantAt?.prefix(10) ?? ""
        return raw.isEmpty ? "-" : String(raw)
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }
}
