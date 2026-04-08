//
//  CheckInView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import MapKit
import UIKit
import Shared

struct CheckInView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = CheckInViewModel()

    @State private var isLocationSettingsAlertVisible = false

    var body: some View {
        ZStack {
            Color(hex: "FFFBFD")
                .ignoresSafeArea()
            Group {
                if viewModel.uiState.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.uiState.currentUser == nil {
                    CheckInGuestContentView(
                        uiState: viewModel.uiState,
                        onAction: viewModel.onAction
                    )
                } else {
                    CheckInUserContentView(
                        uiState: viewModel.uiState,
                        onAction: viewModel.onAction
                    )
                }
            }
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isLoginPromptVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissLoginPrompt)
                    }
                }
            )
        ) {
            CheckInLoginPromptSheet(onAction: viewModel.onAction)
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isNewVisitSheetVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissNewVisitSheet)
                    }
                }
            )
        ) {
            CheckInNewVisitSheet(
                cafes: viewModel.uiState.mapCafes,
                preselectCafeId: viewModel.uiState.preselectCafeId,
                errorMessage: Binding(
                    get: { viewModel.uiState.errorMessage },
                    set: { value in
                        if value == nil {
                            viewModel.onAction(.dismissError)
                        }
                    }
                ),
                onAction: viewModel.onAction
            )
            .compatLargeSheetDetent()
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.reviewPrompt != nil },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissReviewPrompt)
                    }
                }
            )
        ) {
            if let prompt = viewModel.uiState.reviewPrompt {
                CheckInReviewPromptSheet(
                    cafeName: prompt.cafeName,
                    onAction: viewModel.onAction
                )
                .compatLargeSheetDetent()
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToReviewEdit(let cafeId):
                onNavigationAction(.navigateToReviewEdit(cafeId: cafeId))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            case .openLocationSettings:
                isLocationSettingsAlertVisible = true
            }
        }
        .alert(
            String(localized: String.LocalizationValue("checkin_location_permission_title"), table: "Localizable"),
            isPresented: $isLocationSettingsAlertVisible
        ) {
            Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"), role: .cancel) {}
            Button(String(localized: String.LocalizationValue("checkin_location_permission_open_settings"), table: "Localizable")) {
                openLocationSettings()
            }
        } message: {
            Text(String(localized: String.LocalizationValue("checkin_location_permission_desc"), table: "Localizable"))
        }
    }

    private func openLocationSettings() {
        let settingsUrlString = UIApplication.openSettingsURLString
        let settingsUrl = URL(string: settingsUrlString)

        if settingsUrl != nil {
            UIApplication.shared.open(settingsUrl!)
        }
    }
}

private struct CheckInGuestContentView: View {
    let uiState: CheckInUiState

    let onAction: (CheckInAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                CheckInMapSection(
                    currentLocationLabel: uiState.currentLocationLabel,
                    cafes: uiState.mapCafes,
                    userCityKey: uiState.userCityKey,
                    onCafeTap: { onAction(.cafeTapped(id: $0)) },
                    onCheckInForCafeTap: { onAction(.checkInForCafeTapped(cafeId: $0)) },
                    onCheckInTap: { onAction(.checkInTapped) }
                )
                .padding(.top, 16)
                CheckInLoginPromotionSection(onAction: onAction)
                CheckInSectionTitle(
                    title: String(localized: String.LocalizationValue("checkin_section_popular_cafe_title"), table: "Localizable"),
                    trailing: nil,
                    leadingSystemImage: "flame.fill"
                )
                if !uiState.popularCafes.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 14) {
                            ForEach(uiState.popularCafes, id: \.id) { cafe in
                                CafeSummaryCard(
                                    name: cafe.name,
                                    rating: String(format: "%.1f", cafe.rating),
                                    conceptType: nil,
                                    location: cafe.locationLabel,
                                    thumbnailImage: cafe.thumbnailImage,
                                    showLocationIcon: false,
                                    trailingLabel: String(format: String(localized: String.LocalizationValue("checkin_count_label"), table: "Localizable"), locale: Locale.current, cafe.checkInCount),
                                    onTap: { onAction(.cafeTapped(id: cafe.id)) }
                                )
                                .frame(width: 220)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                } else {
                    CheckInSectionPlaceholderCard(
                        title: String(localized: String.LocalizationValue("checkin_popular_cafe_empty_title"), table: "Localizable"),
                        description: String(localized: String.LocalizationValue("checkin_popular_cafe_empty_desc"), table: "Localizable")
                    )
                    .padding(.horizontal, 16)
                }
                CheckInSectionTitle(
                    title: String(localized: String.LocalizationValue("checkin_section_popular_cast_title"), table: "Localizable"),
                    trailing: nil,
                    leadingSystemImage: "cup.and.saucer.fill"
                )
                if !uiState.popularCasts.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(uiState.popularCasts, id: \.id) { cast in
                                CheckInCastCard(cast: cast) {
                                    onAction(.castTapped(id: cast.id))
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 8)
                } else {
                    CheckInSectionPlaceholderCard(
                        title: String(localized: String.LocalizationValue("checkin_popular_cast_empty_title"), table: "Localizable"),
                        description: String(localized: String.LocalizationValue("checkin_popular_cast_empty_desc"), table: "Localizable")
                    )
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
                }
                if let errorMessage = uiState.errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                }
            }
        }
    }
}

