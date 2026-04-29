//
//  CafeEventView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/16.
//

import SwiftUI
import UIKit
import Shared

struct CafeEventView: View {
    let cafeId: String

    let eventId: String

    let showCafeButton: Bool

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeEventViewModel

    var body: some View {
        Group {
            if viewModel.uiState.isLoading {
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let event = viewModel.uiState.event {
                CafeEventDetailContent(
                    event: event,
                    showCafeButton: showCafeButton,
                    onCafeClick: {
                        onNavigationAction(.replaceWithCafe(id: cafeId))
                    }
                )
            } else {
                CafeEventErrorContent(
                    message: viewModel.uiState.errorMessage == "Event not found."
                        ? String(localized: String.LocalizationValue("noticeevent_validation_event_required"), table: "Localizable")
                        : String(localized: String.LocalizationValue("noticeevent_info_event_load_failed"), table: "Localizable"),
                    onRetry: { viewModel.onAction(.retry) }
                )
            }
        }
        .background(Color(uiColor: .systemBackground))
        .navigationTitle(String(localized: String.LocalizationValue("noticeevent_tab_event"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }

    init(
        cafeId: String,
        eventId: String,
        showCafeButton: Bool = false,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.eventId = eventId
        self.showCafeButton = showCafeButton
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeEventViewModel(cafeId: cafeId, eventId: eventId))
    }
}

private struct CafeEventDetailContent: View {
    let event: CafeEventManagementItem

    let showCafeButton: Bool

    let onCafeClick: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                heroImage
                VStack(alignment: .leading, spacing: 14) {
                    Text(event.statusLabel)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: "9E2E5C"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(Color(hex: "FDE7EF"), in: Capsule())
                    Text(event.title)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.primary)
                    HStack(spacing: 6) {
                        Image(systemName: "calendar")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(event.periodText)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    if showCafeButton {
                        Button {
                            onCafeClick()
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "storefront.fill")
                                Text(String(localized: String.LocalizationValue("cafeevent_go_to_cafe"), table: "Localizable"))
                                    .fontWeight(.bold)
                            }
                            .font(.subheadline)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color(hex: "EF6797"))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                    Text(event.content)
                        .font(.body)
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
            .padding(.top, 12)
        }
    }

    private var heroImage: some View {
        GeometryReader { proxy in
            ZStack {
                if let imageUrl = resolvedImageUrl(event.imageUrl) {
                    CachedAsyncImage(url: imageUrl, placeholder: Color.clear, displaySize: .full)
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .clipped()
                } else {
                    LinearGradient(
                        colors: [Color(hex: "FDE7EF"), Color(hex: "FCCFDF")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
            .background(Color(uiColor: .secondarySystemBackground))
            .clipped()
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
    }

    private func resolvedImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }
}

private struct CafeEventErrorContent: View {
    let message: String

    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(String(localized: String.LocalizationValue("cafe_error_retry_prompt"), table: "Localizable")) {
                onRetry()
            }
            .font(.subheadline.weight(.semibold))
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
