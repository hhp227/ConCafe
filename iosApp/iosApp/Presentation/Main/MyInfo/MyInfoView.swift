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
                    Text("로그인하고 메이드카페의 모든 것을 즐겨보세요")
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
                followedMaidsSection
            }
            .padding(16)
        }
    }
    
    private var profileCard: some View {
        let summary = uiState.summary
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(uiState.user?.nickname ?? "메이드러버")
                    .font(.headline)
                    .bold()
            }
            Text("레벨 \(summary?.level ?? 1)")
                .foregroundStyle(.white.opacity(0.9))
            HStack(spacing: 14) {
                Text("📍 \(summary?.totalVisits ?? 0)")
                Text("❤️ \(summary?.favoritesCount ?? 0)")
                Text("👥 \(summary?.followedCastsCount ?? 0)")
            }
            .foregroundStyle(.white)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [Color(hex: "EF6797"), Color(hex: "F8A0C2")], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var statsCard: some View {
        let summary = uiState.summary
        return HStack(spacing: 8) {
            profileStat("방문 횟수", Int(summary?.totalVisits ?? 0))
            profileStat("즐겨찾기", Int(summary?.favoritesCount ?? 0))
            profileStat("팔로우", Int(summary?.followedCastsCount ?? 0))
        }
    }

    private func profileStat(_ title: String, _ value: Int) -> some View {
        VStack {
            Text("\(value)")
                .font(.headline)
                .foregroundStyle(Color(hex: "EF6797"))
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(10)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
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
                    VStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(LinearGradient(colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")], startPoint: .top, endPoint: .bottom))
                            .frame(height: 90)
                        Text(cafe.name)
                            .font(.caption)
                            .padding(8)
                    }
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .onTapGesture { onAction(.cafeTapped(id: cafe.id)) }
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

struct MyInfoView_Previews: PreviewProvider {
    static var previews: some View {
        MyInfoView(onNavigationAction: { _ in })
    }
}