private struct CheckInUserContentView: View {
    let uiState: CheckInUiState

    let onAction: (CheckInAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                CheckInMapSection(
                    currentLocationLabel: uiState.currentLocationLabel,
                    cafes: uiState.mapCafes,
                    userCityKey: uiState.userCityKey,
                    onCafeTap: { onAction(.cafeTapped(id: $0)) },
                    onCheckInForCafeTap: { onAction(.checkInForCafeTapped(cafeId: $0)) },
                    onCheckInTap: { onAction(.checkInTapped) }
                )
                .padding(.top, 16)
                CheckInPrimaryButton(title: String(localized: String.LocalizationValue("checkin_new_visit_cta"), table: "Localizable")) {
                    onAction(.checkInTapped)
                }
                .padding(.horizontal, 64)
                .padding(.vertical, 12)
                CheckInSectionTitle(
                    title: String(localized: String.LocalizationValue("checkin_section_today_visit_title"), table: "Localizable"),
                    trailing: TimeUtils.currentMonthDayLabelKorean(),
                    leadingSystemImage: nil
                )
                CheckInTodayVisitsRow(visits: uiState.todayVisits)
                Spacer()
                    .frame(height: 20)
                CheckInSectionTitle(
                    title: String(localized: String.LocalizationValue("checkin_section_timeline_title"), table: "Localizable"),
                    trailing: nil,
                    leadingSystemImage: "clock.fill"
                )
                CheckInTimelineList(
                    visits: uiState.recentVisits,
                    canLoadMore: uiState.canLoadMoreRecentVisits,
                    isLoadingMore: uiState.isLoadingMoreRecentVisits,
                    onLoadMore: { onAction(.loadMoreRecentVisits) }
                )
            }
        }
    }
}

private struct CheckInMapSection: View {
    let currentLocationLabel: String

    let cafes: [CheckInCafeSummary]

    let userCityKey: String?

    let onCafeTap: (String) -> Void

    let onCheckInForCafeTap: (String) -> Void

    let onCheckInTap: () -> Void

