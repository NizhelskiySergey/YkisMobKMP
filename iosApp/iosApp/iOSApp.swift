import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // 1. Устанавливаем Debug-провайдер для App Check (важно для симулятора)
        let providerFactory = AppCheckDebugProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        
        // 2. Базовая настройка Firebase
        FirebaseApp.configure()
        
        // 3. Запуск KMP инициализатора
        AppInitializer().run()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
