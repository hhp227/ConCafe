//
//  SignInUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

struct SignInUiState {
    var email: String = ""
    
    var password: String = ""
    
    var isLoading: Bool = false
    
    var errorMessage: String?
    
    static let empty = SignInUiState()
}
