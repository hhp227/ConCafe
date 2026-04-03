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
        .alert(String(localized: String.LocalizationValue("inquiry_screen_title"), table: "Localizable"), isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle(String(localized: String.LocalizationValue("inquiry_screen_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .background(Color(hex: "FFFBFD"))
    }

    private var inquiryTypeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: String.LocalizationValue("inquiry_type_section_title"), table: "Localizable"))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            Text(String(localized: String.LocalizationValue("inquiry_type_section_desc"), table: "Localizable"))
                .font(.footnote)
                .foregroundStyle(.secondary)
            Menu {
                ForEach(InquiryType.allCases, id: \.self) { type in
                    Button(localizedInquiryTypeTitle(type)) {
                        viewModel.onAction(.inquiryTypeChanged(type))
                    }
                }
            } label: {
                HStack {
                    Text(localizedInquiryTypeTitle(viewModel.uiState.inquiryType))
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
            Text(String(localized: String.LocalizationValue("inquiry_input_section_title"), table: "Localizable"))
                .font(.title3.weight(.bold))
            Text(String(localized: String.LocalizationValue("inquiry_input_section_desc"), table: "Localizable"))
                .font(.footnote)
                .foregroundStyle(.secondary)

            ConCafeFormField(
                label: String(localized: String.LocalizationValue("inquiry_title_label"), table: "Localizable"),
                text: Binding(
                    get: { viewModel.uiState.title },
                    set: { viewModel.onAction(.titleChanged($0)) }
                ),
                placeholder: String(localized: String.LocalizationValue("inquiry_title_placeholder"), table: "Localizable")
            )

            ConCafeFormEditor(
                label: String(localized: String.LocalizationValue("inquiry_message_label"), table: "Localizable"),
                text: Binding(
                    get: { viewModel.uiState.message },
                    set: { viewModel.onAction(.messageChanged($0)) }
                ),
                placeholder: String(localized: String.LocalizationValue("inquiry_message_placeholder"), table: "Localizable")
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
                        Text(String(localized: String.LocalizationValue("inquiry_submit"), table: "Localizable"))
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

    private func localizedInquiryTypeTitle(_ type: InquiryType) -> String {
        switch type {
        case .service:
            return String(localized: String.LocalizationValue("inquiry_type_service"), table: "Localizable")
        case .bugReport:
            return String(localized: String.LocalizationValue("inquiry_type_bug_report"), table: "Localizable")
        case .suggestion:
            return String(localized: String.LocalizationValue("inquiry_type_suggestion"), table: "Localizable")
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
