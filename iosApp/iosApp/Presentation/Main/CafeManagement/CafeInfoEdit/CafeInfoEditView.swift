//
//  CafeInfoEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit
import MapKit
import CoreLocation

struct CafeInfoEditView: View {
    let cafeId: String?

    let isRegistrationMode: Bool

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeInfoEditViewModel

    @State private var isPhotoPickerPresented = false

    @State private var imagePickTarget: CafeInfoImagePickTarget?

    var body: some View {
        CafeInfoEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onSearchAddressLocation: { query in
                resolveAddressAndUpdateMap(query: query)
            },
            onRepresentativeImagePick: {
                imagePickTarget = .representative
                isPhotoPickerPresented = true
            },
            onGalleryImagePick: {
                imagePickTarget = .gallery
                isPhotoPickerPresented = true
            }
        )
        .navigationTitle(viewModel.uiState.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showSaveSuccessAlert:
                break
            }
        }
        .alert(
            "이미지 등록 필요",
            isPresented: Binding(
                get: { viewModel.uiState.isImageRequiredAlertVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissImageRequiredAlert)
                    }
                }
            )
        ) {
            Button("확인") {
                viewModel.onAction(.dismissImageRequiredAlert)
            }
        } message: {
            Text("카페 등록/수정에는 대표 이미지 또는 갤러리 이미지가 필요합니다.")
        }
        .sheet(isPresented: $isPhotoPickerPresented) {
            CompatImagePicker(
                onImageSelected: { image in
                    let target = imagePickTarget
                    isPhotoPickerPresented = false

                    guard let target else { return }
                    guard let imageUrl = saveImageToTemporaryFile(image) else {
                        imagePickTarget = nil
                        return
                    }

                    switch target {
                    case .representative:
                        viewModel.onAction(.selectRepresentativeImage(imageUrl))
                    case .gallery:
                        viewModel.onAction(.addGalleryImage(imageUrl))
                    }

                    imagePickTarget = nil
                },
                onDismiss: {
                    isPhotoPickerPresented = false
                }
            )
        }
    }

    private func resolveAddressAndUpdateMap(query: String) {
        let normalizedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)

        if normalizedQuery.isEmpty {
            return
        }
        let geocoder = CLGeocoder()

        geocoder.geocodeAddressString(normalizedQuery) { placemarks, _ in
            guard let location = placemarks?.first?.location else { return }
            let fullAddress = placemarks?
                .first
                .flatMap { placemark -> String? in
                    let address = [
                        placemark.administrativeArea,
                        placemark.locality,
                        placemark.thoroughfare,
                        placemark.subThoroughfare
                    ]
                        .compactMap { $0 }
                        .joined(separator: " ")
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    return address.isEmpty ? nil : address
                } ?? normalizedQuery

            DispatchQueue.main.async {
                viewModel.onAction(.setPinnedLocation(
                    latitude: location.coordinate.latitude,
                    longitude: location.coordinate.longitude
                ))
                viewModel.onAction(.changeAddress(fullAddress))
            }
        }
    }

    init(
        cafeId: String? = nil,
        isRegistrationMode: Bool = false,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.isRegistrationMode = isRegistrationMode
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(
            wrappedValue: CafeInfoEditViewModel(
                cafeId: cafeId,
                isRegistrationMode: isRegistrationMode
            )
        )
    }
}

private struct CafeInfoEditContentView: View {
    let uiState: CafeInfoEditUiState

    let onAction: (CafeInfoEditAction) -> Void

    let onSearchAddressLocation: (String) -> Void

    let onRepresentativeImagePick: () -> Void

    let onGalleryImagePick: () -> Void

