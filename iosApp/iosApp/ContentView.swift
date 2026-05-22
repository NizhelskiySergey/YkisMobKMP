import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // РЕШЕНИЕ: Запрашиваем дефолтный размер напрямую через нашу Kotlin-фабрику!
        // Ошибки "Missing arguments" навсегда уничтожены со сборщика Apple!
        let defaultWindowSize = MainViewControllerKt.createDefaultWindowSizeClass()

        return MainViewControllerKt.MainViewController(
            windowSize: defaultWindowSize,
            initialChatId: nil
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
