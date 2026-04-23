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

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeEventViewModel

    var body: some View {
        Group {
            if viewModel.uiState.isLoading {
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let event = viewModel.uiState.event {
                CafeEventDetailContent(event: event)
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
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.eventId = eventId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeEventViewModel(cafeId: cafeId, eventId: eventId))
    }
}

private struct CafeEventDetailContent: View {
    let event: CafeEventManagementItem

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                heroImage
                    .padding(.horizontal, 16)
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
        ZStack {
            if let imageUrl = resolvedImageUrl(event.imageUrl) {
                CachedAsyncImage(url: imageUrl, placeholder: Color.clear, displaySize: .full)
                    .frame(maxWidth: .infinity, minHeight: 280, maxHeight: 280)
                    .clipped()
            } else {
                LinearGradient(
                    colors: [Color(hex: "FDE7EF"), Color(hex: "FCCFDF")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
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
