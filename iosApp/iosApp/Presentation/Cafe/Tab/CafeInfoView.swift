//
//  CafeInfoView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeInfoView: View {
    let cafeDetail: CafeDetail

    var body: some View {
        VStack(spacing: 14) {
            infoCard(detail: cafeDetail)
            descriptionCard(detail: cafeDetail)
            socialMediaCard(detail: cafeDetail)
            reservationButton(
                reservationUrl: cafeDetail.cafe.reservationUrl,
                onTapped: {
                    if let url = URL(string: $0) {
                        UIApplication.shared.open(url)
                    }
                }
            )
        }
    }

    private func infoCard(detail: CafeDetail) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            infoRow(icon: "mappin.and.ellipse", title: String(localized: String.LocalizationValue("cafe_info_label_address"), table: "Localizable"), value: detail.cafe.region.address)
            infoRow(
                icon: "clock.fill",
                title: String(localized: String.LocalizationValue("cafe_info_label_business_hours"), table: "Localizable"),
                value: detail.businessHours.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? String(localized: String.LocalizationValue("cafe_info_placeholder_business_hours"), table: "Localizable")
                    : detail.businessHours
            )
            infoRow(
                icon: "phone.fill",
                title: String(localized: String.LocalizationValue("cafe_info_label_phone"), table: "Localizable"),
                value: detail.phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? String(localized: String.LocalizationValue("cafe_info_placeholder_phone"), table: "Localizable")
                    : detail.phoneNumber
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
    
    private func infoRow(icon: String, title: String, value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(Color(hex: "EF6797"))
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Text(value)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }
    
    private func descriptionCard(detail: CafeDetail) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: String.LocalizationValue("cafe_info_section_description"), table: "Localizable"))
                .font(.subheadline.weight(.semibold))
            Text(detail.cafe.desc)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "666666"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    @ViewBuilder
    private func socialMediaCard(detail: CafeDetail) -> some View {
        let cafe = detail.cafe
        let socialMedia = cafe.socialMedia
        let items: [(label: String, url: String, color: Color)] = {
            var result: [(String, String, Color)] = []

            if let id = socialMedia["instagram"], !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                let trimmed = id.trimmingCharacters(in: .whitespacesAndNewlines)
                result.append(("Instagram", "https://instagram.com/\(trimmed)", Color(hex: "E1306C")))
            }
            if let id = socialMedia["twitter"], !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                let trimmed = id.trimmingCharacters(in: .whitespacesAndNewlines)
                result.append(("X (Twitter)", "https://x.com/\(trimmed)", Color(hex: "1DA1F2")))
            }
            if let id = socialMedia["tiktok"], !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                let trimmed = id.trimmingCharacters(in: .whitespacesAndNewlines)
                result.append(("TikTok", "https://tiktok.com/@\(trimmed)", Color(hex: "010101")))
            }
            if let id = socialMedia["youtube"], !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                let trimmed = id.trimmingCharacters(in: .whitespacesAndNewlines)
                result.append(("YouTube", "https://youtube.com/@\(trimmed)", Color(hex: "FF0000")))
            }
            return result
        }()

        if items.isEmpty {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: 10) {
                Text(String(localized: String.LocalizationValue("cafe_info_section_social_media"), table: "Localizable"))
                    .font(.subheadline.weight(.semibold))
                let rows = stride(from: 0, to: items.count, by: 2).map { i in
                    Array(items[i..<min(i + 2, items.count)])
                }

                ForEach(rows.indices, id: \.self) { rowIndex in
                    HStack(spacing: 10) {
                        ForEach(rows[rowIndex].indices, id: \.self) { colIndex in
                            let item = rows[rowIndex][colIndex]

                            Link(destination: URL(string: item.url) ?? URL(string: "https://")!) {
                                Text(item.label)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(item.color)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color(hex: "F5EDF4"))
                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                        }
                        if rows[rowIndex].count == 1 {
                            Color.clear
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
    }

    private func reservationButton(reservationUrl: String?, onTapped: @escaping (String) -> Void) -> some View {
        Button {
            if let url = reservationUrl {
                onTapped(url)
            }
        } label: {
            HStack {
                Spacer()
                Text(String(localized: String.LocalizationValue("cafe_info_action_reserve"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                Spacer()
            }
            .padding(.vertical, 14)
        }
        .buttonStyle(.borderedProminent)
        .tint(Color(hex: "FFD1DC"))
        .foregroundStyle(Color(hex: "2B2330"))
        .frame(maxWidth: .infinity)
        .disabled(reservationUrl == nil || reservationUrl?.isEmpty == true)
    }
}

/*struct CafeInfoView_Previews: PreviewProvider {
    static var previews: some View {
        CafeInfoView(cafeDetail: <#CafeDetail#>)
    }
}*/
