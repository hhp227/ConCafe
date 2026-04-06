import SwiftUI
import Shared
import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import FirebaseAuth
import KakaoSDKCommon
import KakaoSDKAuth
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    init() {
#if canImport(GoogleMobileAds)
        SharedPlatformModule_iosKt.doInitKoinForIos(nativeAdDataSource: IosNativeAdDataSourceImpl())
#else
        KoinInitializerKt.doInitKoin()
#endif
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
#if canImport(GoogleMobileAds)
        GADMobileAds.sharedInstance().start(completionHandler: nil)
#endif
        KakaoSDK.initSDK(appKey: "af25c4820b65d3fe2a9145156351ccaf")
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in
        }
        application.registerForRemoteNotifications()
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        if Auth.auth().canHandle(url) {
            return true
        } else if AuthApi.isKakaoTalkLoginUrl(url) {
            return AuthController.handleOpenUrl(url: url)
        } else {
            return false
        }
    }

    func application(
        _ application: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        let callbackUrl = userActivity.webpageURL

        if let callbackUrl, Auth.auth().canHandle(callbackUrl) {
            return true
        } else if let callbackUrl, AuthApi.isKakaoTalkLoginUrl(callbackUrl) {
            return AuthController.handleOpenUrl(url: callbackUrl)
        } else {
            return false
        }
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        #if DEBUG
        Auth.auth().setAPNSToken(deviceToken, type: .sandbox)
        #else
        Auth.auth().setAPNSToken(deviceToken, type: .prod)
        #endif
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs registration failed: \(error.localizedDescription)")
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
        } else {
            completionHandler(.newData)
        }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        let token = fcmToken?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        if token.isEmpty {
            return
        } else {
            PushTokenBridge.shared.updateToken(token)
        }
    }
}

final class PushTokenBridge {
    static let shared = PushTokenBridge()

    private let tokenKey = "concafe.push.fcm.token"

    private init() {}

    func updateToken(_ token: String) {
        let normalizedToken = token.trimmingCharacters(in: .whitespacesAndNewlines)

        if normalizedToken.isEmpty {
            return
        } else {
            UserDefaults.standard.set(normalizedToken, forKey: tokenKey)
            NotificationCenter.default.post(
                name: .pushTokenUpdated,
                object: nil,
                userInfo: ["token": normalizedToken]
            )
        }
    }

    func currentToken() -> String {
        let token = UserDefaults.standard.string(forKey: tokenKey) ?? ""
        return token.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

extension Notification.Name {
    static let pushTokenUpdated = Notification.Name("concafe.pushTokenUpdated")
}
