//
//  PostEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import Foundation

struct PostEditUiState {
    var title: String = ""
    var content: String = ""
    var imageUrls: [String] = []
    var imageMaxCount: Int = 5
    var isSubmitting: Bool = false
    var infoMessage: String? = nil
    var isEditMode: Bool = false

    var canSubmit: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
