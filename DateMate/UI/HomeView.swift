//
//  HomeView.swift
//  DateMate
//
//  Created by 홍희표 on 2021/12/04.
//

import SwiftUI

struct HomeView: View{
    
    @ObservedObject var viewModel = HomeViewModel()
    
    @State private var arrayPicB: [String] = ["bakery01","bakery02","bakery02","bakery02","bakery02","bakery02","bakery02","bakery02","bakery02"]
    
    var body: some View{
        ScrollView(){
            VStack(alignment: .leading){
                Text("Recommended People for today")
    
              
                LazyHStack() {
                    
                    RoundedRectangle(cornerRadius: 10)
                        .frame(width: UIScreen.screenWidth/2.3,height: UIScreen.screenWidth/1.5)
                        .foregroundColor(.red)
                    RoundedRectangle(cornerRadius: 10)
                        .foregroundColor(.yellow)
                        .frame(width:UIScreen.screenWidth/2.3,height: UIScreen.screenWidth/1.5)
                    
                }
            }
            .padding(.bottom,20)
        
            
            VStack(alignment: .leading){
                Text("Famous People")
                    .padding(.leading, 20)
                
                    ScrollView(.horizontal) {
                        
                        LazyHStack() {
                            
                            ForEach(0..<arrayPicB.count, id: \.self) {i in
                                RoundedRectangle(cornerRadius: 10)
                                    .frame(width: UIScreen.screenWidth/3,height: UIScreen.screenWidth/3)

                            }
                        }
                        .padding(.leading, 20)
                    }
            }
            
  
            
            VStack(alignment: .leading){
                Text("T.O.P Supporter")
                    .padding(.leading, 20)
                
                    ScrollView(.horizontal) {
                        
                        LazyHStack() {
                            
                            ForEach(0..<arrayPicB.count, id: \.self) {i in
                                RoundedRectangle(cornerRadius: 10)
                                    .frame(width: UIScreen.screenWidth/3,height: UIScreen.screenWidth/3)

                            }
                        }
                        .padding(.leading, 20)
                    }
            }
            Button {
                print("인기 스타되기")
            } label: {
                Text("Be a TOP Supporter")
                    .frame(width: 200)
                    .foregroundColor(.red)
            }
            
            Button {
                print("더 추천 받기")
            } label: {
                Text("New Recommendation")
                    .frame(width: 200)
                    .foregroundColor(.red)
            }
            


        }
    }
}
struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}






//            Section(header: Text("Recommended People for today"), content: {
//                LazyHStack(alignment: .center) {
//
//                        RoundedRectangle(cornerRadius: 10)
//                        .frame(width: UIScreen.screenWidth/2.5,height: 100)
//                            .foregroundColor(.red)
//                        RoundedRectangle(cornerRadius: 10)
//                            .foregroundColor(.yellow)
//                            .frame(width:UIScreen.screenWidth/2.5,height: 100)
//
//                    }
//
//            })
