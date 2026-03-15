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
            case .cafeDashboard(let param):
                CafeDashboardView(cafeId: param, onNavigationAction: viewModel.onAction)
            case .banner(let cafeId):
                BannerView(cafeId: cafeId, onNavigationAction: viewModel.onAction)
            case .bannerEdit(let cafeId):
                BannerEditView(initialCafeId: cafeId, onNavigationAction: viewModel.onAction)
            case .externalLink(let title, let url):
                ExternalLinkView(title: title, url: url, onNavigationAction: viewModel.onAction)
            case .cafeInfoEdit(let param, let isRegistrationMode):
                CafeInfoEditView(
                    cafeId: param,
                    isRegistrationMode: isRegistrationMode,
                    onNavigationAction: viewModel.onAction
                )
            case .noticeEvent(let param):
                NoticeEventView(cafeId: param, onNavigationAction: viewModel.onAction)
            case .castEdit(let cafeId, let castId):
                CastEditView(cafeId: cafeId, castId: castId, onNavigationAction: viewModel.onAction)
            case .schedule(let castId):
                ScheduleView(castId: castId, onNavigationAction: viewModel.onAction)
            case .menuGoods(let param):
                MenuGoodsView(cafeId: param, onNavigationAction: viewModel.onAction)
            case .menuGoodsEdit(let cafeId, let itemId):
                MenuGoodsEditView(cafeId: cafeId, itemId: itemId, onNavigationAction: viewModel.onAction)
            case .reviewEdit(let cafeId):
                ReviewEditView(cafeId: cafeId, onNavigationAction: viewModel.onAction)
            case .signIn:
                SignInView(onNavigationAction: viewModel.onAction)
            case .signUp:
                SignUpView(onNavigationAction: viewModel.onAction)
            case .notification:
                NotificationView(onNavigationAction: viewModel.onAction)
            case .settings:
                SettingsView(onNavigationAction: viewModel.onAction)
            case .notificationSettings:
                NotificationSettingsView(onNavigationAction: viewModel.onAction)
            case .accountSettings:
                AccountSettingsView(onNavigationAction: viewModel.onAction)
            case .changePassword:
                ChangePasswordView(onNavigationAction: viewModel.onAction)
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
                case .cafeDashboard:
                    path.append(route)
                case .banner:
                    path.append(route)
                case .bannerEdit:
                    path.append(route)
                case .externalLink:
                    path.append(route)
                case .cafeInfoEdit:
                    path.append(route)
                case .noticeEvent:
                    path.append(route)
                case .castEdit:
                    path.append(route)
                case .schedule:
                    path.append(route)
                case .menuGoods:
                    path.append(route)
                case .menuGoodsEdit:
                    path.append(route)
                case .reviewEdit:
                    path.append(route)
                case .signIn:
                    path.append(route)
                case .signUp:
                    path.append(route)
                case .notification:
                    path.append(route)
                case .settings:
                    path.append(route)
                case .notificationSettings:
                    path.append(route)
                case .accountSettings:
                    path.append(route)
                case .changePassword:
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
