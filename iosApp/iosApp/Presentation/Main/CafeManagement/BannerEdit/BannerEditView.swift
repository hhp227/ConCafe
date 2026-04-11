//
//  BannerEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI
import UIKit

struct BannerEditView: View {
    let initialCafeId: String?

    let initialBannerId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: BannerEditViewModel

    @State private var isImagePickerPresented = false

    var body: some View {
        BannerEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onPickImage: {
                isImagePickerPresented = true
            }
        )
        .navigationTitle(
            viewModel.uiState.isEditMode
            ? String(localized: String.LocalizationValue("banneredit_screen_title_edit"), table: "Localizable")
            : String(localized: String.LocalizationValue("banneredit_screen_title_create"), table: "Localizable")
        )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(String(localized: String.LocalizationValue("banneredit_action_save"), table: "Localizable")) {
                    viewModel.onAction(.clickSave)
                }
                .font(.system(size: 16, weight: .bold))
                .disabled(!viewModel.uiState.isSaveEnabled)
            }
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.selectorType != nil },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.dismissSelector)
                    }
                }
            )
        ) {
            BannerSelectorSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            .compatLargeSheetDetent()
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showSaveSuccessAlert:
                break
            }
        }
        .sheet(isPresented: $isImagePickerPresented) {
            CompatImagePicker(
                onImageSelected: { image in
                    isImagePickerPresented = false
                    saveImageToTemporaryFileAsync(image) { imageUrl in
                        if let imageUrl {
                            viewModel.onAction(.selectImage(imageUrl))
                        }
                    }
                },
                onDismiss: {
                    isImagePickerPresented = false
                }
            )
        }
        .alert(
            String(localized: String.LocalizationValue("banneredit_alert_image_title"), table: "Localizable"),
            isPresented: Binding(
                get: { viewModel.uiState.isImageRequiredAlertVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissImageRequiredAlert)
                    }
                }
            )
        ) {
            Button(String(localized: String.LocalizationValue("banner_action_ok"), table: "Localizable")) {
                viewModel.onAction(.dismissImageRequiredAlert)
            }
        } message: {
            Text(String(localized: String.LocalizationValue("banneredit_alert_image_message"), table: "Localizable"))
        }
    }

    init(
        initialCafeId: String? = nil,
        initialBannerId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.initialCafeId = initialCafeId
        self.initialBannerId = initialBannerId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(
            wrappedValue: BannerEditViewModel(
                initialCafeId: initialCafeId,
                initialBannerId: initialBannerId
            )
        )
    }
}

private struct BannerEditContentView: View {
    let uiState: BannerEditUiState

    let onAction: (BannerEditAction) -> Void

