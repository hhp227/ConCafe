//
//  RankingNativeAd.swift
//  ConCafe
//
//  Created by 홍희표 on 4/4/26.
//

import SwiftUI
import UIKit
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

#if canImport(GoogleMobileAds)
struct RankingNativeAdCard: View {
    @StateObject private var loader = RankingNativeAdLoader()

    var body: some View {
        ZStack {
            if let nativeAd = loader.nativeAd {
                RankingNativeAdRepresentable(nativeAd: nativeAd)
                    .frame(maxWidth: .infinity)
                    .frame(maxHeight: .infinity)
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color.white.opacity(0.46))
                        .frame(width: 52, height: 20)
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.62))
                        .frame(height: 20)
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.56))
                        .frame(height: 16)
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.56))
                        .frame(width: 180, height: 16)
                    Spacer()
                    Text("Ad")
                        .font(.caption2)
                        .foregroundStyle(Color(hex: "7E5A6E"))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(20)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(
            LinearGradient(
                colors: [Color(hex: "FFEAF3"), Color(hex: "FFDCEB")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }
}

private final class RankingNativeAdLoader: NSObject, ObservableObject, GADNativeAdLoaderDelegate {
    @Published var nativeAd: GADNativeAd?

    private var adLoader: GADAdLoader?

    override init() {
        super.init()

        let adUnitId: String
        #if DEBUG
        adUnitId = "ca-app-pub-3940256099942544/3986624511"
        #else
        adUnitId = "ca-app-pub-6216021268300256/5283160617"
        #endif

        adLoader = GADAdLoader(
            adUnitID: adUnitId,
            rootViewController: UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }?.rootViewController,
            adTypes: [.native],
            options: nil
        )
        adLoader?.delegate = self
        adLoader?.load(GADRequest())
    }

    func adLoader(_ adLoader: GADAdLoader, didReceive nativeAd: GADNativeAd) {
        self.nativeAd = nativeAd
    }

    func adLoader(_ adLoader: GADAdLoader, didFailToReceiveAdWithError error: Error) {
        print("Ranking native ad load failed: \(error.localizedDescription)")
    }
}

private struct RankingNativeAdRepresentable: UIViewRepresentable {
    let nativeAd: GADNativeAd

    func makeUIView(context: Context) -> GADNativeAdView {
        let nativeAdView = GADNativeAdView()
        let container = UIStackView()
        let badgeLabel = UILabel()
        let headlineLabel = UILabel()
        let bodyLabel = UILabel()
        let callToActionButton = UIButton(type: .system)

        container.axis = .vertical
        container.spacing = 10
        container.translatesAutoresizingMaskIntoConstraints = false
        nativeAdView.addSubview(container)

        NSLayoutConstraint.activate([
            container.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor, constant: 20),
            container.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -20),
            container.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 20),
            container.bottomAnchor.constraint(equalTo: nativeAdView.bottomAnchor, constant: -20)
        ])

        badgeLabel.text = String(
            localized: "ranking_native_ad_badge",
            defaultValue: "광고",
            table: "Localizable"
        )
        badgeLabel.font = .systemFont(ofSize: 12, weight: .bold)
        badgeLabel.textColor = UIColor(Color(hex: "B74D73"))
        badgeLabel.backgroundColor = UIColor(Color(hex: "FFE9F1"))
        badgeLabel.textAlignment = .center
        badgeLabel.layer.cornerRadius = 12
        badgeLabel.clipsToBounds = true
        badgeLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 46).isActive = true

        headlineLabel.font = .systemFont(ofSize: 19, weight: .bold)
        headlineLabel.textColor = UIColor(Color(hex: "2B2330"))
        headlineLabel.numberOfLines = 2

        bodyLabel.font = .systemFont(ofSize: 13, weight: .regular)
        bodyLabel.textColor = UIColor(Color(hex: "6F6670"))
        bodyLabel.numberOfLines = 3

        callToActionButton.titleLabel?.font = .systemFont(ofSize: 14, weight: .semibold)
        callToActionButton.setTitleColor(UIColor(Color(hex: "2B2330")), for: .normal)
        callToActionButton.backgroundColor = UIColor(Color(hex: "FFD1DC"))
        callToActionButton.contentEdgeInsets = UIEdgeInsets(top: 8, left: 14, bottom: 8, right: 14)
        callToActionButton.layer.cornerRadius = 16

        container.addArrangedSubview(badgeLabel)
        container.addArrangedSubview(headlineLabel)
        container.addArrangedSubview(bodyLabel)
        container.addArrangedSubview(callToActionButton)

        nativeAdView.headlineView = headlineLabel
        nativeAdView.bodyView = bodyLabel
        nativeAdView.callToActionView = callToActionButton

        return nativeAdView
    }

    func updateUIView(_ nativeAdView: GADNativeAdView, context: Context) {
        (nativeAdView.headlineView as? UILabel)?.text = nativeAd.headline
        (nativeAdView.bodyView as? UILabel)?.text = nativeAd.body

        if let callToAction = nativeAd.callToAction, !callToAction.isEmpty {
            (nativeAdView.callToActionView as? UIButton)?.setTitle(callToAction, for: .normal)
            nativeAdView.callToActionView?.isHidden = false
        } else {
            (nativeAdView.callToActionView as? UIButton)?.setTitle(nil, for: .normal)
            nativeAdView.callToActionView?.isHidden = true
        }

        nativeAdView.nativeAd = nativeAd
    }
}
#else
private struct RankingNativeAdCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(
                String(
                    localized: "ranking_native_ad_badge",
                    defaultValue: "광고",
                    table: "Localizable"
                )
            )
                .font(.caption2.weight(.bold))
                .foregroundStyle(Color(hex: "B74D73"))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color.white.opacity(0.46))
                .clipShape(Capsule())
            Text(
                String(
                    localized: "ranking_native_ad_title",
                    defaultValue: "랭킹 프로모션",
                    table: "Localizable"
                )
            )
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(
                String(
                    localized: "ranking_native_ad_desc",
                    defaultValue: "ConCafe의 최신 소식을 확인해보세요.",
                    table: "Localizable"
                )
            )
                .font(.caption)
                .foregroundStyle(Color(hex: "6F6670"))
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(
            LinearGradient(
                colors: [Color(hex: "FFEAF3"), Color(hex: "FFDCEB")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }
}
#endif

#Preview {
    RankingNativeAdCard()
}
