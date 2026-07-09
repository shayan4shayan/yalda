import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        IosNativeTranslations.shared.register(bridge: IOSMlKitTranslationBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