    let onPickImage: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: 16) {
                    bannerImageCard
                    basicInformationSection
                    targetSection
                    periodSection
                    if let infoMessage = uiState.infoMessage {
                        infoBanner(
                            message: {
                                switch infoMessage {
                                case "banneredit_info_slot_full",
                                     "banneredit_info_owned_cafe_load_failed",
                                     "banneredit_info_edit_banner_not_found",
                                     "banneredit_info_edit_banner_load_failed",
                                     "banneredit_info_select_cafe_first",
                                     "banneredit_info_notice_list_load_failed",
                                     "banneredit_info_event_list_load_failed",
                                     "banneredit_info_image_upload_failed",
                                     "banneredit_info_saved",
                                     "banneredit_validation_image_required",
                                     "banneredit_validation_title_required",
                                     "banneredit_validation_subtitle_required",
                                     "banneredit_validation_external_url_required",
                                     "banneredit_validation_target_required",
                                     "banneredit_error_unauthorized",
                                     "banneredit_error_permission_denied",
                                     "banneredit_error_target_not_found",
                                     "banneredit_error_network_failed",
                                     "banneredit_error_save_unknown":
                                    return String(localized: String.LocalizationValue(infoMessage), table: "Localizable")
                                default:
                                    return infoMessage
                                }
                            }()
                        )
                    }
                }
                .padding(16)
                .padding(.bottom, 100)
            }
            .background(
                LinearGradient(
                    colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            bottomSaveBar()
        }
    }

    private var bannerImageCard: some View {
        VStack(spacing: 12) {
            GeometryReader { proxy in
                ZStack {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFD8E6"), Color(hex: "FFEFF5")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    if let imageUrl = uiState.selectedImageLabel,
                       !imageUrl.isEmpty {
                        BannerEditImageView(
                            imageUrl: imageUrl,
                            placeholder: {
                                bannerPlaceholder
                            }
                        )
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .clipped()
                    } else {
                bannerPlaceholder
            }
        }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(16.0 / 10.0, contentMode: .fit)
            .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .onTapGesture {
                onAction(.clickImagePicker)
                onPickImage()
            }
            VStack(spacing: 4) {
                Text(String(localized: String.LocalizationValue("banneredit_image_section_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .multilineTextAlignment(.center)
                Text(String(localized: String.LocalizationValue("banneredit_image_guide"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(Color(hex: "8F848F"))
            }
            Button {
                onAction(.clickImagePicker)
                onPickImage()
            } label: {
                Text(String(localized: String.LocalizationValue("banneredit_image_button"), table: "Localizable"))
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))
                    .padding(.horizontal, 22)
                    .padding(.vertical, 10)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 24)
        .background(Color.white.opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(style: StrokeStyle(lineWidth: 2, dash: [8, 6]))
                .foregroundStyle(Color(hex: "FFD1DC").opacity(0.45))
        )
    }

    private var bannerPlaceholder: some View {
        VStack(spacing: 8) {
            Image(systemName: "photo.badge.plus")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(Color(hex: "EF6797"))
            Text(String(localized: String.LocalizationValue("banneredit_image_placeholder_pick"), table: "Localizable"))
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color(hex: "5A4954"))
        }
    }

    private var basicInformationSection: some View {
        sectionCard(title: String(localized: String.LocalizationValue("banneredit_section_basic"), table: "Localizable")) {
            ConCafeFormField(
                label: String(localized: String.LocalizationValue("banneredit_label_title"), table: "Localizable"),
                text: Binding(get: { uiState.title }, set: { onAction(.changeTitle($0)) }),
                placeholder: String(localized: String.LocalizationValue("banneredit_placeholder_title"), table: "Localizable")
            )
            ConCafeFormField(
                label: String(localized: String.LocalizationValue("banneredit_label_subtitle"), table: "Localizable"),
                text: Binding(get: { uiState.subtitle }, set: { onAction(.changeSubtitle($0)) }),
                placeholder: String(localized: String.LocalizationValue("banneredit_placeholder_subtitle"), table: "Localizable")
            )
        }
    }

    private var targetSection: some View {
        sectionCard(title: String(localized: String.LocalizationValue("banneredit_section_target"), table: "Localizable")) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 2), spacing: 8) {
                ForEach(BannerTargetType.allCases) { target in
                    Button {
                        onAction(.selectTarget(target))
                    } label: {
                        Text(String(localized: String.LocalizationValue(target.rawValue), table: "Localizable"))
                            .font(.subheadline.weight(uiState.selectedTarget == target ? .bold : .medium))
                            .foregroundStyle(uiState.selectedTarget == target ? Color(hex: "23161C") : Color(hex: "7A707A"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(uiState.selectedTarget == target ? Color(hex: "FFD1DC").opacity(0.12) : Color(hex: "F8F5F6"))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(uiState.selectedTarget == target ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.2), lineWidth: uiState.selectedTarget == target ? 2 : 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            switch uiState.selectedTarget {
            case .externalLink:
                ConCafeFormField(
                    label: String(localized: String.LocalizationValue("banneredit_label_external_link"), table: "Localizable"),
                    text: Binding(get: { uiState.targetValue }, set: { onAction(.changeTargetValue($0)) }),
                    placeholder: String(localized: String.LocalizationValue(uiState.targetFieldPlaceholderKey), table: "Localizable")
                )
            case .cafeDetail:
                if uiState.isAdmin {
                    selectionField(
                        label: String(localized: String.LocalizationValue("banneredit_label_owned_cafe"), table: "Localizable"),
                        selectedTitle: uiState.selectedCafeOption?.name,
                        selectedSubtitle: uiState.selectedCafeOption?.city,
                        placeholder: String(localized: String.LocalizationValue("banneredit_placeholder_select_owned_cafe"), table: "Localizable")
                    ) {
                        onAction(.clickCafeSelector)
                    }
                } else {
                    fixedSelectionField(
                        label: String(localized: String.LocalizationValue("banneredit_label_applied_cafe"), table: "Localizable"),
                        selectedTitle: uiState.selectedCafeOption?.name,
                        selectedSubtitle: uiState.selectedCafeOption?.city,
                        placeholder: String(localized: String.LocalizationValue("banneredit_placeholder_no_applied_cafe"), table: "Localizable")
                    )
                }
            case .notice, .eventDetail:
                if uiState.isAdmin {
                    selectionField(
                        label: String(localized: String.LocalizationValue("banneredit_label_owned_cafe"), table: "Localizable"),
                        selectedTitle: uiState.selectedCafeOption?.name,
                        selectedSubtitle: uiState.selectedCafeOption?.city,
                        placeholder: String(localized: String.LocalizationValue("banneredit_placeholder_select_owned_cafe"), table: "Localizable")
                    ) {
                        onAction(.clickCafeSelector)
                    }
                } else {
                    fixedSelectionField(
                        label: String(localized: String.LocalizationValue("banneredit_label_applied_cafe"), table: "Localizable"),
                        selectedTitle: uiState.selectedCafeOption?.name,
                        selectedSubtitle: uiState.selectedCafeOption?.city,
                        placeholder: String(localized: String.LocalizationValue("banneredit_placeholder_no_applied_cafe"), table: "Localizable")
                    )
                }
                selectionField(
                    label: String(localized: String.LocalizationValue(uiState.targetSelectionLabelKey), table: "Localizable"),
                    selectedTitle: uiState.selectedContentTitle,
                    selectedSubtitle: uiState.selectedContentSubtitle,
                    placeholder: String(localized: String.LocalizationValue(uiState.targetSelectionPlaceholderKey), table: "Localizable")
                ) {
                    onAction(.clickTargetSelector)
                }
            }
        }
    }

    private var periodSection: some View {
        sectionCard(title: String(localized: String.LocalizationValue("banneredit_section_period"), table: "Localizable")) {
            VStack(spacing: 8) {
                badgeText(String(format: String(localized: String.LocalizationValue("banneredit_display_days"), table: "Localizable"), uiState.displayDaysLabelValue))
                Slider(
                    value: Binding(
                        get: { Double(uiState.displayDays) },
                        set: { onAction(.changeDisplayDays(Int($0.rounded()))) }
                    ),
                    in: 1...10,
                    step: 1
                )
                .tint(Color(hex: "EF6797"))
                HStack {
                    Text(String(localized: String.LocalizationValue("banneredit_period_min_day"), table: "Localizable"))
                    Spacer()
                    Text(String(localized: String.LocalizationValue("banneredit_period_max_day"), table: "Localizable"))
                }
                .font(.caption2)
                .foregroundStyle(Color(hex: "9A8D95"))
            }
        }
    }

    private func bottomSaveBar() -> some View {
        Button {
            onAction(.clickSave)
        } label: {
            HStack(spacing: 8) {
                if uiState.isSaving {
                    ProgressView().tint(Color(hex: "2B2330"))
                } else {
                    Image(systemName: "square.and.arrow.down")
                }
                Text(
                    uiState.isEditMode
                    ? String(localized: String.LocalizationValue("banneredit_submit_edit"), table: "Localizable")
                    : String(localized: String.LocalizationValue("banneredit_submit_create"), table: "Localizable")
                ).fontWeight(.bold)
            }
            .foregroundStyle(Color(hex: "2B2330"))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(uiState.isSaveEnabled ? Color(hex: "FFD1DC") : Color(hex: "F4D7DF"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 16)
            .background(Color.white.opacity(0.94))
        }
        .buttonStyle(.plain)
        .disabled(!uiState.isSaveEnabled)
    }

    private func sectionCard<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 999, style: .continuous)
                    .fill(Color(hex: "FFD1DC"))
                    .frame(width: 4, height: 18)
                Text(title).font(.headline.weight(.bold))
            }
            content()
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func selectionField(
        label: String,
        selectedTitle: String?,
        selectedSubtitle: String?,
        placeholder: String,
        onTap: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            Button(action: onTap) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(selectedTitle ?? placeholder)
                            .font(.subheadline.weight(selectedTitle == nil ? .regular : .semibold))
                            .foregroundStyle(selectedTitle == nil ? Color(hex: "AA98A4") : Color(hex: "23161C"))
                        if let subtitle = selectedSubtitle, !subtitle.isEmpty {
                            Text(subtitle)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "8F848F"))
                        }
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(Color(hex: "8F848F"))
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private func fixedSelectionField(
        label: String,
        selectedTitle: String?,
        selectedSubtitle: String?,
        placeholder: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            VStack(alignment: .leading, spacing: 4) {
                Text(selectedTitle ?? placeholder)
                    .font(.subheadline.weight(selectedTitle == nil ? .regular : .semibold))
                    .foregroundStyle(selectedTitle == nil ? Color(hex: "AA98A4") : Color(hex: "23161C"))
                if let subtitle = selectedSubtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "8F848F"))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(hex: "F8F5F6"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
            )
        }
    }

    private func badgeText(_ text: String) -> some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "EF6797"))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color(hex: "EF6797").opacity(0.08))
            .clipShape(Capsule())
    }

    private func infoBanner(message: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "info.circle.fill")
                .foregroundStyle(Color(hex: "EF6797"))
                .padding(.top, 1)
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "7A707A"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(String(localized: String.LocalizationValue("banneredit_action_close"), table: "Localizable")) {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(hex: "FFD1DC").opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct BannerEditImageView<Placeholder: View>: View {
    let imageUrl: String
    let placeholder: () -> Placeholder

    var body: some View {
        if let fileUrl = URL(string: imageUrl),
           fileUrl.isFileURL,
           let uiImage = UIImage(contentsOfFile: fileUrl.path) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
        } else if let remoteUrl = URL(string: imageUrl) {
            AsyncImage(url: remoteUrl) { phase in
                switch phase {
                case .empty:
                    ProgressView()
                        .tint(Color(hex: "9C7A88"))
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    placeholder()
                @unknown default:
                    placeholder()
                }
            }
        } else {
            placeholder()
        }
    }
}

