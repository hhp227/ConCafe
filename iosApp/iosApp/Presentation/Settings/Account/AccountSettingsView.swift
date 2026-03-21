//
//  AccountSettingsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI
import Shared

struct AccountSettingsView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = AccountSettingsViewModel()
    
    @State private var alertMessage: String?

    var body: some View {
        AccountSettingsContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToMain:
                onNavigationAction(.navigateToMain())
            case .navigateToCastEdit(let cafeId, let castId):
                onNavigationAction(.navigateToCastEdit(cafeId: cafeId, castId: castId))
            case .navigateToChangePassword:
                onNavigationAction(.navigateToChangePassword)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert("계정 관리", isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button("확인", role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle("계정 관리")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct AccountSettingsContentView: View {
    let uiState: AccountSettingsUiState
    
    let onAction: (AccountSettingsAction) -> Void

    private var myInfoFeed: Shared.MyInfoFeed? { uiState.myInfoFeed }
    private var currentUser: User? { myInfoFeed?.user }
    private var currentCast: Cast? { myInfoFeed?.castDetail?.cast }
    private var linkedCafeName: String? { myInfoFeed?.castDetail?.cafe.name }

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 18) {
                        heroCard
                        errorMessageView
                        basicInfoSection
                        saveSection
                        castStatusSection
                        ownerStatusSection
                        securitySection
                        deleteButton
                    }
                    .padding(16)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Color(hex: "FFFBFD"))
        .sheet(isPresented: Binding(
            get: { uiState.isDeleteDialogVisible },
            set: { if !$0 { onAction(.dismissDeleteDialogTapped) } }
        )) {
            AccountDeleteConfirmationSheet(
                passwordText: Binding(
                    get: { uiState.deletePassword },
                    set: { onAction(.deletePasswordChanged($0)) }
                ),
                errorMessage: uiState.deletePasswordErrorMessage,
                onDismiss: { onAction(.dismissDeleteDialogTapped) },
                onDelete: { onAction(.deleteAccountTapped) }
            )
        }
    }

    @ViewBuilder
    private var errorMessageView: some View {
        if let errorMessage = uiState.errorMessage {
            Text(errorMessage)
                .font(.subheadline)
                .foregroundStyle(Color.red)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var basicInfoSection: some View {
        settingsCard(title: "기본 정보", symbol: "person.crop.circle") {
            sectionEyebrow("내 계정에서 바로 수정 가능한 정보")
            ConCafeFormField(
                label: "닉네임",
                text: Binding(
                    get: { uiState.nicknameInput },
                    set: { onAction(.nicknameChanged($0)) }
                ),
                placeholder: "닉네임을 입력하세요"
            )
            infoSummaryCard
            if uiState.role == .cafeOwner {
                Text("운영 권한 정보는 카페 관리 화면에서 이어서 확인할 수 있습니다.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var infoSummaryCard: some View {
        VStack(spacing: 10) {
            infoRow(label: "권한", value: uiState.role?.displayText ?? "")
            infoRow(label: "가입일", value: (currentUser?.createdAt.isEmpty == false ? currentUser?.createdAt : "연동 예정") ?? "연동 예정")
            if uiState.role == .cafeOwner {
                infoRow(label: "운영 카페 수", value: "\(myInfoFeed?.ownedCafes.count ?? 0)곳")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var saveSection: some View {
        settingsCard(title: "저장", symbol: "square.and.arrow.down") {
            sectionEyebrow("닉네임 변경 사항을 반영합니다")
            primaryButton(title: "사용자 정보 저장") {
                onAction(.saveUserInfoTapped)
            }
        }
    }

    @ViewBuilder
    private var castStatusSection: some View {
        if uiState.role == .cast {
            settingsCard(title: "캐스트 연결 상태", symbol: "person.crop.rectangle.stack") {
                sectionEyebrow("현재 연결된 프로필 요약")
                Text(castDescriptionText)
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "6F6673"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    @ViewBuilder
    private var ownerStatusSection: some View {
        if uiState.role == .cafeOwner || uiState.role == .admin {
            settingsCard(title: "권한 연결 상태", symbol: "storefront") {
                sectionEyebrow("현재 계정에 연결된 운영 권한")
                Text(ownerStatusText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var securitySection: some View {
        settingsCard(title: "보안 및 연결", symbol: "lock.shield") {
            sectionEyebrow("전용 화면으로 이동해 안전하게 처리합니다")
            linkedDestinationCard(
                title: "비밀번호 변경",
                description: "현재 비밀번호 확인 후 새 비밀번호를 설정합니다.",
                supporting: "비밀번호는 전용 화면에서만 변경합니다.",
                symbol: "lock.shield",
                onTap: { onAction(.openChangePasswordTapped) }
            )
            if uiState.role == .cast {
                linkedDestinationCard(
                    title: "캐스트 정보 수정",
                    description: castLinkDescription,
                    supporting: linkedCafeName ?? "캐스트 프로필 전체 편집 화면으로 이동합니다.",
                    symbol: "person.text.rectangle",
                    onTap: { onAction(.openCastEditTapped) }
                )
            }
        }
    }

    private var deleteButton: some View {
        Button {
            onAction(.showDeleteDialogTapped)
        } label: {
            Text(uiState.isDeleteRequested ? "회원탈퇴 요청 완료" : "회원탈퇴")
                .font(.footnote)
                .foregroundStyle(uiState.isDeleteRequested ? Color(hex: "B84473") : Color(hex: "8E8794"))
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    private var castDescriptionText: String {
        (currentCast?.desc.isEmpty == false ? currentCast?.desc : nil)
        ?? "캐스트 설명이 아직 없습니다. 전용 수정 화면에서 프로필과 공개 정보를 편집할 수 있습니다."
    }

    private var ownerStatusText: String {
        if uiState.role == .admin {
            return "관리자 계정은 운영 승인과 검토 작업을 수행합니다."
        }
        return "운영 카페 \(myInfoFeed?.ownedCafes.count ?? 0)곳이 현재 계정과 연결되어 있습니다."
    }

    private var castLinkDescription: String {
        [currentCast?.name ?? "", currentCast?.conceptRole ?? ""]
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(currentUser?.nickname.isEmpty == false ? (currentUser?.nickname ?? "") : "ConCafe User")
                .font(.title3)
                .bold()
                .foregroundStyle(.white)
            Text(currentUser?.email.isEmpty == false ? (currentUser?.email ?? "") : "로그인 정보 없음")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.92))
            Text(uiState.role?.roleSummary ?? "")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.92))
                .padding(.top, 4)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(hex: "EF6797"), Color(hex: "F7A0C1")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func sectionEyebrow(_ text: String) -> some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(Color(hex: "8E8794"))
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func settingsCard<Content: View>(
        title: String,
        symbol: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .foregroundStyle(Color(hex: "EF6797"))
                Text(title)
                    .font(.title3)
                    .bold()
            }
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func linkedDestinationCard(
        title: String,
        description: String,
        supporting: String,
        symbol: String,
        onTap: @escaping () -> Void
    ) -> some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color(hex: "FFF1F7"))
                    .frame(width: 46, height: 46)
                    .overlay(
                        Image(systemName: symbol)
                            .foregroundStyle(Color(hex: "EF6797"))
                    )
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.subheadline)
                        .bold()
                        .foregroundStyle(Color.primary)
                    if !description.isEmpty {
                        Text(description)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "302732"))
                    }
                    Text(supporting)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: "B3ACB7"))
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func primaryButton(title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundStyle(Color(hex: "2B2330"))
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.subheadline)
                .bold()
                .foregroundStyle(Color.primary)
        }
    }
}

private extension UserRole {
    var displayText: String {
        switch self {
        case .admin:
            return "관리자"
        case .cafeOwner:
            return "카페 운영자"
        case .cast:
            return "캐스트"
        case .visitor:
            return "일반 유저"
        default:
            return "게스트"
        }
    }

    var roleSummary: String {
        switch self {
        case .cast:
            return "캐스트 계정으로 팬과의 접점을 관리하고 있어요."
        case .cafeOwner:
            return "운영 카페와 함께 계정 권한을 관리하고 있어요."
        case .admin:
            return "운영 관리용 관리자 계정입니다."
        case .visitor:
            return "팬 활동과 리뷰 기록을 관리하는 일반 계정입니다."
        default:
            return "로그인이 필요한 화면입니다."
        }
    }
}

private struct AccountDeleteConfirmationSheet: View {
    @Binding var passwordText: String

    let errorMessage: String?

    let onDismiss: () -> Void

    let onDelete: () -> Void

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("회원탈퇴")
                        .font(.title3.bold())
                    Text("계정 보안을 위해 현재 비밀번호를 입력해 주세요.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                ConCafeFormField(
                    label: "현재 비밀번호",
                    text: $passwordText,
                    placeholder: "비밀번호 입력",
                    isSecure: true
                )
                .textInputAutocapitalization(.never)
                if let errorMessage, !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(Color.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                HStack(spacing: 12) {
                    Button(action: onDismiss) {
                        Text("취소")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color(hex: "6F6673"))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color(hex: "F4EDF1"))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    Button(action: onDelete) {
                        Text("회원탈퇴")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color(hex: "C9527E"))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
            }
            .padding(20)
            .background(Color(hex: "FFFBFD"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("닫기") {
                        onDismiss()
                    }
                }
            }
        }
    }
}

struct AccountSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        AccountSettingsView(onNavigationAction: { _ in })
    }
}
