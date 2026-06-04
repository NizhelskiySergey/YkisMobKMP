import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import ComposeApp // ИСПРАВЛЕНО: Импортируем наше КМР-ядро

@main
struct iOSApp: App {
    init() {
        // 1. Инициализируем Koin DI до старта любых UI-компонентов
        Koin_iosKt.doInitIosKoin()
        
        // 2. Настраиваем App Check для симуляторов
        #if DEBUG
        let providerFactory = AppCheckDebugProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        #endif
        
        // 3. Запускаем Firebase
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
