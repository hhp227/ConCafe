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
    
    @State var currentMainTab: String = "home"
    
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
            case .detail:
                DetailView(
                    onNavigationAction: viewModel.onAction
                )
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
                    currentMainTab = initialTab ?? "home"
                    currentRoute = .main(initialTab: currentMainTab)
                    path.removeAll()
                case .detail(let param):
                    currentRoute = .detail(param: param)
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
        case .detail:
            // Detail is pushed through NavigationStack path.
            MainView(
                initialTab: currentMainTab,
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