    @State private var mapRegion = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780),
        span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)
    )

    @State private var selectedRegion: ExploreUiState.RegionFilter = .all

    @State private var selectedPinId: String? = nil

    var body: some View {
        VStack(spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: String.LocalizationValue("checkin_map_title"), table: "Localizable"))
                        .font(.headline)
                        .fontWeight(.bold)
                    Menu {
                        ForEach(ExploreUiState.RegionFilter.allCases, id: \.self) { region in
                            Button(region == .all ? String(localized: String.LocalizationValue("checkin_nearby_label"), table: "Localizable") : region.label) {
                                selectedRegion = region
                            }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "mappin.and.ellipse")
                                .foregroundStyle(Color(hex: "EF6797"))
                            Text(
                                String(
                                    format: String(localized: String.LocalizationValue("checkin_main_cafe_label"), table: "Localizable"),
                                    locale: Locale.current,
                                    (selectedRegion == .all ? String(localized: String.LocalizationValue("checkin_nearby_label"), table: "Localizable") : selectedRegion.label)
                                )
                            )
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Color(hex: "7B7480"))
                            Image(systemName: "chevron.down")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(Color(hex: "7B7480"))
                        }
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
                Button(String(localized: String.LocalizationValue("checkin_button"), table: "Localizable"), action: onCheckInTap)
                    .buttonStyle(.bordered)
                    .tint(Color(hex: "EF6797"))
            }
            GeometryReader { _ in
                Map(
                    coordinateRegion: $mapRegion,
                    annotationItems: mapPins
                ) { pin in
                    MapAnnotation(
                        coordinate: CLLocationCoordinate2D(
                            latitude: pin.latitude,
                            longitude: pin.longitude
                        ),
                        anchorPoint: CGPoint(x: 0.5, y: 1.0)
                    ) {
                        VStack(spacing: 0) {
                            if selectedPinId == pin.id {
                                HStack(spacing: 4) {
                                    Button {
                                        selectedPinId = nil
                                        onCafeTap(pin.id)
                                    } label: {
                                        Text(pin.name)
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(Color(hex: "2B2330"))
                                            .lineLimit(1)
                                    }
                                    .buttonStyle(.plain)
                                    Button {
                                        selectedPinId = nil
                                        onCheckInForCafeTap(pin.id)
                                    } label: {
                                        Image(systemName: "checkmark.circle")
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(Color(hex: "EF6797"))
                                    }
                                    .buttonStyle(.plain)
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.96))
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .shadow(color: .black.opacity(0.12), radius: 6, y: 2)
                            }
                            Button {
                                if selectedPinId == pin.id {
                                    selectedPinId = nil
                                } else {
                                    selectedPinId = pin.id
                                }
                            } label: {
                                Circle()
                                    .fill(Color(hex: "EF6797"))
                                    .frame(width: 12, height: 12)
                                    .shadow(color: .black.opacity(0.15), radius: 3, y: 1)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .onAppear {
                    mapRegion = resolvedMapRegion(cafes: filteredCafes, selectedRegion: selectedRegion)
                }
                .onChange(of: cafes.count) { _ in
                    mapRegion = resolvedMapRegion(cafes: filteredCafes, selectedRegion: selectedRegion)
                }
                .onChange(of: selectedRegion) { region in
                    mapRegion = resolvedMapRegion(cafes: filteredCafes, selectedRegion: region)
                }
                .onChange(of: userCityKey) { _ in
                    mapRegion = resolvedMapRegion(cafes: filteredCafes, selectedRegion: selectedRegion)
                }
            }
            .frame(height: 240)
        }
        .padding(12)
        .padding(.vertical, 6)
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF0F6"), Color(hex: "FFFAFC"), Color(hex: "FFF3F8")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(Color.white.opacity(0.8), lineWidth: 1)
        )
        .padding(.horizontal, 16)
    }

    private var filteredCafes: [CheckInCafeSummary] {
        if selectedRegion != .all {
            return cafes.filter { $0.locationLabel.lowercased().contains(selectedRegion.rawValue) }
        } else if let cityKey = userCityKey {
            return cafes.filter { $0.locationLabel.lowercased().contains(cityKey) }
        } else {
            return cafes
        }
    }

    private var mapPins: [CheckInMapPin] {
        return filteredCafes.map { cafe in
            CheckInMapPin(
                id: cafe.id,
                name: cafe.name,
                latitude: cafe.geoPoint.latitude,
                longitude: cafe.geoPoint.longitude
            )
        }
    }

    private func resolvedMapRegion(
        cafes: [CheckInCafeSummary],
        selectedRegion: ExploreUiState.RegionFilter
    ) -> MKCoordinateRegion {
        if let regionPreset = regionPreset(for: selectedRegion) {
            return regionPreset
        } else if cafes.isEmpty {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780),
                span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)
            )
        } else if cafes.count == 1 {
            let first = cafes[0]
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(
                    latitude: first.geoPoint.latitude,
                    longitude: first.geoPoint.longitude
                ),
                span: MKCoordinateSpan(latitudeDelta: 0.03, longitudeDelta: 0.03)
            )
        } else {
            let latitudes = cafes.map { $0.geoPoint.latitude }
            let longitudes = cafes.map { $0.geoPoint.longitude }
            let minLatitude = latitudes.min() ?? 37.5
            let maxLatitude = latitudes.max() ?? 37.6
            let minLongitude = longitudes.min() ?? 126.9
            let maxLongitude = longitudes.max() ?? 127.1
            let centerLatitude = (minLatitude + maxLatitude) / 2.0
            let centerLongitude = (minLongitude + maxLongitude) / 2.0
            let latitudeDelta = max(0.03, (maxLatitude - minLatitude) * 1.7)
            let longitudeDelta = max(0.03, (maxLongitude - minLongitude) * 1.7)
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: centerLatitude, longitude: centerLongitude),
                span: MKCoordinateSpan(latitudeDelta: latitudeDelta, longitudeDelta: longitudeDelta)
            )
        }
    }

    private func regionPreset(for region: ExploreUiState.RegionFilter) -> MKCoordinateRegion? {
        switch region {
        case .all:
            return nil
        case .seoul:
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780),
                span: MKCoordinateSpan(latitudeDelta: 0.10, longitudeDelta: 0.10)
            )
        case .busan:
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 35.1796, longitude: 129.0756),
                span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12)
            )
        case .daegu:
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 35.8714, longitude: 128.6014),
                span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12)
            )
        case .tokyo:
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 35.6762, longitude: 139.6503),
                span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12)
            )
        case .osaka:
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 34.6937, longitude: 135.5023),
                span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12)
            )
        }
    }

}

