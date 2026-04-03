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
            reservationButton()
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

    private func reservationButton() -> some View {
        Button {
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
        .disabled(true)
    }
}

/*struct CafeInfoView_Previews: PreviewProvider {
    static var previews: some View {
        CafeInfoView(cafeDetail: <#CafeDetail#>)
    }
}*/
