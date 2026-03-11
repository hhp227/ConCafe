//
//  MenuGoodsEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI

struct MenuGoodsEditView: View {
    let cafeId: String

    let itemId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: MenuGoodsEditViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(viewModel.uiState.screenTitle)
                    .font(.title2.weight(.bold))
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(viewModel.uiState.isEditMode ? "항목 정보를 수정합니다." : "새 항목을 등록합니다.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                if let infoMessage = viewModel.uiState.infoMessage {
                    Text(infoMessage)
                        .font(.footnote)
                        .foregroundStyle(Color(hex: "6B5320"))
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: "FFF6D7"))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                Group {
                    TextField(
                        "항목명",
                        text: Binding(
                            get: { viewModel.uiState.itemName },
                            set: { viewModel.onAction(.changeName($0)) }
                        )
                    )
                    .textFieldStyle(.roundedBorder)
                    TextField(
                        "가격",
                        text: Binding(
                            get: { viewModel.uiState.price },
                            set: { viewModel.onAction(.changePrice($0)) }
                        )
                    )
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)

                    TextEditor(
                        text: Binding(
                            get: { viewModel.uiState.description },
                            set: { viewModel.onAction(.changeDescription($0)) }
                        )
                    )
                    .frame(minHeight: 120)
                    .padding(8)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    Toggle(
                        "재고 있음",
                        isOn: Binding(
                            get: { viewModel.uiState.isInStock },
                            set: { viewModel.onAction(.toggleStock($0)) }
                        )
                    )
                }
                Button {
                    viewModel.onAction(.clickSave)
                } label: {
                    HStack {
                        Spacer()
                        if viewModel.uiState.isSaving {
                            ProgressView()
                                .progressViewStyle(.circular)
                        } else {
                            Text(viewModel.uiState.saveButtonLabel)
                                .font(.headline.weight(.bold))
                        }
                        Spacer()
                    }
                    .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(hex: "FFD1DC"))
                .foregroundStyle(Color(hex: "2B2330"))
                .disabled(viewModel.uiState.isSaving || viewModel.uiState.isLoading)
            }
            .padding(16)
        }
        .navigationTitle(viewModel.uiState.screenTitle)
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
        itemId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.itemId = itemId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: MenuGoodsEditViewModel(cafeId: cafeId, itemId: itemId))
    }
}

struct MenuGoodsEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            MenuGoodsEditView(cafeId: "cafe-1", itemId: nil, onNavigationAction: { _ in })
        }
    }
}
