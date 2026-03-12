//
//  NavigationStackCompat.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct NavigationStackCompat<Root: View>: View {
    @Binding private var path: [Route]
    
    private let root: Root
    
    private let destination: (Route) -> AnyView

    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack(path: $path) {
                root
                    .navigationDestination(for: Route.self) { route in
                        destination(route)
                    }
            }
        } else {
            LegacyNavigationStack(
                path: $path,
                root: root,
                destination: destination
            )
        }
    }
    
    init(
        path: Binding<[Route]>,
        @ViewBuilder root: () -> Root,
        @ViewBuilder destination: @escaping (Route) -> some View
    ) {
        self._path = path
        self.root = root()
        self.destination = { AnyView(destination($0)) }
    }
}

private struct LegacyNavigationStack<Root: View>: View {
    @Binding var path: [Route]
    
    let root: Root
    
    let destination: (Route) -> AnyView

    var body: some View {
        NavigationView {
            LegacyNavigationNode(
                path: $path,
                depth: 0,
                root: AnyView(root),
                destination: destination
            )
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

private struct LegacyNavigationNode: View {
    @Binding var path: [Route]
    
    let depth: Int
    
    let root: AnyView
    
    let destination: (Route) -> AnyView

    var body: some View {
        ZStack {
            root
            NavigationLink(
                isActive: Binding(
                    get: { path.count > depth },
                    set: { isActive in
                        if !isActive {
                            path = Array(path.prefix(depth))
                        }
                    }
                ),
                destination: {
                    if path.count > depth {
                        let route = path[depth]

                        LegacyNavigationNode(
                            path: $path,
                            depth: depth + 1,
                            root: destination(route),
                            destination: destination
                        )
                    } else {
                        EmptyView()
                    }
                },
                label: { EmptyView() }
            )
            .hidden()
        }
    }
}
