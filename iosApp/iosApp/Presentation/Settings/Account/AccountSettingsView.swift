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

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 18) {
                        heroCard
                        if let errorMessage = uiState.errorMessage {
                            Text(errorMessage)
                                .font(.subheadline)
                                .foregroundStyle(Color.red)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        settingsCard(title: "기본 정보", symbol: "person.crop.circle") {
                            sectionEyebrow("내 계정에서 바로 수정 가능한 정보")
                            ConCafeFormField(
                                label: "닉네임",
                                text: Binding(
                                    get: { uiState.nickname },
                                    set: { onAction(.nicknameChanged($0)) }
                                ),
                                placeholder: "닉네임을 입력하세요"
                            )
                            ConCafeFormField(
                                label: "이메일",
                                text: Binding(
                                    get: { uiState.email },
                                    set: { onAction(.emailChanged($0)) }
                                ),
                                placeholder: "이메일을 입력하세요"
                            )
                            VStack(spacing: 10) {
                                infoRow(label: "권한", value: uiState.role.displayText)
                                infoRow(label: "가입일", value: uiState.memberSince.isEmpty ? "연동 예정" : uiState.memberSince)
                                if uiState.role == .cafeOwner {
                                    infoRow(label: "운영 카페 수", value: "\(uiState.ownedCafeCount)곳")
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(hex: "F8F5F6"))
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            if uiState.role == .cafeOwner {
                                Text("운영 권한 정보는 카페 관리 화면에서 이어서 확인할 수 있습니다.")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        settingsCard(title: "저장", symbol: "square.and.arrow.down") {
                            sectionEyebrow("닉네임과 이메일 변경 사항을 반영합니다")
                            primaryButton(title: "사용자 정보 저장") {
                                onAction(.saveUserInfoTapped)
                            }
                        }
                        if uiState.role == .cast {
                            settingsCard(title: "캐스트 연결 상태", symbol: "person.crop.rectangle.stack") {
                                sectionEyebrow("현재 연결된 프로필 요약")
                                Text(uiState.castDescription.isEmpty ? "캐스트 설명이 아직 없습니다. 전용 수정 화면에서 프로필과 공개 정보를 편집할 수 있습니다." : uiState.castDescription)
                                    .font(.subheadline)
                                    .foregroundStyle(Color(hex: "6F6673"))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        if uiState.role == .cafeOwner || uiState.role == .admin {
                            settingsCard(title: "권한 연결 상태", symbol: "storefront") {
                                sectionEyebrow("현재 계정에 연결된 운영 권한")
                                Text(uiState.role == .admin ? "관리자 계정은 운영 승인과 검토 작업을 수행합니다." : "운영 카페 \(uiState.ownedCafeCount)곳이 현재 계정과 연결되어 있습니다.")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
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
                                    description: [uiState.castName, uiState.castConceptRole]
                                        .filter { !$0.isEmpty }
                                        .joined(separator: " · "),
                                    supporting: uiState.linkedCafeName ?? "캐스트 프로필 전체 편집 화면으로 이동합니다.",
                                    symbol: "person.text.rectangle",
                                    onTap: { onAction(.openCastEditTapped) }
                                )
                            }
                        }
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
                    .padding(16)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Color(hex: "FFFBFD"))
        .alert("회원탈퇴 확인", isPresented: Binding(
            get: { uiState.isDeleteDialogVisible },
            set: { if !$0 { onAction(.dismissDeleteDialogTapped) } }
        )) {
            TextField(
                "탈퇴",
                text: Binding(
                    get: { uiState.deleteConfirmation },
                    set: { onAction(.deleteConfirmationChanged($0)) }
                )
            )
            Button("취소", role: .cancel) {
                onAction(.dismissDeleteDialogTapped)
            }
            Button("회원탈퇴", role: .destructive) {
                onAction(.deleteAccountTapped)
            }
        } message: {
            Text("정말 탈퇴하려면 아래 입력칸에 '탈퇴'를 입력해 주세요.")
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(uiState.nickname.isEmpty ? "ConCafe User" : uiState.nickname)
                .font(.title3)
                .bold()
                .foregroundStyle(.white)
            Text(uiState.email.isEmpty ? "로그인 정보 없음" : uiState.email)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.92))
            Text(uiState.roleSummary)
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

private extension UserRole? {
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
}

struct AccountSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        AccountSettingsView(onNavigationAction: { _ in })
    }
}
