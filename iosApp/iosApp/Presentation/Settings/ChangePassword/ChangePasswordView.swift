//
//  ChangePasswordView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import SwiftUI

struct ChangePasswordView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = ChangePasswordViewModel()

    @State private var alertMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.white)
                    Text("새 비밀번호를 설정하세요")
                        .font(.headline)
                        .bold()
                        .foregroundStyle(.white)
                    Text("현재 비밀번호를 확인한 뒤 새 비밀번호를 등록합니다.")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.92))
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
                settingsCard(title: "입력 정보", symbol: "key.horizontal") {
                    Text("비밀번호는 전용 화면에서만 변경되며, 변경 전 현재 비밀번호 확인이 필요합니다.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    ConCafeFormField(
                        label: "현재 비밀번호",
                        text: Binding(
                            get: { viewModel.uiState.currentPassword },
                            set: { viewModel.onAction(.currentPasswordChanged($0)) }
                        ),
                        placeholder: "현재 비밀번호를 입력하세요"
                    )
                    ConCafeFormField(
                        label: "새 비밀번호",
                        text: Binding(
                            get: { viewModel.uiState.newPassword },
                            set: { viewModel.onAction(.newPasswordChanged($0)) }
                        ),
                        placeholder: "8자 이상 입력하세요"
                    )
                    ConCafeFormField(
                        label: "새 비밀번호 확인",
                        text: Binding(
                            get: { viewModel.uiState.confirmPassword },
                            set: { viewModel.onAction(.confirmPasswordChanged($0)) }
                        ),
                        placeholder: "새 비밀번호를 다시 입력하세요"
                    )
                }
                settingsCard(title: "안내", symbol: "checkmark.shield") {
                    guideRow("새 비밀번호는 8자 이상이어야 합니다.")
                    guideRow("새 비밀번호 확인 입력값까지 일치해야 합니다.")
                    guideRow("변경 즉시 다음 로그인부터 새 비밀번호가 적용됩니다.")
                }
                Button {
                    viewModel.onAction(.submitTapped)
                } label: {
                    Text(viewModel.uiState.isSubmitting ? "변경 중..." : "비밀번호 변경")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundStyle(Color(hex: "2B2330"))
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color(hex: "FFD1DC"))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(viewModel.uiState.isSubmitting)
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(Color(hex: "FFFBFD"))
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert("비밀번호 변경", isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button("확인", role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle("비밀번호 변경")
        .navigationBarTitleDisplayMode(.inline)
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

    private func guideRow(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Color(hex: "EF6797"))
            Text(text)
                .font(.caption)
                .foregroundStyle(Color(hex: "6F6673"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct ChangePasswordView_Previews: PreviewProvider {
    static var previews: some View {
        ChangePasswordView(onNavigationAction: { _ in })
    }
}
