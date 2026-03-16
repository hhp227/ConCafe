//
//  MyInfoView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

struct MyInfoView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = MyInfoViewModel()

    var body: some View {
        Group {
            if viewModel.uiState.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.uiState.isLoggedIn {
                ProfileMyInfoView(
                    uiState: viewModel.uiState,
                    onAction: viewModel.onAction
                )
            } else {
                GuestMyInfoView(
                    uiState: viewModel.uiState,
                    onAction: viewModel.onAction
                )
            }
        }
        .background(Color(hex: "FFF9FC"))
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            }
        }
    }
}

@MainActor
private struct GuestMyInfoView: View {
    let uiState: MyInfoUiState

    let onAction: @MainActor (MyInfoAction) -> Void

    let features = [
        ("mappin.and.ellipse", "체크인 기록", "방문한 카페를 기록하고\n추억을 남겨보세요", "EF6797", "F57AA8"),
        ("heart.fill", "즐겨찾기", "좋아하는 카페와 메이드를\n저장하세요", "9C6ADE", "B388EB"),
        ("star.fill", "배지 수집", "다양한 활동으로\n특별한 배지를 모아보세요", "F0B429", "F5C857"),
        ("gift.fill", "멤버십 혜택", "특별한 이벤트와\n할인 혜택을 받으세요", "4C8BF5", "71A7FF")
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 8) {
                    Text("💗")
                        .font(.system(size: 42))
                    Text("ConCafe에 오신 것을\n환영합니다!")
                        .font(.headline)
                        .bold()
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.white)
                    Text("로그인하고 컨셉카페의 모든 것을 즐겨보세요")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.9))
                    Button {
                        onAction(.signInTapped)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.system(size: 14, weight: .bold))
                            Text("로그인하기")
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                    }
                    .background(.white)
                    .foregroundStyle(Color(hex: "EF6797"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(colors: [Color(hex: "EF6797"), Color(hex: "F8A0C2")], startPoint: .topLeading, endPoint: .bottomTrailing)
                )
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
                VStack(alignment: .leading, spacing: 8) {
                    Text("로그인 후 이용 가능한 기능")
                        .font(.headline)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                        ForEach(Array(features.enumerated()), id: \.offset) { _, item in
                            VStack(alignment: .leading, spacing: 6) {
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(
                                        LinearGradient(
                                            colors: [Color(hex: item.3), Color(hex: item.4)],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 44, height: 44)
                                    .overlay(
                                        Image(systemName: item.0)
                                            .font(.system(size: 20, weight: .semibold))
                                            .foregroundStyle(.white)
                                    )
                                Text(item.1).font(.subheadline).bold()
                                Text(item.2)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(3)
                                    .multilineTextAlignment(.leading)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, minHeight: 140, maxHeight: 140, alignment: .topLeading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                    }
                }
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("인기 카페 둘러보기")
                            .font(.headline)
                        Spacer()
                        Button {
                        } label: {
                            HStack(spacing: 2) {
                                Text("더보기")
                                    .font(.caption)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 11, weight: .semibold))
                            }
                            .foregroundStyle(Color(hex: "EF6797"))
                        }
                        .buttonStyle(.plain)
                    }
                    ForEach(uiState.popularCafes, id: \.id) { cafe in
                        HStack(spacing: 10) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(LinearGradient(colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")], startPoint: .top, endPoint: .bottom))
                                .frame(width: 64, height: 64)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(cafe.name).bold()
                                Text("⭐ \(String(format: "%.1f", cafe.ratingAvg))").font(.caption)
                            }
                            Spacer()
                        }
                        .padding(10)
                        .background(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .onTapGesture {
                            onAction(.cafeTapped(id: cafe.id))
                        }
                    }
                }
                VStack(spacing: 8) {
                    Text("✨")
                        .font(.title2)
                    Text("지금 바로 시작하세요!")
                        .font(.headline)
                        .bold()
                    Text("ConCafe 회원만의 특별한 혜택을 누려보세요")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button {
                    } label: {
                        Text("회원가입하기")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .background(
                                LinearGradient(
                                    colors: [Color(hex: "EF6797"), Color(hex: "F8A0C2")],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(colors: [Color(hex: "FFEAF2"), Color(hex: "FDE3F0")], startPoint: .topLeading, endPoint: .bottomTrailing)
                )
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            }
            .padding(16)
        }
    }
}

@MainActor
private struct ProfileMyInfoView: View {
    let uiState: MyInfoUiState