    var body: some View {
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
                if !uiState.isRegistrationMode {
                    gallerySection
                }
                locationContactSection
                businessHoursSection
            }
            .padding(16)
            .padding(.bottom, 100)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            bottomSaveBar
        }
        .background(Color(hex: "F8F5F6"))
    }

    private var basicInformationSection: some View {
        editSectionCard(title: "기본 정보") {
            ConCafeFormField(
                label: "카페명",
                text: Binding(
                    get: { uiState.cafeName },
                    set: { onAction(.changeCafeName($0)) }
                )
            )
            ConCafeFormEditor(
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
                onRepresentativeImagePick()
            } label: {
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
                        if let imageUrl = uiState.representativeImageUrl,
                           !imageUrl.isEmpty {
                            CafeInfoImageView(
                                imageUrl: imageUrl,
                                placeholder: {
                                    VStack(spacing: 8) {
                                        Image(systemName: "camera.fill")
                                            .font(.system(size: 32, weight: .semibold))
                                            .foregroundStyle(Color(hex: "8B5164"))
                                        Text(uiState.representativeImageTitle)
                                            .font(.subheadline.weight(.bold))
                                            .foregroundStyle(Color(hex: "5A4954"))
                                    }
                                },
                                loading: {
                                    ProgressView()
                                        .tint(Color(hex: "9C7A88"))
                                }
                            )
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipped()
                        } else {
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
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                }
                .frame(maxWidth: .infinity)
                .frame(height: 200)
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
                if uiState.galleryImages.count < uiState.galleryMaxCount {
                    addGalleryItem
                }
            }
        }
    }

    private var locationContactSection: some View {
        editSectionCard(title: "위치 및 연락처") {
            ConCafeFormField(
                label: "지역 / 주소",
                text: Binding(
                    get: { uiState.address },
                    set: { onAction(.changeAddress($0)) }
                ),
                trailingContent: {
                    Button {
                        onSearchAddressLocation(uiState.address)
                    } label: {
                        Image(systemName: "location.fill")
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                    .buttonStyle(.plain)
                }
            )
            ZStack(alignment: .bottomTrailing) {
                CafeInfoLocationMapView(
                    latitude: uiState.mapLatitude,
                    longitude: uiState.mapLongitude
                ) { latitude, longitude, address in
                    onAction(.setPinnedLocation(latitude: latitude, longitude: longitude))
                    if let address, !address.isEmpty {
                        onAction(.changeAddress(address))
                    }
                }
                    .frame(height: 160)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            Text("선택 좌표: \(formatCoordinate(uiState.mapLatitude)), \(formatCoordinate(uiState.mapLongitude))")
                .font(.caption)
                .foregroundStyle(Color(hex: "7E737B"))
                .frame(maxWidth: .infinity, alignment: .leading)
            ConCafeFormField(
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
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: "FFD1DC").opacity(0.2))
                .frame(height: 1)
            Button {
                onAction(.clickSave)
            } label: {
                HStack {
                    Spacer()
                    if uiState.isSaving {
                        ProgressView()
                            .progressViewStyle(.circular)
                    } else {
                        Text(uiState.submitButtonText)
                            .font(.headline.weight(.bold))
                    }
                    Spacer()
                }
                .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(hex: "FFD1DC"))
            .foregroundStyle(Color(hex: "2B2330"))
            .disabled(uiState.isSaving)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(Color.white.opacity(0.92))
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
        return GeometryReader { proxy in
            ZStack(alignment: .bottomLeading) {
                let backgroundShape = RoundedRectangle(cornerRadius: 16, style: .continuous)
                if !imageUrl.isEmpty {
                    CafeInfoImageView(
                        imageUrl: imageUrl,
                        placeholder: {
                            backgroundShape
                                .fill(
                                    LinearGradient(
                                        colors: [Color(hex: colors.0), Color(hex: colors.1)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        },
                        loading: {
                            ProgressView()
                        }
                    )
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .clipped()
                } else {
                    backgroundShape
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: colors.0), Color(hex: colors.1)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                }
                Text(label)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(.black.opacity(0.32))
                    .clipShape(Capsule())
                    .padding(10)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .aspectRatio(1, contentMode: .fit)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var addGalleryItem: some View {
        Button {
            onGalleryImagePick()
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

private struct CafeInfoImageView<Placeholder: View, Loading: View>: View {
    let imageUrl: String

    let placeholder: () -> Placeholder

    let loading: () -> Loading

    var body: some View {
        if let fileUrl = URL(string: imageUrl),
           fileUrl.isFileURL,
           let uiImage = UIImage(contentsOfFile: fileUrl.path) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
        } else if let url = URL(string: imageUrl) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    loading()
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

private func formatCoordinate(_ value: Double) -> String {
    String(format: "%.5f", value)
}

private struct CafeInfoLocationMapView: UIViewRepresentable {
    let latitude: Double

    let longitude: Double

    let onLocationSelected: (Double, Double, String?) -> Void

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView(frame: .zero)
        mapView.delegate = context.coordinator
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false

        let tapRecognizer = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleMapTap(_:))
        )
        mapView.addGestureRecognizer(tapRecognizer)
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        context.coordinator.onLocationSelected = onLocationSelected
        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let region = MKCoordinateRegion(
            center: coordinate,
            span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
        )

        mapView.setRegion(region, animated: false)
        context.coordinator.updateAnnotation(on: mapView, coordinate: coordinate)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onLocationSelected: onLocationSelected)
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var onLocationSelected: (Double, Double, String?) -> Void

        private var selectedAnnotation: MKPointAnnotation?

        private let geocoder = CLGeocoder()

        @objc func handleMapTap(_ recognizer: UITapGestureRecognizer) {
            guard let mapView = recognizer.view as? MKMapView else { return }
            let point = recognizer.location(in: mapView)
            let coordinate = mapView.convert(point, toCoordinateFrom: mapView)
            updateAnnotation(on: mapView, coordinate: coordinate)
            let location = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
            geocoder.reverseGeocodeLocation(location) { placemarks, _ in
                let first = placemarks?.first
                let address = [
                    first?.administrativeArea,
                    first?.locality,
                    first?.thoroughfare,
                    first?.subThoroughfare
                ]
                    .compactMap { $0 }
                    .joined(separator: " ")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                let normalizedAddress = address.isEmpty ? nil : address
                DispatchQueue.main.async {
                    self.onLocationSelected(
                        coordinate.latitude,
                        coordinate.longitude,
                        normalizedAddress
                    )
                }
            }
        }

        func updateAnnotation(on mapView: MKMapView, coordinate: CLLocationCoordinate2D) {
            if let selectedAnnotation {
                selectedAnnotation.coordinate = coordinate
            } else {
                let annotation = MKPointAnnotation()
                annotation.coordinate = coordinate
                mapView.addAnnotation(annotation)
                selectedAnnotation = annotation
            }
        }

        init(onLocationSelected: @escaping (Double, Double, String?) -> Void) {
            self.onLocationSelected = onLocationSelected
        }
    }
}

private enum CafeInfoImagePickTarget {
    case representative
    case gallery
}

struct CafeInfoEditView_Previews: PreviewProvider {
    static var previews: some View {
        CafeInfoEditView(cafeId: "cafe-1", onNavigationAction: { _ in })
    }
}
