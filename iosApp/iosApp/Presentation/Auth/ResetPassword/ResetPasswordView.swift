//
//  ResetPasswordView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/28.
//

import SwiftUI
import UIKit

struct ResetPasswordView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = ResetPasswordViewModel()

    @State private var alertMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Image(systemName: "envelope.fill")
                        .foregroundStyle(.white)
                    Text("이메일로 비밀번호를 재설정하세요")
                        .font(.headline)
                        .bold()
                        .foregroundStyle(.white)
                    Text("가입한 이메일 주소로 재설정 링크를 보내드립니다.")
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
                settingsCard(title: "입력 정보", symbol: "envelope") {
                    Text("계정에 등록된 이메일을 입력하면 비밀번호 재설정 메일을 발송합니다.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    ConCafeFormField(
                        label: "이메일",
                        text: Binding(
                            get: { viewModel.uiState.email },
                            set: { viewModel.onAction(.emailChanged($0)) }
                        ),
                        placeholder: "가입한 이메일을 입력하세요",
                        keyboardType: .emailAddress,
                        trailingContent: {
                            Image(systemName: "envelope")
                                .foregroundStyle(Color(hex: "B3ACB7"))
                        }
                    )
                }
                settingsCard(title: "안내", symbol: "checkmark.shield") {
                    guideRow("메일 수신까지 1~3분 정도 소요될 수 있습니다.")
                    guideRow("메일이 보이지 않으면 스팸함을 확인해 주세요.")
                    guideRow("링크를 통해 새 비밀번호를 설정할 수 있습니다.")
                }
                Button {
                    viewModel.onAction(.submitTapped)
                } label: {
                    Text(viewModel.uiState.isSubmitting ? "발송 중..." : "재설정 메일 발송")
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
        .alert("비밀번호 재설정", isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button("확인", role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle("비밀번호 재설정")
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

struct ResetPasswordView_Previews: PreviewProvider {
    static var previews: some View {
        ResetPasswordView(onNavigationAction: { _ in })
    }
}
