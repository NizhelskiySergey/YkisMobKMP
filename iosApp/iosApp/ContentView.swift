import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // ИСПРАВЛЕНО: Вызываем MainViewController без параметров.
        // Теперь WindowSizeClass вычисляется динамически внутри KMP на основе реального экрана.
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea() // Позволяем Compose занимать весь экран
    }
}
