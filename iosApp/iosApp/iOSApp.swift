import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import FirebaseMessaging
import UserNotifications
import GoogleSignIn
import AuthenticationServices
import CryptoKit
import FirebaseAILogic
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate, NativeAuthBridge {
    
    // Ініціалізація згідно з документацією Firebase 2026
    private lazy var ai = FirebaseAI.firebaseAI(backend: .googleAI())
    private lazy var model = ai.generativeModel(modelName: "gemini-3.5-flash")
    
    // Переменная для хранения nonce (защита Apple Sign In)
    fileprivate var currentNonce: String?
    
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        #if DEBUG
        let providerFactory = AppCheckDebugProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        #endif
        
        FirebaseApp.configure()
        
        AppInitializer().run(bridge: self)
        
        UNUserNotificationCenter.current().delegate = self
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { granted, error in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        
        Messaging.messaging().delegate = self
        
        return true
    }

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }

    // --- GOOGLE SIGN IN ---
    func signInWithGoogle(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }

        guard let rootViewController = window?.rootViewController else {
            onError("Root View Controller not found")
            return
        }
        
        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { signInResult, error in
            if let error = error {
                onError(error.localizedDescription)
                return
            }
            guard let user = signInResult?.user, let idToken = user.idToken?.tokenString else {
                onError("Failed to get ID Token")
                return
            }
            onSuccess(idToken)
        }
    }
    
    // --- APPLE SIGN IN ---
    func signInWithApple(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        let nonce = randomNonceString()
        currentNonce = nonce
        
        let appleIDProvider = ASAuthorizationAppleIDProvider()
        let request = appleIDProvider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
        
        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        
        // Сохраняем колбэки во временных переменных для использования в делегате
        self.appleSuccessCallback = onSuccess
        self.appleErrorCallback = onError
        
        authorizationController.performRequests()
    }
    
    private var appleSuccessCallback: ((String) -> Void)?
    private var appleErrorCallback: ((String) -> Void)?

    // Уведомления
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("[YkisLogKMP.AppDelegate]: Актуальный FCM токен: \(fcmToken ?? "пусто")")
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([[.banner, .sound, .badge]])
    }

    // --- FIREBASE AI LOGIC (iOS) ---
    func generateAiContent(prompt: String, onResult: @escaping (String?, String?) -> Void) {
        Task {
            do {
                // У нових версіях SDK метод очікує або рядок, або масив ModelContent
                let response = try await model.generateContent(prompt)
                DispatchQueue.main.async {
                    onResult(response.text, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    print("[YkisLogKMP.AI_ERROR]: \(error.localizedDescription)")
                    onResult(nil, error.localizedDescription)
                }
            }
        }
    }

    func analyzeAiImage(prompt: String, imageBase64: String, onResult: @escaping (String?, String?) -> Void) {
        guard let data = Data(base64Encoded: imageBase64),
              let image = UIImage(data: data) else {
            onResult(nil, "Invalid image data")
            return
        }
        
        Task {
            do {
                // У FirebaseAILogic для Swift:
                // String та UIImage автоматично відповідають протоколу PartsRepresentable.
                // Ми передаємо їх прямо у метод generateContent.
                let response = try await model.generateContent(prompt, image)

                DispatchQueue.main.async {
                    onResult(response.text, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    print("[YkisLogKMP.AI_VISION_ERROR]: \(error.localizedDescription)")
                    onResult(nil, error.localizedDescription)
                }
            }
        }
    }
}

// РЕАЛИЗАЦИЯ ДЕЛЕГАТОВ APPLE AUTH
extension AppDelegate: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        return window ?? UIWindow()
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
            guard let identityToken = appleIDCredential.identityToken,
                  let idTokenString = String(data: identityToken, encoding: .utf8) else {
                appleErrorCallback?("Unable to fetch identity token")
                return
            }
            appleSuccessCallback?(idTokenString)
        }
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        appleErrorCallback?(error.localizedDescription)
    }
}

// ХЕЛПЕРЫ ДЛЯ БЕЗОПАСНОСТИ APPLE AUTH
private func randomNonceString(length: Int = 32) -> String {
    precondition(length > 0)
    let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
    var result = ""
    var remainingLength = length
    while remainingLength > 0 {
        let randoms: [UInt8] = (0..<16).map { _ in UInt8.random(in: 0...255) }
        randoms.forEach { random in
            if remainingLength == 0 { return }
            if random < charset.count {
                result.append(charset[Int(random)])
                remainingLength -= 1
            }
        }
    }
    return result
}

private func sha256(_ input: String) -> String {
    let inputData = Data(input.utf8)
    let hashedData = SHA256.hash(data: inputData)
    let hashString = hashedData.compactMap { String(format: "%02x", $0) }.joined()
    return hashString
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
