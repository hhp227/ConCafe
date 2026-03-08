//
//  AppNavigationView.swift
//  iosApp
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct AppNavigationView: View {
    @StateObject private var viewModel = NavigationViewModel()
    
    @State var path: [Route] = []
    
    @State var currentRoute: Route = .entry
    
    var body: some View {
        NavigationStackCompat(path: $path) {
            rootContent
                .onAppear {
                    if case .entry = currentRoute {
                        viewModel.onAction(.navigateToMain())
                    }
                }
        } destination: { route in
            switch route {
            case .cast(let param):
                CastView(castId: param, onNavigationAction: viewModel.onAction)
            case .cafe(let param):
                CafeView(cafeId: param, onNavigationAction: viewModel.onAction)
            case .signIn:
                SignInView(onNavigationAction: viewModel.onAction)
            case .notification:
                NotificationView()
            case .main:
                MainView(onNavigationAction: viewModel.onAction)
            case .entry:
                EmptyView()
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateTo(let route):
                switch route {
                case .main(let initialTab):
                    currentRoute = .main(initialTab: initialTab)
                    path.removeAll()
                case .cast:
                    path.append(route)
                case .cafe:
                    path.append(route)
                case .signIn:
                    path.append(route)
                case .notification:
                    path.append(route)
                case .entry:
                    currentRoute = .entry
                }
            case .navigateBack:
                if !path.isEmpty {
                    path.removeLast()
                }
            }
        }
    }
    
    @ViewBuilder
    private var rootContent: some View {
        switch currentRoute {
        case .main:
            MainView(onNavigationAction: viewModel.onAction)
        case .entry:
            ProgressView()
        default:
            // Detail is pushed through NavigationStack path.
            MainView(onNavigationAction: viewModel.onAction)
        }
    }
}

struct AppNavigationView_Previews: PreviewProvider {
    static var previews: some View {
        AppNavigationView()
    }
}
