package com.hhp227.concafe.di

import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.JvmKakaoIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.JvmGoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.KakaoIdTokenProvider
import com.hhp227.concafe.presentation.auth.signup.JvmPhoneAuthProvider
import com.hhp227.concafe.presentation.auth.signup.JvmSocialFirebaseAuthProvider
import com.hhp227.concafe.presentation.auth.signup.PhoneAuthProvider
import com.hhp227.concafe.presentation.auth.signup.SocialFirebaseAuthProvider
import com.hhp227.concafe.presentation.main.checkin.CheckInLocationProvider
import com.hhp227.concafe.presentation.main.checkin.JvmCheckInLocationProvider
import com.hhp227.concafe.presentation.theme.JvmThemePreferenceStore
import com.hhp227.concafe.presentation.theme.ThemePreferenceStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun jvmPlatformModules(): List<Module> {
    return listOf(
        module {
            single<ThemePreferenceStore> { JvmThemePreferenceStore() }
            single<GoogleIdTokenProvider> { JvmGoogleIdTokenProvider() }
            single<KakaoIdTokenProvider> { JvmKakaoIdTokenProvider() }
            single<CheckInLocationProvider> { JvmCheckInLocationProvider() }
            single<PhoneAuthProvider> { JvmPhoneAuthProvider() }
            single<SocialFirebaseAuthProvider> { JvmSocialFirebaseAuthProvider() }
        }
    )
}
