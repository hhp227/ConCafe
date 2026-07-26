//
//  RankingNativeAd.swift
//  ConCafe
//
//  Created by 홍희표 on 4/4/26.
//

import SwiftUI
import UIKit
import Shared
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

final class IOSNativeAdHandle: NativeAdHandle {
    let nativeAd: GADNativeAd

    func destroy() {
        // iOS GADNativeAd는 destroy 없음
        // 대신 strong reference 해제만 하면 됨
    }
    
    init(_ nativeAd: GADNativeAd) {
        self.nativeAd = nativeAd
    }
}

class IosNativeAdDataSourceImpl: NSObject, NativeAdDataSource {
    private var continuation: CheckedContinuation<NativeAdHandle?, Error>?
    
    private var adLoader: GADAdLoader?

    func loadAd(slot: Int32) async throws -> (any NativeAdHandle)? {
        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation

            let adUnitId: String
            #if DEBUG
            adUnitId = "ca-app-pub-3940256099942544/3986624511"
            #else
            if slot >= 100 {
                adUnitId = "ca-app-pub-6216021268300256/5916790240"
            } else {
                adUnitId = (slot == 2) ? "ca-app-pub-6216021268300256/7952443563" : "ca-app-pub-6216021268300256/5283160617"
            }
            #endif

            let loader = GADAdLoader(
                adUnitID: adUnitId,
                rootViewController: UIApplication.shared.connectedScenes
                    .compactMap { $0 as? UIWindowScene }
                    .flatMap { $0.windows }
                    .first { $0.isKeyWindow }?.rootViewController,
                adTypes: [.native],
                options: nil
            )

            loader.delegate = self
            self.adLoader = loader
            loader.load(GADRequest())
        }
    }
}

extension IosNativeAdDataSourceImpl: GADNativeAdLoaderDelegate {
    func adLoader(_ adLoader: GADAdLoader, didReceive nativeAd: GADNativeAd) {
        continuation?.resume(returning: IOSNativeAdHandle(nativeAd))
        continuation = nil
    }

    func adLoader(_ adLoader: GADAdLoader, didFailToReceiveAdWithError error: Error) {
        continuation?.resume(returning: nil)
        continuation = nil
    }
}

#if canImport(GoogleMobileAds)
struct RankingNativeAdCard: View {
    let nativeAdHandle: (any NativeAdHandle)?

    private var nativeAd: GADNativeAd? {
        (nativeAdHandle as? IOSNativeAdHandle)?.nativeAd
    }

