import SwiftUI
import FirebaseCore
import FirebaseAppCheck // КРИТИЧЕСКИЙ ИМПОРТ

@main
struct iOSApp: App {
    init() {
        // ИСПРАВЛЕНО: Принудительно запускаем отладочный провайдер для симуляторов
        #if DEBUG
        let providerFactory = AppCheckDebugProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        #endif
        
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