private func saveImageToTemporaryFile(_ image: UIImage) -> String? {
    saveCompressedImageToTemporaryFile(image)
}

private func saveImageToTemporaryFileAsync(
    _ image: UIImage,
    completion: @escaping (String?) -> Void
) {
    saveCompressedImageToTemporaryFileAsync(image, completion: completion)
}

private struct BannerSelectorSheet: View {
    let uiState: BannerEditUiState

    let onAction: (BannerEditAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String(localized: String.LocalizationValue(uiState.selectorTitleKey), table: "Localizable"))
                .font(.title3.weight(.bold))
                .padding(.horizontal, 24)
            ConCafeFormField(
                label: String(localized: String.LocalizationValue("banneredit_label_search"), table: "Localizable"),
                text: Binding(get: { uiState.selectorQuery }, set: { onAction(.changeSelectorQuery($0)) }),
                placeholder: String(localized: String.LocalizationValue(uiState.selectorSearchPlaceholderKey), table: "Localizable")
            )
            .padding(.horizontal, 24)
            if uiState.isSelectorLoading {
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
            } else if uiState.activeSelectorItemCount == 0 {
                Text(String(localized: String.LocalizationValue("banneredit_selector_empty"), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "8F848F"))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 24)
            } else {
                ScrollView {
                    VStack(spacing: 10) {
                        if uiState.selectorType == .cafe {
                            ForEach(uiState.filteredCafeSelectorOptions, id: \.id) { item in
                                selectorOptionButton(
                                    id: item.id,
                                    title: item.name,
                                    subtitle: item.city,
                                    onAction: onAction
                                )
                            }
                        } else if uiState.selectorType == .notice {
                            ForEach(uiState.noticeSelectorOptions, id: \.id) { item in
                                selectorOptionButton(
                                    id: item.id,
                                    title: item.title,
                                    subtitle: item.displayDate,
                                    onAction: onAction
                                )
                            }
                        } else if uiState.selectorType == .event {
                            ForEach(uiState.eventSelectorOptions, id: \.id) { item in
                                selectorOptionButton(
                                    id: item.id,
                                    title: item.title,
                                    subtitle: item.periodText,
                                    onAction: onAction
                                )
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
                }
            }
        }
        .padding(.top, 16)
        .background(Color(hex: "F8F5F6"))
    }

    private func selectorOptionButton(
        id: String,
        title: String,
        subtitle: String,
        onAction: @escaping (BannerEditAction) -> Void
    ) -> some View {
        Button {
            onAction(.selectSelectorItem(id))
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "23161C"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "8F848F"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct BannerEditView_Previews: PreviewProvider {
    static var previews: some View {
        CompatNavigationContainer {
            BannerEditView()
        }
    }
}
