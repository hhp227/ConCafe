//
//  HomeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation
import SwiftUI
import Shared

struct HomeView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = HomeViewModel()
    
    @State private var currentBannerPage = 0
    
    private let bannerTimer = Timer.publish(every: 3.0, on: .main, in: .common).autoconnect()

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                bannerSection
                popularMaidSection
                nearbyCafeSection
                birthdaySection
                noticeSection
            }
            .padding(.vertical, 16)
        }
        .background(Color(hex: "FFF9FC"))
        .onAppear {
            viewModel.onEvent = { event in
                switch event {
                case .navigateToDetail(let id):
                    onNavigationAction(.navigateToDetail(id: id))
                }
            }
            if currentBannerPage >= viewModel.uiState.banners.count {
                currentBannerPage = 0
            }
        }
        .onReceive(bannerTimer) { _ in
            guard viewModel.uiState.banners.count > 1 else { return }
            withAnimation(.easeInOut(duration: 0.35)) {
                currentBannerPage = (currentBannerPage + 1) % viewModel.uiState.banners.count
            }
        }
    }

    private var bannerSection: some View {
        TabView(selection: $currentBannerPage) {
            ForEach(Array(viewModel.uiState.banners.enumerated()), id: \.element.id) { index, banner in
                ZStack(alignment: .bottomLeading) {
                    LinearGradient(colors: banner.colors, startPoint: .topLeading, endPoint: .bottomTrailing)
                    Text(banner.title)
                        .font(.title3.weight(.bold))
                        .foregroundColor(.white)
                        .padding(16)
                }
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .padding(.horizontal, 16)
                .tag(index)
            }
        }
        .frame(height: 190)
        .tabViewStyle(.page(indexDisplayMode: .automatic))
    }

    private var popularMaidSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "❤", title: "인기 메이드")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.uiState.popularMaids) { maid in
                        VStack(alignment: .leading, spacing: 8) {
                            RoundedRectangle(cornerRadius: 16)
                                .fill(LinearGradient(colors: [Color(hex: "FFDCE8"), Color(hex: "FFC4D8")], startPoint: .top, endPoint: .bottom))
                                .frame(width: 132, height: 124)
                            Text(maid.name)
                                .font(.subheadline.weight(.semibold))
                            Text(maid.cafe)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text("👥 \(maid.followers)")
                                .font(.caption)
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                        .frame(width: 132, alignment: .leading)
                        .padding(10)
                        .background(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .onTapGesture {
                            viewModel.onAction(.maidTapped(id: maid.id))
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private var nearbyCafeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "📍", title: "근처 메이드카페")
            VStack(spacing: 12) {
                ForEach(viewModel.uiState.nearbyCafes) { cafe in
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(LinearGradient(colors: [Color(hex: "FFE1C7"), Color(hex: "FFCEAE")], startPoint: .top, endPoint: .bottom))
                            .frame(width: 92, height: 92)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(cafe.name)
                                .font(.subheadline.weight(.semibold))
                            Text("⭐ \(cafe.rating)")
                                .font(.caption)
                            Text(cafe.location)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text("📍 \(cafe.distance)")
                                .font(.caption)
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                        Spacer()
                    }
                    .onTapGesture {
                        viewModel.onAction(.cafeTapped(id: cafe.id))
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private var birthdaySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "🎂", title: "생일인 메이드")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(viewModel.uiState.birthdayMaids) { maid in
                        VStack(spacing: 8) {
                            Circle()
                                .fill(LinearGradient(colors: [Color(hex: "FFD3E2"), Color(hex: "FFB6D0")], startPoint: .top, endPoint: .bottom))
                                .frame(width: 74, height: 74)
                            Text(maid.name)
                                .font(.caption)
                        }
                        .onTapGesture {
                            viewModel.onAction(.birthdayMaidTapped(id: maid.id))
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private var noticeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "📢", title: "최근 카페 공지")
            VStack(spacing: 10) {
                ForEach(viewModel.uiState.notices) { notice in
                    HStack(alignment: .top, spacing: 8) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(notice.cafe)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Color(hex: "EF6797"))
                            Text(notice.content)
                                .font(.subheadline)
                        }
                        Spacer()
                        Text(notice.time)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .padding(12)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct SectionTitle: View {
    let icon: String
    
    let title: String

    var body: some View {
        HStack(spacing: 6) {
            Text(icon)
            Text(title)
                .font(.headline)
        }
        .padding(.horizontal, 16)
    }
}

private extension Color {
    init(hex: String) {
        let value = Int(hex, radix: 16) ?? 0
        let red = Double((value >> 16) & 0xFF) / 255.0
        let green = Double((value >> 8) & 0xFF) / 255.0
        let blue = Double(value & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: 1)
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView(onNavigationAction: { _ in })
    }
}
