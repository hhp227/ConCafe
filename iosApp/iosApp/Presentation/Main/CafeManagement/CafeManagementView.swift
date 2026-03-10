//
//  CafeManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import Shared

struct CafeManagementView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = CafeManagementViewModel()

    var body: some View {
        CafeManagementContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCafeDashboard(let cafeId):
                onNavigationAction(.navigateToCafeDashboard(id: cafeId))
            case .navigateToCafe(let cafeId):
                onNavigationAction(.navigateToCafe(id: cafeId))
            }
        }
    }
}

private struct CafeManagementContentView: View {
    let uiState: CafeManagementUiState

    let onAction: (CafeManagementAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                heroCard
                if let infoMessage = uiState.infoMessage {
                    infoBanner(message: infoMessage)
                }
                if uiState.hasOwnedCafes {
                    sectionHeader(
                        title: "내 카페",
                        subtitle: "카페를 탭하면 운영 대시보드 상세 화면으로 이동합니다"
                    )
                    VStack(spacing: 12) {
                        ForEach(uiState.visibleOwnedCafes, id: \.id) { cafe in
                            ownedCafeCard(cafe: cafe)
                        }
                    }
                    if uiState.hasHiddenOwnedCafes {
                        expandOwnedCafeButton
                    }
                    if !uiState.pendingClaims.isEmpty {
                        sectionHeader(
                            title: "운영자 신청 상태",
                            subtitle: "기존 카페 연결 요청 현황"
                        )
                        VStack(spacing: 12) {
                            ForEach(Array(uiState.pendingClaims.enumerated()), id: \.offset) { _, claim in
                                pendingClaimCard(claim: claim)
                            }
                        }
                    }
                } else {
                    searchCafeSection
                    emptyStateCard
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 32)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF7FB"), Color(hex: "FFEEF6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Cafe Manage")
                .font(.title3.weight(.bold))
                .foregroundStyle(.white)
            Text(
                uiState.featuredCafe != nil
                    ? "운영 중인 카페를 확인하고 각 카페의 관리 화면으로 이동할 수 있습니다."
                    : "운영 카페 연결 상태를 확인하고 기존 카페 검색 또는 새 카페 등록을 시작하세요."
            )
            .font(.subheadline)
            .foregroundStyle(.white.opacity(0.9))
            Text("운영 카페 \(uiState.ownedCafes.count)개")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(Color.white.opacity(0.18))
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(
            LinearGradient(
                colors: [Color(hex: "2F1B3A"), Color(hex: "7C3F67"), Color(hex: "F06A9D")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "6B5320"))
            }
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

    private func sectionHeader(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(subtitle)
                .font(.caption)
                .foregroundStyle(Color(hex: "786E7A"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func ownedCafeCard(cafe: CafeManagementData.OwnedCafeSummary) -> some View {
        ZStack(alignment: .trailing) {
            Button {
                onAction(.clickCafe(cafe.id))
            } label: {
                ZStack(alignment: .bottomLeading) {
                    LinearGradient(
                        colors: cafe.isApproved
                            ? [Color(hex: "2F1B3A"), Color(hex: "7C3F67"), Color(hex: "F06A9D")]
                            : [Color(hex: "3A3240"), Color(hex: "6F6272"), Color(hex: "B8A8B2")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    LinearGradient(
                        colors: [.clear, Color.black.opacity(0.14), Color.black.opacity(0.52)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    VStack(alignment: .leading, spacing: 6) {
                        Text(cafe.name)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                        Text(cafe.city)
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.88))
                            .lineLimit(1)
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(maxWidth: .infinity)
                .aspectRatio(1.8, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .buttonStyle(.plain)
            Button {
                onAction(.clickCafeDetail(cafe.id))
            } label: {
                Image(systemName: "chevron.right")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .padding(.trailing, 10)
        }
    }

    private var expandOwnedCafeButton: some View {
        Button {
            onAction(.toggleCafeListExpanded)
        } label: {
            HStack {
                Text(
                    uiState.isShowingAllCafes
                        ? "카페 목록 접기"
                        : "나머지 카페 \((uiState.ownedCafes.count - uiState.visibleOwnedCafes.count))개 더 보기"
                )
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "5E4F5D"))
                Spacer()
                Image(systemName: uiState.isShowingAllCafes ? "chevron.up" : "chevron.down")
                    .foregroundStyle(Color(hex: "7C6B79"))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(hex: "F7F2F6"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "E5DCE5"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var searchCafeSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader(
                title: "기존 카페 검색",
                subtitle: "기등록되어있는 카페를 검색해서 등록할수 있습니다."
            )
            HStack(spacing: 12) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color(hex: "8E8794"))
                TextField(
                    "카페 이름 또는 지역 검색",
                    text: Binding(
                        get: { uiState.cafeSearchQuery },
                        set: { onAction(.changeCafeSearchQuery($0)) }
                    )
                )
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color(hex: "E4DDE5"), lineWidth: 1)
            )
            if !uiState.cafeSearchQuery.isEmpty {
                VStack(spacing: 0) {
                    if uiState.filteredSearchableCafes.isEmpty {
                        Text("검색 결과가 없습니다")
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "8E8794"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 20)
                    } else {
                        ForEach(Array(uiState.filteredSearchableCafes.enumerated()), id: \.element.id) { index, cafe in
                            searchCafeItem(cafe: cafe)

                            if index < uiState.filteredSearchableCafes.count - 1 {
                                Divider()
                                    .overlay(Color(hex: "F1EAF1"))
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color(hex: "E4DDE5"), lineWidth: 1)
                )
            }
        }
    }

    private func searchCafeItem(cafe: CafeManagementData.SearchableCafeSummary) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(cafe.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "2B2330"))
                Text(cafe.location)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "8E8794"))
            }
            Spacer()
            Button("등록") {
                onAction(.clickClaimCafe(cafe.id))
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 14)
            .frame(height: 38)
            .background(Color(hex: "EF6797"))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    private var emptyStateCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color(hex: "FCE6EF"))
                    .frame(width: 54, height: 54)
                Image(systemName: "building.2.crop.circle")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            Text("아직 연결된 운영 카페가 없습니다")
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text("검색으로 기존 카페를 찾거나 새 카페를 등록해 운영 권한을 연결하세요.")
                .font(.subheadline)
                .foregroundStyle(Color(hex: "786E7A"))
            Button("새 카페 등록") {
                onAction(.clickCreateCafe)
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Color(hex: "6A5666"))
            .padding(.horizontal, 16)
            .frame(height: 44)
            .background(Color(hex: "F6EDF4"))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            if !uiState.pendingClaims.isEmpty {
                Text("운영자 신청 상태")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))

                ForEach(Array(uiState.pendingClaims.enumerated()), id: \.offset) { _, claim in
                    pendingClaimCard(claim: claim)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func pendingClaimCard(claim: CafeManagementData.PendingClaimSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(claim.cafeName)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))
                Spacer()
                ZStack {
                    Circle()
                        .fill(Color(hex: "FFE8B8"))
                        .frame(width: 30, height: 30)
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color(hex: "9A6A11"))
                }
            }
            Text("\(claim.status) · \(claim.requestedAt)")
                .font(.caption)
                .foregroundStyle(Color(hex: "8B774C"))
            Text(claim.message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6E6248"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color(hex: "FFF8EA"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "F0DEB1"), lineWidth: 1)
        )
    }
}

struct CafeManagementView_Previews: PreviewProvider {
    static var previews: some View {
        CafeManagementView(onNavigationAction: { _ in })
    }
}
