import SwiftUI

extension View {
    func protectedFromScreenCapture() -> some View {
        ScreenCaptureProtectedView {
            self
        }
    }
}
