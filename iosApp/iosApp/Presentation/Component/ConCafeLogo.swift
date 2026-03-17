//
//  ConCafeLogo.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/17.
//

import SwiftUI

struct ConCafeLogo: View {
    var color: Color = .primary

    var body: some View {
        (
            Text("콘")
                .font(.custom("GoyangDeogyangEB-Regular", size: 22))
                .fontWeight(.bold)
                .foregroundColor(color)
            +
            Text("셉")
                .font(.custom("GoyangDeogyangB-Regular", size: 5))
                .foregroundColor(.gray)
            +
            Text("카")
                .font(.custom("GoyangDeogyangEB-Regular", size: 22))
                .fontWeight(.bold)
                .foregroundColor(color)
            +
            Text("페")
                .font(.custom("GoyangDeogyangB-Regular", size: 5))
                .foregroundColor(.gray)
        )
        /*.onAppear {
            for family in UIFont.familyNames {
                print(family)
                for name in UIFont.fontNames(forFamilyName: family) {
                    print("  \(name)")
                }
            }
        }*/
    }
}

struct ConCafeLogo_Previews: PreviewProvider {
    static var previews: some View {
        ConCafeLogo()
    }
}