private struct CheckInMapPin: Identifiable {
    let id: String
    let name: String
    let latitude: Double
    let longitude: Double
}

private struct CheckInLoginPromotionSection: View {
    let onAction: (CheckInAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Image(systemName: "sparkles")
                        .foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("checkin_login_promo_title"), table: "Localizable"))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                }
                Text(String(localized: String.LocalizationValue("checkin_login_required_desc"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.9))
                VStack(alignment: .leading, spacing: 6) {
                    Text(String(localized: String.LocalizationValue("checkin_login_promo_feature_visit"), table: "Localizable")).foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("checkin_login_promo_feature_fan_level"), table: "Localizable")).foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("checkin_login_promo_feature_badge"), table: "Localizable")).foregroundStyle(.white)
                }
                .font(.caption)
            }
            Button(String(localized: String.LocalizationValue("signin_submit"), table: "Localizable")) {
                onAction(.signInTapped)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color.white)
            .foregroundStyle(Color(hex: "EF6797"))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .font(.subheadline.weight(.bold))
        }
        .padding(18)
        .background(
            LinearGradient(
                colors: [Color(hex: "EF6797"), Color(hex: "F7A0C1")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .padding(.horizontal, 16)
    }
}

private struct CheckInCastCard: View {
    let cast: CheckInCastSummary

    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFD1E2"), Color(hex: "FFEAF2")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    Text(String(cast.name.prefix(1)))
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(Color(hex: "B74C72"))
                    if let rawImageUrl = cast.profileImage?.trimmingCharacters(in: .whitespacesAndNewlines),
                       !rawImageUrl.isEmpty,
                       let imageUrl = URL(string: rawImageUrl) {
                        CachedAsyncImage(
                            url: imageUrl,
                            placeholder: Color.clear
                        )
                    }
                }
                .frame(width: 56, height: 56)
                .clipShape(Circle())
                .clipped()
                VStack(alignment: .leading, spacing: 4) {
                    Text(cast.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.primary)
                        .lineLimit(1)
                    Text(cast.cafeName)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "7A7380"))
                        .lineLimit(1)
                }
            }
            HStack {
                Text(String(format: String(localized: String.LocalizationValue("checkin_today_visit_count"), table: "Localizable"), locale: Locale.current, cast.todayVisit))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "EF6797"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(hex: "FFEEF5"))
                    .clipShape(Capsule())
                Spacer(minLength: 0)
            }
        }
        .padding(16)
        .frame(width: 200, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: .black.opacity(0.03), radius: 8, y: 3)
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

private struct CheckInTodayVisitsRow: View {
    let visits: [CheckInVisitEntry]

