import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseAppCheck
import FirebaseMessaging
import UserNotifications
import GoogleSignIn
import AuthenticationServices
import CryptoKit
import FirebaseAILogic
import RecaptchaEnterprise // Импорт нативной библиотеки Google
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate, NativeAuthBridge {
    
    private lazy var ai = FirebaseAI.firebaseAI(backend: .googleAI())
    private lazy var model = ai.generativeModel(modelName: "gemini-3.5-flash")
    
    fileprivate var currentNonce: String?
    
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        print("[YkisLogKMP] Initializing Firebase...")
        
        // Настройка App Check
        let providerFactory = AppCheckDebugProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        
        FirebaseApp.configure()
        
        // --- РУЧНАЯ АКТИВАЦИЯ RECAPTCHA ENTERPRISE ---
        // Это нужно, чтобы убрать статус "Incomplete" в Google Cloud
        let siteKey = "6LeWoGMtAAAAAIiCR5vTQH3VFmkefNfOYDt4SeCV"
        print("[YkisLogKMP.Recaptcha]: Попытка активации ключа \(siteKey)...")
        
        Recaptcha.getClient(siteKey: siteKey) { client, error in
            if let error = error {
                print("[YkisLogKMP.Recaptcha_ERROR]: Ошибка инициализации: \(error.localizedDescription)")
            } else if let client = client {
                print("[YkisLogKMP.Recaptcha_SUCCESS]: Ключ активирован! Запрашиваем токен проверки...")
                let action = RecaptchaAction(action: .login)
                client.execute(action) { (token: String?, error: Error?) in
                    if let token = token {
                        print("[YkisLogKMP.Recaptcha_TOKEN]: Токен получен успешно! Теперь статус в Google Cloud сменится на Active.")
                    } else {
                        print("[YkisLogKMP.Recaptcha_ERROR]: Не удалось выполнить проверку: \(error?.localizedDescription ?? "null")")
                    }
                }
            }
        }
        
        AppInitializer().run(bridge: self)
        
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        Messaging.messaging().delegate = self
        
        return true
    }

    // --- ОСТАЛЬНЫЕ МЕТОДЫ (didRegister, didReceive, и т.д. без изменений) ---
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Auth.auth().setAPNSToken(deviceToken, type: .sandbox)
        Messaging.messaging().apnsToken = deviceToken
        print("[YkisLogKMP.AppDelegate]: APNs токен передано")
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.noData)
    }

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        if Auth.auth().canHandle(url) { return true }
        return GIDSignIn.sharedInstance.handle(url)
    }

    func sendSmsCode(phoneNumber: String, onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        print("[YkisLogKMP.SWIFT_SMS]: Запрос СМС для \(phoneNumber)")
        DispatchQueue.main.async {
            PhoneAuthProvider.provider().verifyPhoneNumber(phoneNumber, uiDelegate: nil) { (vID, error) in
                if let error = error as NSError? {
                    print("[YkisLogKMP.SWIFT_SMS_ERROR]: \(error.localizedDescription) (Код: \(error.code))")
                    onError(error.localizedDescription)
                    return
                }
                onSuccess(vID ?? "")
            }
        }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("[YkisLogKMP.AppDelegate]: FCM токен отримано")
    }

    func signInWithGoogle(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        let window = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.flatMap { $0.windows }.first { $0.isKeyWindow }
        guard let rootVC = window?.rootViewController else { return }
        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { res, err in
            if let err = err { onError(err.localizedDescription); return }
            guard let token = res?.user.idToken?.tokenString else { return }
            onSuccess(token)
        }
    }

    func signInWithApple(onSuccess: @escaping (String, String?, String?) -> Void, onError: @escaping (String) -> Void) {
        let rawNonce = randomNonceString()
        currentNonce = rawNonce
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(rawNonce)
        let ctrl = ASAuthorizationController(authorizationRequests: [request])
        ctrl.delegate = self
        ctrl.presentationContextProvider = self
        self.appleSuccessCallback = onSuccess
        self.appleErrorCallback = onError
        ctrl.performRequests()
    }
    
    private var appleSuccessCallback: ((String, String?, String?) -> Void)?
    private var appleErrorCallback: ((String) -> Void)?

    func generateAiContent(prompt: String, onResult: @escaping (String?, String?) -> Void) {
        Task { do { let res = try await model.generateContent(prompt); DispatchQueue.main.async { onResult(res.text, nil) } } catch { onResult(nil, error.localizedDescription) } }
    }

    func analyzeAiImage(prompt: String, imageBase64: String, onResult: @escaping (String?, String?) -> Void) {
        guard let data = Data(base64Encoded: imageBase64), let img = UIImage(data: data) else { return }
        Task { do { let res = try await model.generateContent(prompt, img); DispatchQueue.main.async { onResult(res.text, nil) } } catch { onResult(nil, error.localizedDescription) } }
    }
}

extension AppDelegate: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.flatMap { $0.windows }.first { $0.isKeyWindow } ?? UIWindow()
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization auth: ASAuthorization) {
        if let cred = auth.credential as? ASAuthorizationAppleIDCredential, let token = cred.identityToken, let tokenStr = String(data: token, encoding: .utf8) {
            let code = cred.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
            appleSuccessCallback?(tokenStr, currentNonce, code)
        }
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError err: Error) { appleErrorCallback?(err.localizedDescription) }
}

private func randomNonceString(length: Int = 32) -> String {
    let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
    var result = ""
    for _ in 0..<length { result.append(charset.randomElement()!) }
    return result
}

private func sha256(_ input: String) -> String {
    let data = Data(input.utf8)
    let hash = SHA256.hash(data: data)
    return hash.compactMap { String(format: "%02x", $0) }.joined()
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    var body: some Scene { WindowGroup { ContentView() } }
}