    var body: some View {
        ZStack {
            if let nativeAd {
                RankingNativeAdRepresentable(nativeAd: nativeAd)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(alignment: .top, spacing: 12) {
                        VStack(alignment: .leading, spacing: 8) {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(Color.white.opacity(0.46))
                                .frame(width: 52, height: 22)
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color.white.opacity(0.42))
                                .frame(width: 120, height: 14)
                        }
                        Spacer(minLength: 8)
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color.white.opacity(0.62))
                            .frame(width: 74, height: 36)
                    }
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.62))
                        .frame(height: 22)
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.56))
                        .frame(height: 16)
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.56))
                        .frame(width: 190, height: 16)
                    Spacer(minLength: 0)
                    Text(
                        String(
                            localized: "ranking_native_ad_badge",
                            defaultValue: "광고",
                            table: "Localizable"
                        )
                    )
                    .font(.caption2)
                    .foregroundStyle(ConCafeColors.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(20)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct RankingNativeAdRepresentable: UIViewRepresentable {
    let nativeAd: GADNativeAd

    func makeUIView(context: Context) -> GADNativeAdView {
        let nativeAdView = GADNativeAdView()
        let container = UIStackView()
        let topRow = UIStackView()
        let metaStack = UIStackView()
        let badgeChip = UIView()
        let badgeLabel = UILabel()
        let sponsorLabel = UILabel()
        let headlineLabel = UILabel()
        let bodyLabel = UILabel()
        let callToActionButton = UIButton(type: .system)
        let spacer = UIView()

        container.axis = .vertical
        container.spacing = 12
        container.translatesAutoresizingMaskIntoConstraints = false
        nativeAdView.addSubview(container)

        NSLayoutConstraint.activate([
            container.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor, constant: 20),
            container.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -20),
            container.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 20),
            container.bottomAnchor.constraint(equalTo: nativeAdView.bottomAnchor, constant: -20)
        ])

        topRow.axis = .horizontal
        topRow.alignment = .top
        topRow.distribution = .fill
        topRow.spacing = 10

        metaStack.axis = .vertical
        metaStack.alignment = .leading
        metaStack.spacing = 6
        metaStack.setContentHuggingPriority(.required, for: .horizontal)
        metaStack.setContentCompressionResistancePriority(.required, for: .horizontal)
        metaStack.setContentHuggingPriority(.required, for: .vertical)
        metaStack.setContentCompressionResistancePriority(.required, for: .vertical)

        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        spacer.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)

        badgeChip.backgroundColor = UIColor.white.withAlphaComponent(0.46)
        badgeChip.layer.cornerRadius = 11
        badgeChip.clipsToBounds = true
        badgeChip.isHidden = false
        badgeChip.alpha = 1
        badgeChip.translatesAutoresizingMaskIntoConstraints = false
        badgeChip.setContentHuggingPriority(.required, for: .horizontal)
        badgeChip.setContentCompressionResistancePriority(.required, for: .horizontal)
        badgeChip.heightAnchor.constraint(greaterThanOrEqualToConstant: 22).isActive = true
        badgeChip.widthAnchor.constraint(greaterThanOrEqualToConstant: 46).isActive = true
        badgeChip.tag = 99100

        badgeLabel.text = String(
            localized: "ranking_native_ad_badge",
            defaultValue: "광고",
            table: "Localizable"
        )
        if badgeLabel.text?.isEmpty != false {
            badgeLabel.text = "광고"
        }
        badgeLabel.font = .systemFont(ofSize: 12, weight: .bold)
        badgeLabel.textColor = UIColor(ConCafeColors.primary)
        badgeLabel.textAlignment = .center
        badgeLabel.translatesAutoresizingMaskIntoConstraints = false
        badgeLabel.setContentHuggingPriority(.required, for: .horizontal)
        badgeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        badgeLabel.setContentHuggingPriority(.required, for: .vertical)
        badgeLabel.setContentCompressionResistancePriority(.required, for: .vertical)
        badgeLabel.tag = 99101

        badgeChip.addSubview(badgeLabel)
        NSLayoutConstraint.activate([
            badgeLabel.leadingAnchor.constraint(equalTo: badgeChip.leadingAnchor, constant: 10),
            badgeLabel.trailingAnchor.constraint(equalTo: badgeChip.trailingAnchor, constant: -10),
            badgeLabel.topAnchor.constraint(equalTo: badgeChip.topAnchor, constant: 4),
            badgeLabel.bottomAnchor.constraint(equalTo: badgeChip.bottomAnchor, constant: -4)
        ])

        sponsorLabel.font = .systemFont(ofSize: 11, weight: .medium)
        sponsorLabel.textColor = UIColor(ConCafeColors.textMuted)
        sponsorLabel.numberOfLines = 1

        headlineLabel.font = .systemFont(ofSize: 19, weight: .bold)
        headlineLabel.textColor = UIColor(ConCafeColors.textPrimary)
        headlineLabel.numberOfLines = 2
        headlineLabel.lineBreakMode = .byTruncatingTail

        bodyLabel.font = .systemFont(ofSize: 13, weight: .regular)
        bodyLabel.textColor = UIColor(ConCafeColors.textSecondary)
        bodyLabel.numberOfLines = 3

        callToActionButton.titleLabel?.font = .systemFont(ofSize: 14, weight: .semibold)
        callToActionButton.setTitleColor(UIColor(ConCafeColors.textPrimary), for: .normal)
        callToActionButton.backgroundColor = UIColor(ConCafeColors.primaryContainer)
        callToActionButton.contentEdgeInsets = UIEdgeInsets(top: 8, left: 16, bottom: 8, right: 16)
        callToActionButton.layer.cornerRadius = 16
        callToActionButton.setContentHuggingPriority(.defaultHigh, for: .horizontal)
        callToActionButton.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        callToActionButton.heightAnchor.constraint(greaterThanOrEqualToConstant: 36).isActive = true

        metaStack.addArrangedSubview(badgeChip)
        metaStack.addArrangedSubview(sponsorLabel)
        topRow.addArrangedSubview(metaStack)
        topRow.addArrangedSubview(spacer)
        topRow.addArrangedSubview(callToActionButton)

        container.addArrangedSubview(topRow)
        container.addArrangedSubview(headlineLabel)
        container.addArrangedSubview(bodyLabel)

        nativeAdView.headlineView = headlineLabel
        nativeAdView.bodyView = bodyLabel
        nativeAdView.advertiserView = sponsorLabel
        nativeAdView.callToActionView = callToActionButton

        return nativeAdView
    }

    func updateUIView(_ nativeAdView: GADNativeAdView, context: Context) {
        if let badgeChip = nativeAdView.viewWithTag(99100) {
            badgeChip.isHidden = false
            badgeChip.alpha = 1
        }
        if let badgeLabel = nativeAdView.viewWithTag(99101) as? UILabel {
            badgeLabel.text = String(
                localized: "ranking_native_ad_badge",
                defaultValue: "광고",
                table: "Localizable"
            )
            if badgeLabel.text?.isEmpty != false {
                badgeLabel.text = "광고"
            }
            badgeLabel.sizeToFit()
            badgeLabel.isHidden = false
            badgeLabel.alpha = 1
        }

        (nativeAdView.headlineView as? UILabel)?.text = nativeAd.headline
        if let body = nativeAd.body, !body.isEmpty {
            (nativeAdView.bodyView as? UILabel)?.text = body
            nativeAdView.bodyView?.isHidden = false
        } else {
            (nativeAdView.bodyView as? UILabel)?.text = nil
            nativeAdView.bodyView?.isHidden = true
        }

        let defaultSponsor = String(
            localized: "ranking_native_ad_sponsor",
            defaultValue: "ConCafe 제공 광고",
            table: "Localizable"
        )
        let advertiser = nativeAd.advertiser?.trimmingCharacters(in: .whitespacesAndNewlines)
        (nativeAdView.advertiserView as? UILabel)?.text = (advertiser?.isEmpty == false) ? advertiser : defaultSponsor

        if let callToAction = nativeAd.callToAction, !callToAction.isEmpty {
            (nativeAdView.callToActionView as? UIButton)?.setTitle(callToAction, for: .normal)
            nativeAdView.callToActionView?.isHidden = false
        } else {
            (nativeAdView.callToActionView as? UIButton)?.setTitle(nil, for: .normal)
            nativeAdView.callToActionView?.isHidden = true
        }
        nativeAdView.callToActionView?.isUserInteractionEnabled = false

        nativeAdView.nativeAd = nativeAd
    }
}
#else
private struct RankingNativeAdCard: View {
    let nativeAdHandle: (any NativeAdHandle)?

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
                .foregroundStyle(ConCafeColors.primary)
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
                .foregroundStyle(ConCafeColors.textPrimary)
            Text(
                String(
                    localized: "ranking_native_ad_desc",
                    defaultValue: "ConCafe의 최신 소식을 확인해보세요.",
                    table: "Localizable"
                )
            )
                .font(.caption)
                .foregroundStyle(ConCafeColors.textSecondary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
#endif

#Preview {
    RankingNativeAdCard(nativeAdHandle: nil)
}