    var body: some View {
        if visits.isEmpty {
            CheckInEmptyState(
                title: String(localized: String.LocalizationValue("checkin_today_visit_empty_title"), table: "Localizable"),
                description: String(localized: String.LocalizationValue("checkin_today_visit_empty_desc"), table: "Localizable")
            )
        } else {
            HStack(spacing: 12) {
                switch visits.count {
                case 1:
                    CheckInVisitCard(name: visits[0].cafeName, time: visits[0].visitedLabel, image: visits[0].cafeImage)
                    Spacer(minLength: 0)
                default:
                    CheckInVisitCard(name: visits[0].cafeName, time: visits[0].visitedLabel, image: visits[0].cafeImage)
                    CheckInMoreVisitCard(remainingCount: visits.count - 1)
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct CheckInVisitCard: View {
    let name: String
   
    let time: String
   
    let image: String

    private let cornerRadius: CGFloat = 24

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size

            ZStack(alignment: .bottomLeading) {
                CachedAsyncImage(
                    url: resolvedRemoteImageUrl(image),
                    placeholder: placeholder
                )
                .frame(width: size.width, height: size.height)
                .clipped()
                gradientOverlay
                textSection
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
        .frame(height: 180)
    }

    private var placeholder: some View {
        LinearGradient(
            colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var gradientOverlay: some View {
        LinearGradient(
            colors: [
                Color.black.opacity(0.0),
                Color.black.opacity(0.45)
            ],
            startPoint: .center,
            endPoint: .bottom
        )
    }

    private var textSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(name)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
                .lineLimit(2)
            Text(time)
                .font(.caption)
                .foregroundStyle(Color.white.opacity(0.8))
                .lineLimit(1)
        }
        .padding(16)
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }
}

private struct CheckInMoreVisitCard: View {
    let remainingCount: Int

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFF1F6"), Color(hex: "FFE1EC")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            VStack(spacing: 6) {
                Text("+\(remainingCount)")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(Color(hex: "EF6797"))
                Text(String(localized: String.LocalizationValue("checkin_more_visit_label"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7C7480"))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 180)
    }
}

private struct CheckInPrimaryButton: View {
    let title: String

    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                Text(title)
                    .fontWeight(.bold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color(hex: "EF6797"))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct CheckInTimelineList: View {
    let visits: [CheckInVisitEntry]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onLoadMore: () -> Void

    var body: some View {
        if visits.isEmpty {
            CheckInEmptyState(
                title: String(localized: String.LocalizationValue("checkin_timeline_empty_title"), table: "Localizable"),
                description: String(localized: String.LocalizationValue("checkin_timeline_empty_desc"), table: "Localizable")
            )
        } else {
            VStack(spacing: 12) {
                ForEach(Array(visits.enumerated()), id: \.element.id) { index, visit in
                    CheckInTimelineItem(
                        visit: visit,
                        showsConnector: index < visits.count - 1
                    )
                }
                if isLoadingMore {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                } else if canLoadMore {
                    Button(String(localized: String.LocalizationValue("checkin_load_more_visits"), table: "Localizable")) {
                        onLoadMore()
                    }
                    .font(.caption.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct CheckInTimelineItem: View {
    let visit: CheckInVisitEntry

    let showsConnector: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(spacing: 0) {
                Circle()
                    .fill(Color.white)
                    .frame(width: 32, height: 32)
                    .overlay(
                        Circle()
                            .stroke(Color(hex: "F6BCD1"), lineWidth: 1)
                    )
                    .overlay(
                        Image(systemName: "mappin")
                            .foregroundStyle(Color(hex: "4E4750"))
                    )
                if showsConnector {
                    Rectangle()
                        .fill(Color(hex: "F6BCD1").opacity(0.3))
                        .frame(width: 2, height: 100)
                }
            }
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top) {
                    Text(visit.cafeName)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "4E4750"))
                    Spacer(minLength: 8)
                    Text(visit.relativeVisitedLabel)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(hex: "F5F5F5"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                Text(visit.memo ?? String(localized: String.LocalizationValue("checkin_visit_memo_empty"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: .black.opacity(0.03), radius: 8, y: 3)
            .padding(.bottom, 24)
        }
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        if trimmed.isEmpty {
            return nil
        } else {
            return URL(string: trimmed)
        }
    }
}

private struct CheckInEmptyState: View {
    let title: String

    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
            Text(description)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .padding(.horizontal, 16)
    }
}

private struct CheckInSectionTitle: View {
    let title: String

    let trailing: String?

    let leadingSystemImage: String?

    var body: some View {
        HStack {
            HStack(spacing: 6) {
                if let leadingSystemImage {
                    Image(systemName: leadingSystemImage)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color(hex: "7B7480"))
                }
                if let trailing {
                    Text(trailing)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "7B7480"))
                }
                Text(title)
                    .font(.headline.weight(.bold))
            }
            Spacer()
        }
        .padding(.horizontal, 16)
    }
}

private struct CheckInLoginPromptSheet: View {
    let onAction: (CheckInAction) -> Void

    var body: some View {
        VStack(spacing: 18) {
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .fill(Color(hex: "E1D7DE"))
                .frame(width: 42, height: 5)
                .padding(.top, 8)
            VStack(spacing: 10) {
                Text(String(localized: String.LocalizationValue("checkin_login_required_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                Text(String(localized: String.LocalizationValue("checkin_login_required_desc"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 12)
            }
            Button(String(localized: String.LocalizationValue("signin_submit"), table: "Localizable")) {
                onAction(.signInTapped)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color(hex: "FFD1DC"))
            .foregroundStyle(Color(hex: "2B2330"))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .font(.subheadline.weight(.bold))
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
        .background(Color.white)
    }
}

private struct CheckInReviewPromptSheet: View {
    let cafeName: String
   
    let onAction: (CheckInAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(String(localized: String.LocalizationValue("checkin_review_prompt_title"), table: "Localizable"))
                .font(.title3.weight(.bold))
                .foregroundColor(Color(hex: "24161E"))
            Text(String(format: String(localized: String.LocalizationValue("checkin_review_prompt_desc"), table: "Localizable"), locale: Locale.current, cafeName))
                .font(.subheadline)
                .foregroundColor(Color(hex: "6F6670"))
            Button {
                onAction(.writeReviewPromptTapped)
            } label: {
                Text(String(localized: String.LocalizationValue("checkin_review_prompt_primary"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundColor(Color(hex: "2B2330"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            .buttonStyle(PlainButtonStyle())
            Button {
                onAction(.dismissReviewPrompt)
            } label: {
                Text(String(localized: String.LocalizationValue("checkin_review_prompt_later"), table: "Localizable"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(PlainButtonStyle())
        }
        .padding(20)
    }
}

private struct CheckInNewVisitSheet: View {
    let cafes: [CheckInCafeSummary]

    var preselectCafeId: String? = nil

    @Binding var errorMessage: String?

    let onAction: (CheckInAction) -> Void

    @State private var selectedCafeId: String?

    @State private var visitDate = Date()

    @State private var visitTime = Date()

    @State private var isTimePickerPresented = false

    @State private var memo = ""

    private var selectedCafeName: String {
        cafes.first(where: { $0.id == selectedCafeId })?.name ?? cafes.first?.name ?? ""
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: 16) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(Color(hex: "E1D7DE"))
                        .frame(width: 42, height: 5)
                    HStack {
                        Spacer()
                        Text(String(localized: String.LocalizationValue("checkin_new_visit_title"), table: "Localizable"))
                            .font(.title3.weight(.bold))
                        Spacer()
                        Button {
                            onAction(.dismissNewVisitSheet)
                        } label: {
                            Image(systemName: "xmark")
                                .foregroundStyle(Color(hex: "7C7480"))
                                .padding(4)
                        }
                    }
                    Text(String(localized: String.LocalizationValue("checkin_new_visit_desc"), table: "Localizable"))
                        .font(.footnote)
                        .foregroundStyle(Color(hex: "7C7480"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    VStack(spacing: 10) {
                        if cafes.isEmpty {
                            VStack(spacing: 6) {
                                Text(String(localized: String.LocalizationValue("checkin_new_visit_no_cafe"), table: "Localizable"))
                                    .font(.footnote)
                                    .foregroundStyle(Color(hex: "7C7480"))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                ConCafeFormField(
                                    label: String(localized: String.LocalizationValue("checkin_new_visit_cafe_label"), table: "Localizable"),
                                    text: .constant(""),
                                    placeholder: String(localized: String.LocalizationValue("checkin_new_visit_cafe_unavailable_placeholder"), table: "Localizable"),
                                    isEditable: false
                                )
                            }
                        } else {
                            ZStack {
                                Menu {
                                    ForEach(cafes, id: \.id) { cafe in
                                        Button(cafe.name) {
                                            selectedCafeId = cafe.id
                                        }
                                    }
                                } label: {
                                    ConCafeFormField(
                                        label: String(localized: String.LocalizationValue("checkin_new_visit_cafe_label"), table: "Localizable"),
                                        text: .constant(selectedCafeName),
                                        placeholder: String(localized: String.LocalizationValue("checkin_new_visit_cafe_placeholder"), table: "Localizable"),
                                        isEditable: false,
                                        trailingContent: {
                                            Image(systemName: "chevron.down")
                                                .font(.caption.weight(.semibold))
                                                .foregroundStyle(Color(hex: "7C7480"))
                                        }
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        ZStack {
                            ConCafeFormField(
                                label: String(localized: String.LocalizationValue("checkin_new_visit_time_label"), table: "Localizable"),
                                text: .constant(TimeUtils.formatHourMinute(visitTime)),
                                placeholder: String(localized: String.LocalizationValue("checkin_new_visit_time_placeholder"), table: "Localizable"),
                                isEditable: false,
                                trailingContent: {
                                    Image(systemName: "clock")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(Color(hex: "7C7480"))
                                }
                            )
                            Button(action: { isTimePickerPresented = true }) {
                                Color.clear
                            }
                            .buttonStyle(.plain)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                        ConCafeFormEditor(
                            label: String(localized: String.LocalizationValue("checkin_new_visit_memo_label"), table: "Localizable"),
                            text: $memo,
                            placeholder: String(localized: String.LocalizationValue("checkin_new_visit_memo_placeholder"), table: "Localizable")
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 170)
                .padding(.top, 10)
                .background(Color.white)
            }
            VStack(spacing: 8) {
                if let errorMessage, !errorMessage.isEmpty {
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Color(hex: "E25575"))
                            .padding(.top, 2)
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(Color(hex: "B03854"))
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(hex: "FFF1F3"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Color(hex: "FFCDD5"), lineWidth: 1)
                    )
                }
                Button(String(localized: String.LocalizationValue("checkin_new_visit_submit"), table: "Localizable")) {
                    guard let cafeId = selectedCafeId else { return }
                    let normalizedMemo = memo.trimmingCharacters(in: .whitespacesAndNewlines)

                    onAction(
                        .submitNewVisit(
                            cafeId: cafeId,
                            visitedAt: makeVisitedAtString(date: visitDate, time: visitTime),
                            memo: normalizedMemo.isEmpty ? nil : normalizedMemo
                        )
                    )
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "FFD1DC"))
                .foregroundStyle(Color(hex: "2B2330"))
                .font(.headline.weight(.bold))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .disabled(selectedCafeId == nil)
                .padding(.bottom, 8)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 10)
            .padding(.top, 10)
            .background(Color.white)
            .overlay(
                Rectangle()
                    .fill(Color(hex: "EEE4EA"))
                    .frame(height: 1),
                alignment: .top
            )
            Spacer()
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .background(Color(hex: "F8F5F6"))
        .sheet(isPresented: $isTimePickerPresented) {
            CompatNavigationContainer(title: String(localized: String.LocalizationValue("checkin_new_visit_time_picker_title"), table: "Localizable")) {
                VStack {
                    DatePicker(
                        String(localized: String.LocalizationValue("checkin_new_visit_time_label"), table: "Localizable"),
                        selection: $visitTime,
                        displayedComponents: .hourAndMinute
                    )
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .padding()
                    Spacer()
                }
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable")) {
                        isTimePickerPresented = false
                    }
                }
            }
            .compatFractionSheetDetent(0.35)
        }
    }

    init(
        cafes: [CheckInCafeSummary],
        preselectCafeId: String? = nil,
        errorMessage: Binding<String?>,
        onAction: @escaping (CheckInAction) -> Void
    ) {
        self.cafes = cafes
        self.preselectCafeId = preselectCafeId
        self._errorMessage = errorMessage
        self.onAction = onAction
        let initialId = preselectCafeId.flatMap { id in cafes.first(where: { $0.id == id })?.id } ?? cafes.first?.id
        _selectedCafeId = State(initialValue: initialId)
    }

    private func makeVisitedAtString(date: Date, time: Date) -> String {
        return TimeUtils.makeVisitedAtString(date: date, time: time)
    }
}

private struct CheckInSectionPlaceholderCard: View {
    let title: String

    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Color(hex: "5B4F57"))
            Text(description)
            .font(.caption)
            .foregroundStyle(Color(hex: "857A82"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(hex: "FFF2F7"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private extension CheckInVisitEntry {
    var relativeVisitedLabel: String {
        return TimeUtils.relativeVisitedLabel(
            visitedAt: visitedAt,
            visitedLabel: visitedLabel,
            referenceDate: TimeUtils.currentIsoDate()
        )
    }
}

struct CheckInView_Previews: PreviewProvider {
    static var previews: some View {
        CheckInView(onNavigationAction: { _ in })
    }
}
