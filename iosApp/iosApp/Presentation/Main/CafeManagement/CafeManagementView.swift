//
//  CafeManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct CafeManagementView: View {
    @StateObject private var viewModel = CafeManagementViewModel()

    var body: some View {
        CafeManagementContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { _ in
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
                        subtitle: "운영 중인 카페를 선택해 대시보드를 전환하세요"
                    )
                    ownedCafeList

                    if let selectedCafe = uiState.selectedCafe {
                        sectionHeader(
                            title: "운영 대시보드",
                            subtitle: selectedCafe.name
                        )
                        dashboardGrid(cafe: selectedCafe)
                        shortcutGrid
                        cafeOperationsCard(cafe: selectedCafe)
                    }
                } else {
                    emptyStateCard
                }

                if !uiState.pendingClaims.isEmpty && uiState.hasOwnedCafes {
                    sectionHeader(
                        title: "운영자 신청 상태",
                        subtitle: "기존 카페 연결 요청 현황"
                    )
                    VStack(spacing: 12) {
                        ForEach(uiState.pendingClaims) { claim in
                            pendingClaimCard(claim: claim)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 32)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF7FB"), Color(hex: "FFEEF6"), Color(hex: "FFFBFD")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Cafe Manage")
                .font(.title3.weight(.bold))
                .foregroundStyle(.white)

            Text(
                uiState.selectedCafe != nil
                    ? "\(uiState.selectedCafe!.name) 운영 현황을 확인하고 주요 관리 화면으로 이동할 수 있습니다."
                    : "운영 카페 연결 상태를 확인하고 기존 카페 검색 또는 새 카페 등록을 시작하세요."
            )
            .font(.subheadline)
            .foregroundStyle(.white.opacity(0.92))

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

    private var ownedCafeList: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(uiState.ownedCafes) { cafe in
                    ownedCafeCard(cafe: cafe)
                }
            }
            .padding(.vertical, 2)
        }
    }

    private var shortcutGrid: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader(
                title: "빠른 이동",
                subtitle: "문서 기준 핵심 운영 진입점"
            )

            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12)
                ],
                spacing: 12
            ) {
                ForEach(CafeManagementUiState.Shortcut.allCases) { shortcut in
                    shortcutCard(shortcut: shortcut)
                }
            }
        }
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

            Text("기존 카페를 검색해 운영 권한을 신청하거나 새 카페를 등록하세요.")
                .font(.subheadline)
                .foregroundStyle(Color(hex: "786E7A"))

            HStack(spacing: 10) {
                Button("기존 카페 검색") {
                    onAction(.searchCafeTapped)
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .frame(height: 44)
                .background(Color(hex: "EF6797"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                Button("새 카페 등록") {
                    onAction(.createCafeTapped)
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "6A5666"))
                .padding(.horizontal, 16)
                .frame(height: 44)
                .background(Color(hex: "F6EDF4"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }

            if !uiState.pendingClaims.isEmpty {
                Text("운영자 신청 상태")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))

                ForEach(uiState.pendingClaims) { claim in
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

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessageTapped)
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

    private func ownedCafeCard(cafe: CafeManagementUiState.OwnedCafe) -> some View {
        let isSelected = cafe.id == uiState.selectedCafe?.id

        return Button {
            onAction(.selectCafe(cafe.id))
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    ZStack {
                        Circle()
                            .fill(isSelected ? Color(hex: "EF6797") : Color(hex: "F6E6EE"))
                            .frame(width: 42, height: 42)
                        Image(systemName: "storefront.fill")
                            .foregroundStyle(isSelected ? .white : Color(hex: "9B6E86"))
                    }
                    Spacer()
                    approvalBadge(isApproved: cafe.isApproved)
                }

                Text(cafe.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))
                    .multilineTextAlignment(.leading)

                Text(cafe.city)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7A707A"))

                HStack(spacing: 12) {
                    metaChip(label: "체크인 \(cafe.todayCheckIns)")
                    metaChip(label: "평점 \(cafe.rating > 0 ? String(format: "%.1f", cafe.rating) : "-")")
                }
            }
            .frame(width: 230, alignment: .leading)
            .padding(16)
            .background(isSelected ? Color(hex: "FFF0F6") : .white)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(isSelected ? Color(hex: "EF6797") : Color(hex: "E8DFE7"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func approvalBadge(isApproved: Bool) -> some View {
        Text(isApproved ? "승인 완료" : "승인 대기")
            .font(.caption2.weight(.bold))
            .foregroundStyle(isApproved ? Color(hex: "2F8B57") : Color(hex: "9A6A11"))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(isApproved ? Color(hex: "E8F8EE") : Color(hex: "FFF1D8"))
            .clipShape(Capsule())
    }

    private func metaChip(label: String) -> some View {
        Text(label)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Color(hex: "6A606B"))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color(hex: "F7F2F6"))
            .clipShape(Capsule())
    }

    private func dashboardGrid(cafe: CafeManagementUiState.OwnedCafe) -> some View {
        LazyVGrid(
            columns: [
                GridItem(.flexible(), spacing: 12),
                GridItem(.flexible(), spacing: 12)
            ],
            spacing: 12
        ) {
            dashboardMetricCard(title: "오늘 방문자", value: "\(cafe.todayVisitors)", accent: Color(hex: "4F8EF7"))
            dashboardMetricCard(title: "오늘 체크인", value: "\(cafe.todayCheckIns)", accent: Color(hex: "EF6797"))
            dashboardMetricCard(title: "오늘 리뷰", value: "\(cafe.todayReviews)", accent: Color(hex: "47A88B"))
            dashboardMetricCard(
                title: "평점",
                value: cafe.rating > 0 ? String(format: "%.1f", cafe.rating) : "-",
                accent: Color(hex: "F59E0B")
            )
        }
    }

    private func dashboardMetricCard(title: String, value: String, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Circle()
                .fill(accent)
                .frame(width: 10, height: 10)
            Text(title)
                .font(.caption)
                .foregroundStyle(Color(hex: "7A707A"))
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
        }
        .frame(maxWidth: .infinity, minHeight: 112, alignment: .leading)
        .padding(16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func shortcutCard(shortcut: CafeManagementUiState.Shortcut) -> some View {
        Button {
            onAction(.shortcutTapped(shortcut))
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color(hex: "FCE6EF"))
                        .frame(width: 42, height: 42)
                    Image(systemName: shortcut.iconName)
                        .foregroundStyle(Color(hex: "EF6797"))
                }
                Text(shortcut.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "2B2330"))
                Text(shortcut.subtitle)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7A707A"))
                    .multilineTextAlignment(.leading)
            }
            .frame(maxWidth: .infinity, minHeight: 132, alignment: .leading)
            .padding(16)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func cafeOperationsCard(cafe: CafeManagementUiState.OwnedCafe) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader(
                title: "선택 카페 요약",
                subtitle: "현재 카페 컨텍스트에서 관리 가능한 핵심 수치"
            )
            HStack(spacing: 10) {
                metaChip(label: "캐스트 \(cafe.castCount)")
                metaChip(label: "공지 \(cafe.noticeCount)")
                metaChip(label: "외부 링크 \(cafe.externalLinkCount)")
            }
            Text("이 화면은 다중 카페 운영 구조를 전제로 하며, 다른 카페를 선택하면 대시보드와 관리 진입점이 같은 구조로 전환됩니다.")
                .font(.caption)
                .foregroundStyle(Color(hex: "786E7A"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func pendingClaimCard(claim: CafeManagementUiState.PendingClaim) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(claim.cafeName)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))
                Spacer()
                approvalBadge(isApproved: false)
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
        CafeManagementView()
    }
}
