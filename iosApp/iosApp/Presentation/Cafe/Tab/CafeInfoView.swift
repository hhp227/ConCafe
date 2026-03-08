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
        }
    }
    
    private func infoCard(detail: CafeDetail) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            infoRow(icon: "mappin.and.ellipse", title: "주소", value: detail.cafe.region.address)
            infoRow(icon: "clock.fill", title: "영업시간", value: detail.businessHours)
            infoRow(icon: "phone.fill", title: "전화번호", value: detail.phoneNumber)
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
            Text("소개")
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
}

/*struct CafeInfoView_Previews: PreviewProvider {
    static var previews: some View {
        CafeInfoView(cafeDetail: <#CafeDetail#>)
    }
}*/
