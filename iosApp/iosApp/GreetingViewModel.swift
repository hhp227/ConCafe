import Foundation
import Combine
import Shared

final class GreetingViewModel: ObservableObject {
    @Published var showContent = false
    @Published var greeting = ""

    func toggleContent() {
        showContent.toggle()
        greeting = showContent ? Greeting().greet() : ""
    }
}
