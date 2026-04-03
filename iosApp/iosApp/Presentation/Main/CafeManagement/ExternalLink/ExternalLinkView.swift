//
//  ExternalLinkView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import WebKit

struct ExternalLinkView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: ExternalLinkViewModel

    var body: some View {
        ExternalLinkContentView(
            uiState: viewModel.uiState
        )
        .navigationTitle(viewModel.uiState.displayTitle.isEmpty ? String(localized: String.LocalizationValue("externallink_title"), table: "Localizable") : viewModel.uiState.displayTitle)
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }

    init(
        title: String,
        url: String,
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(
            wrappedValue: ExternalLinkViewModel(
                title: title,
                url: url
            )
        )
    }
}

private struct ExternalLinkContentView: View {
    let uiState: ExternalLinkUiState

    var body: some View {
        ExternalWebView(urlString: uiState.url)
            .background(Color.white)
    }
}

private struct ExternalWebView: UIViewRepresentable {
    let urlString: String

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.allowsBackForwardNavigationGestures = true
        load(urlString, into: webView)
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard webView.url?.absoluteString != urlString else { return }
        load(urlString, into: webView)
    }

    private func load(_ urlString: String, into webView: WKWebView) {
        guard let url = URL(string: urlString) else { return }
        webView.load(URLRequest(url: url))
    }
}

struct ExternalLinkView_Previews: PreviewProvider {
    static var previews: some View {
        CompatNavigationContainer {
            ExternalLinkView(
                title: "ConCafe",
                url: "https://example.com"
            )
        }
    }
}

