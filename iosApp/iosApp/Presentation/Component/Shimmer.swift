//
//  Shimmer.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/26.
//

import SwiftUI

/// ConCafe 로딩 스켈레톤 쉬머 이펙트.
///
/// 콘텐츠 로딩 중 프로그레스 인디케이터 대신 화면 레이아웃을 모방한
/// 스켈레톤 위로 하이라이트 밴드가 흐르는 쉬머를 표시한다.
/// 색상은 ConCafeColors 시맨틱 토큰만 사용한다 (Compose Shimmer.kt와 1:1 유지).
private enum ShimmerAppearance {
    static let duration: Double = 1.2

    static let travel: CGFloat = 600

    static let band: CGFloat = 240
}

struct ShimmerBox: View {
    var cornerRadius: CGFloat = 8

    var isCircular: Bool = false

    @State private var isAnimating: Bool = false

    private var shimmerContent: some View {
        GeometryReader { geometry in
            let originX = geometry.frame(in: .global).minX
            let sweepStart = -ShimmerAppearance.band - originX
            let sweepEnd = ShimmerAppearance.travel + ShimmerAppearance.band - originX

            ConCafeColors.outline
                .overlay(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.clear,
                            ConCafeColors.outlineStrong.opacity(0.4),
                            Color.clear
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .frame(width: ShimmerAppearance.band)
                    .offset(x: isAnimating ? sweepEnd : sweepStart)
                    // withAnimation(repeatForever)은 전역 트랜잭션이라 등장 중 확정되는 레이아웃까지
                    // 무한 반복에 가둔다(가변폭 배너 미표시, push 전환 중 내비바 상태 꼬임의 원인).
                    // 값 범위 애니메이션으로 offset 변화만 반복시킨다.
                    .animation(
                        .linear(duration: ShimmerAppearance.duration).repeatForever(autoreverses: false),
                        value: isAnimating
                    ),
                    alignment: .leading
                )
        }
    }

    var body: some View {
        Group {
            if isCircular {
                shimmerContent.clipShape(Circle())
            } else {
                shimmerContent.clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            }
        }
        .onAppear {
            isAnimating = true
        }
    }
}

struct ShimmerListItemSkeleton: View {
    var avatarSize: CGFloat = 48

    var isAvatarCircular: Bool = true

    var body: some View {
        HStack(spacing: 12) {
            ShimmerBox(cornerRadius: 12, isCircular: isAvatarCircular)
                .frame(width: avatarSize, height: avatarSize)
            VStack(alignment: .leading, spacing: 8) {
                ShimmerBox()
                    .frame(width: 180, height: 16)
                ShimmerBox()
                    .frame(width: 110, height: 12)
            }
            Spacer(minLength: 0)
        }
    }
}

struct ShimmerListSkeleton: View {
    var itemCount: Int = 8

    var avatarSize: CGFloat = 48

    var isAvatarCircular: Bool = true

    var body: some View {
        VStack(spacing: 20) {
            ForEach(0..<itemCount, id: \.self) { _ in
                ShimmerListItemSkeleton(
                    avatarSize: avatarSize,
                    isAvatarCircular: isAvatarCircular
                )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
    }
}

struct ShimmerCardListSkeleton: View {
    var itemCount: Int = 3

    var imageHeight: CGFloat = 160

    var body: some View {
        VStack(spacing: 24) {
            ForEach(0..<itemCount, id: \.self) { _ in
                VStack(alignment: .leading, spacing: 0) {
                    ShimmerBox(cornerRadius: 16)
                        .frame(maxWidth: .infinity)
                        .frame(height: imageHeight)
                    ShimmerBox()
                        .frame(width: 180, height: 16)
                        .padding(.top, 12)
                    ShimmerBox()
                        .frame(width: 120, height: 12)
                        .padding(.top, 8)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
    }
}

struct ShimmerCardGridSkeleton: View {
    var columnCount: Int = 2

    var rowCount: Int = 3

    var imageHeight: CGFloat = 120

    var body: some View {
        VStack(spacing: 12) {
            ForEach(0..<rowCount, id: \.self) { _ in
                HStack(alignment: .top, spacing: 12) {
                    ForEach(0..<columnCount, id: \.self) { _ in
                        VStack(alignment: .leading, spacing: 0) {
                            ShimmerBox(cornerRadius: 16)
                                .frame(maxWidth: .infinity)
                                .frame(height: imageHeight)
                            ShimmerBox()
                                .frame(width: 120, height: 14)
                                .padding(.top, 10)
                            ShimmerBox()
                                .frame(width: 80, height: 12)
                                .padding(.top, 6)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
    }
}

struct ShimmerFormSkeleton: View {
    var fieldCount: Int = 6

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ForEach(0..<fieldCount, id: \.self) { _ in
                VStack(alignment: .leading, spacing: 8) {
                    ShimmerBox()
                        .frame(width: 100, height: 12)
                    ShimmerBox(cornerRadius: 12)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
    }
}
