import SwiftUI
import Shared

struct UserManagementView: View {
    @StateObject private var viewModel = UserManagementViewModel()

    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                filterRow
                content
                if let message = viewModel.uiState.infoMessage {
                    infoMessage(message)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(Color(hex: "FFF9FC"))
        .navigationTitle("유저 관리")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.uiState.filterChips) { chip in
                    Button {
                        viewModel.onAction(.selectFilter(chip.filter))
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: chip.filter == .cafeOwner ? "storefront.fill" : "nosign")
                            Text(chip.label)
                        }
                        .font(.subheadline.weight(chip.isSelected ? .bold : .medium))
                        .foregroundStyle(chip.isSelected ? Color(hex: "2B2330") : Color(hex: "6F6670"))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(chip.isSelected ? Color(hex: "FFD1DC") : Color(hex: "F5F2F4"))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.uiState.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.vertical, 48)
        } else if viewModel.uiState.users.isEmpty {
            Text("\(viewModel.uiState.selectedFilter.label) 목록이 없습니다.")
                .font(.subheadline)
                .foregroundStyle(Color(hex: "7A707A"))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(cardBackground)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        } else {
            ForEach(viewModel.uiState.users, id: \.id) { user in
                userCard(user)
            }
            if viewModel.uiState.canLoadMore || viewModel.uiState.isLoadingMore {
                Button {
                    viewModel.onAction(.loadMore)
                } label: {
                    HStack {
                        if viewModel.uiState.isLoadingMore {
                            ProgressView()
                        } else {
                            Text("더 불러오기")
                                .font(.subheadline.weight(.bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color(hex: "F5F2F4"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(viewModel.uiState.isLoadingMore)
            }
        }
    }

    private func userCard(_ user: User) -> some View {
        HStack(spacing: 12) {
            AsyncImage(url: resolvedRemoteImageUrl(user.profileImage)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Circle()
                    .fill(Color(hex: "FFE7EF"))
                    .overlay {
                        Text(String(user.nickname.prefix(1)))
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
            }
            .frame(width: 48, height: 48)
            .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(user.nickname)
                        .font(.subheadline.weight(.bold))
                        .lineLimit(1)
                    Text(user.banned ? "차단" : "운영자")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(user.banned ? Color(hex: "BE123C") : Color(hex: "6D28D9"))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(user.banned ? Color(hex: "FFE4E6") : Color(hex: "EDE9FE"))
                        .clipShape(Capsule())
                }
                Text(user.email)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "6F6670"))
                    .lineLimit(1)
                Text("가입일 \(String(user.createdAt.prefix(10)))")
                    .font(.caption2)
                    .foregroundStyle(Color(hex: "8B7F8A"))
            }
            Spacer()
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
                .foregroundStyle(Color(hex: "6B5320"))
            Spacer()
            Button("닫기") {
                viewModel.onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
        }
        .padding(14)
        .background(Color(hex: "FFF4CC"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }

    init(onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }) {
        self.onNavigationAction = onNavigationAction
    }
}
