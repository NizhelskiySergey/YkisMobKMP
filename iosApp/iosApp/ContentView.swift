import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let defaultWindowSize = MainViewControllerKt.createDefaultWindowSizeClass()

        return MainViewControllerKt.MainViewController(
            windowSize: defaultWindowSize
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