    let onAction: @MainActor (MyInfoAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                profileCard
                statsCard
                badgesSection
                recentVisitsSection
                favoritesSection
                if uiState.user?.role != .cast {
                    followedMaidsSection
                }
            }
            .padding(16)
        }
    }

    private var profileCard: some View {
        let user = uiState.user
        let castDetail = uiState.castDetail
        let ownerCafe = uiState.ownedCafes.first
        let title = {
            switch user?.role {
            case UserRole.cast:
                return castDetail?.cast.name ?? user?.nickname ?? "메이드러버"
            default:
                return user?.nickname ?? "메이드러버"
            }
        }()
        let subtitle = {
            switch user?.role {
            case UserRole.cast:
                guard let user, let castDetail else { return "" }
                return user.nickname == castDetail.cast.name ? "" : user.nickname
            case UserRole.cafeOwner:
                return "카페 운영자"
            case UserRole.admin:
                return "관리자 계정"
            default:
                return "레벨 \(uiState.summary?.level ?? 1) · 열정적인 팬"
            }
        }()
        let accentText = {
            switch user?.role {
            case UserRole.cast:
                return castDetail?.cafe.name ?? "소속 카페 없음"
            case UserRole.cafeOwner:
                return ownerCafe?.name ?? "운영 카페 없음"
            case UserRole.admin:
                return "ConCafe 운영"
            default:
                return "내 활동 요약"
            }
        }()
        return HStack(spacing: 16) {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFD7E5"), Color(hex: "F2ADC2")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 78, height: 78)
                .overlay(
                    Text(String(title.prefix(2)).uppercased())
                        .font(.title3.weight(.bold))
                        .foregroundStyle(Color(hex: "7C3F67"))
                )
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .bottom, spacing: 8) {
                    Text(title)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(Color(hex: "24161E"))
                    if !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "7A707A"))
                    }
                }
                HStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                    Text(accentText)
                        .font(.subheadline)
                        .foregroundStyle(Color(hex: "5B4A57"))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color.white.opacity(0.65), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 12, x: 0, y: 6)
    }

    private var statsCard: some View {
        return HStack(spacing: 8) {
            ForEach(metricCards, id: \.title) { metric in
                profileStat(metric)
            }
        }
    }

    private func profileStat(_ metric: MyInfoMetricCard) -> some View {
        VStack(spacing: 6) {
            Text(metric.title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(Color(hex: "7A707A"))
                .multilineTextAlignment(.center)
            Text(metric.value)
                .font(.title2.weight(.bold))
                .foregroundStyle(metric.highlight ? Color(hex: "D94A82") : Color(hex: "24161E"))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .padding(.horizontal, 10)
        .background(metric.highlight ? Color(hex: "FFD1DC").opacity(0.10) : Color.white.opacity(0.92))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: metric.highlight ? .clear : Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(metric.highlight ? Color(hex: "FFB3C6").opacity(0.20) : Color(hex: "FFD1DC").opacity(0.10), lineWidth: 1)
        )
    }

    private var metricCards: [MyInfoMetricCard] {
        switch uiState.user?.role {
        case UserRole.cast:
            return [
                .init(title: "전체 팔로워", value: "\(uiState.castDetail?.cast.followerCount ?? 0)", highlight: false),
                .init(title: "근무 일정", value: "\(uiState.castDetail?.schedule.count ?? 0)", highlight: true),
                .init(title: "평점", value: String(format: "%.1f", uiState.castDetail?.cast.rating ?? 0), highlight: false)
            ]
        case UserRole.cafeOwner:
            let cafeCount = uiState.ownedCafes.count
            let castCount = uiState.ownedCafes.reduce(0) { $0 + Int($1.castCount) }
            let rating = uiState.ownedCafes.isEmpty ? 0 : uiState.ownedCafes.map(\.rating).reduce(0, +) / Double(uiState.ownedCafes.count)
            return [
                .init(title: "운영 카페", value: "\(cafeCount)", highlight: false),
                .init(title: "소속 캐스트", value: "\(castCount)", highlight: true),
                .init(title: "평균 평점", value: String(format: "%.1f", rating), highlight: false)
            ]
        default:
            return [
                .init(title: "방문 횟수", value: "\(uiState.summary?.totalVisits ?? 0)", highlight: false),
                .init(title: "즐겨찾기", value: "\(uiState.summary?.favoritesCount ?? 0)", highlight: true),
                .init(title: "팔로우", value: "\(uiState.summary?.followedCastsCount ?? 0)", highlight: false)
            ]
        }
    }

    private var badgesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("획득 배지").font(.headline)
                Spacer()
                Text("\(uiState.badges.filter { $0.unlocked }.count) / \(uiState.badges.count)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(uiState.badges, id: \.id) { badge in
                        VStack {
                            RoundedRectangle(cornerRadius: 16)
                                .fill(badge.unlocked ? Color(hex: "EF6797") : Color(hex: "DADADA"))
                                .frame(width: 70, height: 70)
                                .overlay(Text(badge.icon))
                            Text(badge.name)
                                .font(.caption2)
                        }
                    }
                }
            }
        }
    }

    private var recentVisitsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("최근 방문").font(.headline)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(uiState.recentVisits, id: \.id) { cafe in
                        VStack(alignment: .leading, spacing: 6) {
                            RoundedRectangle(cornerRadius: 14)
                                .fill(LinearGradient(colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")], startPoint: .top, endPoint: .bottom))
                                .frame(width: 120, height: 120)
                            Text(cafe.name).font(.caption)
                        }
                        .onTapGesture { onAction(.cafeTapped(id: cafe.id)) }
                    }
                }
            }
        }
    }

    private var favoritesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("즐겨찾기").font(.headline)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(uiState.favorites.prefix(4), id: \.id) { cafe in
                    CafeSummaryCard(
                        name: cafe.name,
                        rating: String(format: "%.1f", cafe.ratingAvg),
                        location: cafe.region.city,
                        thumbnailImage: cafe.thumbnailImage,
                        trailingLabel: nil,
                        onTap: { onAction(.cafeTapped(id: cafe.id)) }
                    )
                }
            }
        }
    }

    private var followedMaidsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("팔로우한 메이드").font(.headline)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(uiState.followedMaids.prefix(6), id: \.id) { maid in
                        VStack {
                            Circle()
                                .fill(LinearGradient(colors: [Color(hex: "FFDFEA"), Color(hex: "FFBED5")], startPoint: .top, endPoint: .bottom))
                                .frame(width: 72, height: 72)
                            Text(maid.name)
                                .font(.caption)
                        }
                        .onTapGesture { onAction(.maidTapped(id: maid.id)) }
                    }
                }
            }
        }
    }
}

private struct MyInfoMetricCard {
    let title: String
    let value: String
    let highlight: Bool
}

struct MyInfoView_Previews: PreviewProvider {
    static var previews: some View {
        MyInfoView(onNavigationAction: { _ in })
    }
}
