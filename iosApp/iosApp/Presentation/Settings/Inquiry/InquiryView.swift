//
//  InquiryView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI

struct InquiryView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = InquiryViewModel()

    @State private var alertMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                inquiryTypeSection
                inquiryInputSection
            }
            .padding(16)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            submitButtonBar
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert("문의하기", isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button("확인", role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle("문의하기")
        .navigationBarTitleDisplayMode(.inline)
        .background(Color(hex: "FFFBFD"))
    }

    private var inquiryTypeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("문의 유형")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            Text("문의 성격에 맞는 항목을 선택해 주세요.")
                .font(.footnote)
                .foregroundStyle(.secondary)
            Menu {
                ForEach(InquiryType.allCases, id: \.self) { type in
                    Button(type.title) {
                        viewModel.onAction(.inquiryTypeChanged(type))
                    }
                }
            } label: {
                HStack {
                    Text(viewModel.uiState.inquiryType.title)
                        .foregroundStyle(Color(hex: "2B2330"))
                    Spacer()
                    Image(systemName: "chevron.down")
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var inquiryInputSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("문의 입력")
                .font(.title3.weight(.bold))
            Text("입력한 문의는 서버에 저장되어 운영팀이 확인합니다.")
                .font(.footnote)
                .foregroundStyle(.secondary)

            ConCafeFormField(
                label: "제목",
                text: Binding(
                    get: { viewModel.uiState.title },
                    set: { viewModel.onAction(.titleChanged($0)) }
                ),
                placeholder: "문의 제목을 입력하세요"
            )

            ConCafeFormEditor(
                label: "문의 내용",
                text: Binding(
                    get: { viewModel.uiState.message },
                    set: { viewModel.onAction(.messageChanged($0)) }
                ),
                placeholder: "상세 내용을 입력해 주세요"
            )

            if let errorMessage = viewModel.uiState.errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var submitButtonBar: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: "FFD1DC").opacity(0.2))
                .frame(height: 1)
            Button {
                viewModel.onAction(.submitTapped)
            } label: {
                HStack {
                    Spacer()
                    if viewModel.uiState.isSubmitting {
                        ProgressView()
                            .progressViewStyle(.circular)
                    } else {
                        Image(systemName: "paperplane.fill")
                        Text("문의 접수")
                            .font(.headline.weight(.bold))
                    }
                    Spacer()
                }
                .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(hex: "FFD1DC"))
            .foregroundStyle(Color(hex: "2B2330"))
            .disabled(viewModel.uiState.isSubmitting)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(Color.white.opacity(0.92))
        }
    }
}

struct InquiryView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            InquiryView(onNavigationAction: { _ in })
        }
    }
}
