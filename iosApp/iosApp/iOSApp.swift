import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    init() {
        KoinInitializerKt.doInitKoin()
        FirebaseApp.configure()
    }
}
