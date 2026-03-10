//
//  CafeInfoEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit

struct CafeInfoEditView: View {
    let cafeId: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeInfoEditViewModel
    @State private var showSaveSuccessAlert = false

    var body: some View {
        CafeInfoEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationBarBackButtonHidden()
        .navigationTitle("카페 정보 관리")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    viewModel.onAction(.clickBack)
                } label: {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showSaveSuccessAlert:
                showSaveSuccessAlert = true
            }
        }
        .alert("카페 정보가 저장되었습니다.", isPresented: $showSaveSuccessAlert) {
            Button("확인", role: .cancel) {}
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeInfoEditViewModel(cafeId: cafeId))
    }
}

private struct CafeInfoEditContentView: View {
    let uiState: CafeInfoEditUiState

    let onAction: (CafeInfoEditAction) -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: 16) {
                    if uiState.isLoading {
                        ProgressView()
                            .tint(Color(hex: "EF6797"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 32)
                    }
                    if let infoMessage = uiState.infoMessage {
                        infoBanner(message: infoMessage)
                    }
                    basicInformationSection
                    representativeImageSection
                    gallerySection
                    locationContactSection
                    businessHoursSection
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
            bottomSaveBar
        }
        .background(Color(hex: "F8F5F6"))
    }

    private var basicInformationSection: some View {
        editSectionCard(title: "기본 정보") {
            cafeInfoTextField(
                label: "카페명",
                text: Binding(
                    get: { uiState.cafeName },
                    set: { onAction(.changeCafeName($0)) }
                )
            )
            cafeInfoTextEditor(
                label: "카페 소개",
                text: Binding(
                    get: { uiState.cafeDescription },
                    set: { onAction(.changeCafeDescription($0)) }
                )
            )
        }
    }

    private var representativeImageSection: some View {
        editSectionCard(title: "대표 이미지") {
            Button {
                onAction(.clickRepresentativeImage)
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFD8E6"), Color(hex: "FFEFF5")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(height: 200)
                    VStack(spacing: 8) {
                        Image(systemName: "camera.fill")
                            .font(.system(size: 32, weight: .semibold))
                            .foregroundStyle(Color(hex: "8B5164"))
                        Text(uiState.representativeImageTitle)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color(hex: "5A4954"))
                    }
                }
            }
            .buttonStyle(.plain)
            Text("검색 결과에 노출되는 대표 이미지입니다")
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var gallerySection: some View {
        editSectionCard(title: "카페 갤러리", trailing: {
            Text(uiState.galleryLimitText)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(hex: "EF6797"))
        }) {
            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12)
                ],
                spacing: 12
            ) {
                ForEach(Array(uiState.galleryImages.enumerated()), id: \.offset) { index, imageUrl in
                    galleryItem(
                        label: "이미지 \(index + 1)",
                        imageUrl: imageUrl,
                        index: index
                    )
                }
                addGalleryItem
            }
        }
    }

    private var locationContactSection: some View {
        editSectionCard(title: "위치 및 연락처") {
            cafeInfoTextField(
                label: "지역 / 주소",
                text: Binding(
                    get: { uiState.address },
                    set: { onAction(.changeAddress($0)) }
                ),
                trailingIcon: {
                    Image(systemName: "location.fill")
                        .foregroundStyle(Color(hex: "EF6797"))
                }
            )
            ZStack(alignment: .bottomTrailing) {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color(hex: "F4EFF2"))
                    .frame(height: 160)
                VStack(spacing: 8) {
                    Image(systemName: "map")
                        .font(.system(size: 36))
                        .foregroundStyle(Color(hex: "B5A9B0"))
                    Text("지도 미리보기")
                        .font(.subheadline)
                        .foregroundStyle(Color(hex: "998D95"))
                }
                Button {
                    onAction(.clickPinLocation)
                } label: {
                    Text("위치 지정")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Color(hex: "2B2330"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.92))
                        .clipShape(Capsule())
                        .overlay(
                            Capsule()
                                .stroke(Color(hex: "FFD1DC").opacity(0.4), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .padding(10)
            }
            cafeInfoTextField(
                label: "연락처",
                text: Binding(
                    get: { uiState.contactNumber },
                    set: { onAction(.changeContactNumber($0)) }
                )
            )
        }
    }

    private var businessHoursSection: some View {
        editSectionCard(title: "영업시간") {
            hoursRow(
                label: "평일",
                open: Binding(
                    get: { uiState.weekdayOpen },
                    set: { onAction(.changeWeekdayOpen($0)) }
                ),
                close: Binding(
                    get: { uiState.weekdayClose },
                    set: { onAction(.changeWeekdayClose($0)) }
                )
            )
            hoursRow(
                label: "주말",
                open: Binding(
                    get: { uiState.weekendOpen },
                    set: { onAction(.changeWeekendOpen($0)) }
                ),
                close: Binding(
                    get: { uiState.weekendClose },
                    set: { onAction(.changeWeekendClose($0)) }
                )
            )
            Button {
                onAction(.clickManageExceptionDates)
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "calendar.badge.clock")
                    Text("예외 영업일 관리")
                        .fontWeight(.semibold)
                }
                .foregroundStyle(Color(hex: "EF6797"))
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)
        }
    }

    private var bottomSaveBar: some View {
        VStack {
            Spacer()
            Button {
                onAction(.clickSave)
            } label: {
                HStack(spacing: 8) {
                    if uiState.isSaving {
                        ProgressView()
                            .tint(Color(hex: "2B2330"))
                    } else {
                        Image(systemName: "square.and.arrow.down.fill")
                    }
                    Text("변경사항 저장")
                        .fontWeight(.bold)
                }
                .foregroundStyle(Color(hex: "2B2330"))
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            .disabled(uiState.isSaving)
            .buttonStyle(.plain)
            .padding(16)
            .background(Color.white.opacity(0.92))
            .overlay(alignment: .top) {
                Rectangle()
                    .fill(Color(hex: "FFD1DC").opacity(0.2))
                    .frame(height: 1)
            }
        }
    }

    private func editSectionCard<Content: View, Trailing: View>(
        title: String,
        @ViewBuilder trailing: () -> Trailing,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(title)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))
                Spacer()
                trailing()
            }
            content()
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(Color(hex: "FFD1DC").opacity(0.1), lineWidth: 1)
        )
    }

    private func editSectionCard<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        editSectionCard(title: title, trailing: { EmptyView() }, content: content)
    }

    private func galleryItem(
        label: String,
        imageUrl: String,
        index: Int
    ) -> some View {
        let gradients = [
            ("FFD8E6", "FFF1F6"),
            ("F9D4E4", "FFE7F0"),
            ("FFD9CF", "FFF0EA")
        ]
        let colors = gradients[index % gradients.count]
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: colors.0), Color(hex: colors.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Text(imageUrl.isEmpty ? label : label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(hex: "5A4954"))
                .padding(10)
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private var addGalleryItem: some View {
        Button {
            onAction(.clickAddGalleryImage)
        } label: {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(hex: "FFD1DC").opacity(0.1))
                .overlay {
                    Circle()
                        .stroke(Color(hex: "FFD1DC").opacity(0.4), style: StrokeStyle(lineWidth: 2, dash: [5]))
                        .overlay {
                            Image(systemName: "plus")
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                        .padding(22)
                }
                .aspectRatio(1, contentMode: .fit)
        }
        .buttonStyle(.plain)
    }

    private func cafeInfoTextField<Trailing: View>(
        label: String,
        text: Binding<String>,
        @ViewBuilder trailingIcon: () -> Trailing
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            HStack(spacing: 8) {
                TextField("", text: text)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                trailingIcon()
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
    }

    private func cafeInfoTextField(
        label: String,
        text: Binding<String>
    ) -> some View {
        cafeInfoTextField(label: label, text: text) { EmptyView() }
    }

    private func cafeInfoTextEditor(
        label: String,
        text: Binding<String>
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            TextEditor(text: text)
                .frame(minHeight: 120)
                .background(TextEditorClearBackgroundView())
                .padding(12)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
                )
        }
    }

    private func hoursRow(
        label: String,
        open: Binding<String>,
        close: Binding<String>
    ) -> some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .frame(maxWidth: .infinity, alignment: .leading)

            smallTimeField(text: open)
            Text("—")
                .foregroundStyle(Color(hex: "8A8088"))
            smallTimeField(text: close)
        }
        .padding(12)
        .background(Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func smallTimeField(text: Binding<String>) -> some View {
        TextField("", text: text)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
            )
            .frame(width: 108)
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(hex: "FFF6D7"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "F1D88D"), lineWidth: 1)
        )
    }
}

private struct TextEditorClearBackgroundView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        DispatchQueue.main.async {
            UITextView.appearance().backgroundColor = .clear
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}

struct CafeInfoEditView_Previews: PreviewProvider {
    static var previews: some View {
        CafeInfoEditView(cafeId: "cafe-1", onNavigationAction: { _ in })
    }
}
