//
//  MapView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import SwiftUI
import MapKit
import Shared

struct MapView: View {
    let initialRegionKey: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = MapViewModel()

    @State private var selectedPinId: String? = nil

    var body: some View {
        ZStack {
            CheckInCafeMapView(
                pins: mapPins,
                cameraRegion: resolvedMapRegion(
                    cafes: filteredCafes,
                    selectedRegion: viewModel.uiState.selectedRegion
                ),
                cameraToken: cameraToken,
                selectedPinId: $selectedPinId,
                onCafeTap: { viewModel.onAction(.cafeTapped(id: $0)) },
                onCheckInForCafeTap: { _ in },
                showsCheckInButton: false
            )
            .ignoresSafeArea()
            LinearGradient(
                colors: [Color.black.opacity(0.34), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(maxHeight: 170, alignment: .top)
            .allowsHitTesting(false)
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("\(filteredCafes.count)곳 표시 중")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.82))
                    Spacer()
                    Menu {
                        ForEach(ExploreUiState.RegionFilter.allCases, id: \.self) { region in
                            Button(region == .all ? "근처 주요 카페" : region.label) {
                                viewModel.onAction(.regionChanged(region: region))
                            }
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "mappin.and.ellipse")
                                .foregroundStyle(Color(hex: "EF6797"))
                            Text(viewModel.uiState.selectedRegion == .all ? "근처" : viewModel.uiState.selectedRegion.label)
                                .font(.caption.weight(.semibold))
                            Image(systemName: "chevron.down")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(.secondary)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(.ultraThinMaterial, in: Capsule())
                    }
                }
            }
            .padding(.top, 8)
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .compatMapNavigationBarAppearance()
        .navigationTitle("컨셉 카페 지도")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: Binding(
                get: { viewModel.uiState.searchQuery },
                set: { viewModel.onAction(.searchQueryChanged(query: $0)) }
            ),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "카페명 또는 지역 검색"
        )
        .compatSearchSuggestions(cafes: Array(filteredCafes.prefix(5))) { cafeName in
            viewModel.onAction(.searchQueryChanged(query: cafeName))
        }
        .onAppear {
            viewModel.initializeRegion(initialRegionKey)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            }
        }
    }

    private var filteredCafes: [CheckInCafeSummary] {
        let regionFiltered: [CheckInCafeSummary]
        if viewModel.uiState.selectedRegion != .all {
            regionFiltered = viewModel.uiState.mapCafes.filter {
                $0.matchesRegion(viewModel.uiState.selectedRegion)
            }
        } else {
            regionFiltered = viewModel.uiState.mapCafes.filter {
                $0.matchesNearbyCity(viewModel.uiState.userCityKey)
            }
        }

        let query = viewModel.uiState.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !query.isEmpty else { return regionFiltered }
        return regionFiltered.filter {
            $0.name.lowercased().contains(query) ||
            $0.locationLabel.lowercased().contains(query)
        }
    }

    private var mapPins: [CheckInMapPin] {
        filteredCafes.map { cafe in
            CheckInMapPin(
                id: cafe.id,
                name: cafe.name,
                latitude: cafe.geoPoint.latitude,
                longitude: cafe.geoPoint.longitude,
                isSelected: selectedPinId == cafe.id
            )
        }
    }

    private var cameraToken: String {
        let cityKey = viewModel.uiState.userCityKey ?? "all"
        let pinsKey = mapPins
            .map { "\($0.id):\($0.latitude):\($0.longitude)" }
            .joined(separator: "|")
        return "\(viewModel.uiState.selectedRegion.rawValue)#\(cityKey)#\(viewModel.uiState.searchQuery)#\(pinsKey)"
    }

    private func resolvedMapRegion(
        cafes: [CheckInCafeSummary],
        selectedRegion: ExploreUiState.RegionFilter
    ) -> MKCoordinateRegion {
        if cafes.isEmpty {
            return regionPreset(for: selectedRegion) ?? MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780),
                span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)
            )
        } else if cafes.count == 1 {
            let first = cafes[0]
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: first.geoPoint.latitude, longitude: first.geoPoint.longitude),
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
            let latitudeDelta = max(0.02, (maxLatitude - minLatitude) * 1.3)
            let longitudeDelta = max(0.02, (maxLongitude - minLongitude) * 1.3)
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
            return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780), span: MKCoordinateSpan(latitudeDelta: 0.10, longitudeDelta: 0.10))
        case .busan:
            return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 35.1796, longitude: 129.0756), span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12))
        case .daegu:
            return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 35.8714, longitude: 128.6014), span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12))
        case .tokyo:
            return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 35.6762, longitude: 139.6503), span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12))
        case .osaka:
            return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 34.6937, longitude: 135.5023), span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12))
        case .yokohama:
            return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 35.4437, longitude: 139.6380), span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12))
        }
    }
}

private extension CheckInCafeSummary {
    func matchesRegion(_ region: ExploreUiState.RegionFilter) -> Bool {
        let normalizedLocation = locationLabel.lowercased()
        return normalizedLocation.contains(region.rawValue) || normalizedLocation.contains(region.label.lowercased())
    }

    func matchesNearbyCity(_ cityKey: String?) -> Bool {
        matchesRegion(Self.nearbyRegion(for: cityKey))
    }

    private static func nearbyRegion(for cityKey: String?) -> ExploreUiState.RegionFilter {
        let normalizedCityKey = cityKey?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        return ExploreUiState.RegionFilter.allCases
            .first(where: { $0.rawValue == normalizedCityKey }) ?? .seoul
    }
}

#Preview {
    MapView(initialRegionKey: nil, onNavigationAction: { _ in })
}
