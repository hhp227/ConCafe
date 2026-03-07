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
            case .cast:
                CastView(onNavigationAction: viewModel.onAction)
            case .cafe:
                CafeView(onNavigationAction: viewModel.onAction)
            case .signIn:
                SignInView(onNavigationAction: viewModel.onAction)
            case .notification:
                NotificationView()
            case .main(let initialTab):
                MainView(
                    initialTab: initialTab,
                    onNavigationAction: viewModel.onAction
                )
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
            }
        }
    }
    
    @ViewBuilder
    private var rootContent: some View {
        switch currentRoute {
        case .main(let initialTab):
            MainView(
                initialTab: initialTab,
                onNavigationAction: viewModel.onAction
            )
        case .entry:
            ProgressView()
        default:
            // Detail is pushed through NavigationStack path.
            MainView(
                initialTab: nil,
                onNavigationAction: viewModel.onAction
            )
        }
    }
}

struct AppNavigationView_Previews: PreviewProvider {
    static var previews: some View {
        AppNavigationView()
    }
}
