package com.ykis.ykismobkmp.domain.services

import dev.gitlive.firebase.auth.FirebaseUser
import com.ykis.ykismobkmp.core.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
typealias SignInWithGoogleResponse = Resource<Boolean>
typealias SignUpResponse = Resource<Boolean>
typealias SendEmailVerificationResponse = Resource<Boolean>
typealias SignInResponse = Resource<Boolean>
typealias addUserFirestoreResponse = Resource<Boolean>
typealias ReloadUserResponse = Resource<Boolean>
typealias SendPasswordResetEmailResponse = Resource<Boolean>
typealias AuthStateResponse = StateFlow<Boolean>
interface FirebaseService {
  val isUserAuthenticatedInFirebase: Boolean
  val uid: String
  val hasUser: Boolean
  val isEmailVerified: Boolean?
  val currentUser: FirebaseUser? // Использует кроссплатформенную обертку GitLive
  val displayName: String
  val providerId: String
  val photoUrl: String
  val email: String
  val isWiFiCheckConfig: Boolean
  val isMobileCheckConfig: Boolean
  val agreementTitle: String
  val agreementText: String
  suspend fun fetchConfiguration(): Boolean
  suspend fun isUserAgreed(): Boolean
  suspend fun setUserAgreed(agreed: Boolean)
  suspend fun authenticate(email: String, password: String)
  suspend fun sendRecoveryEmail(email: String)
  suspend fun linkAccount(email: String, password: String)
  suspend fun deleteAccount()
  suspend fun signOut()
  suspend fun firebaseSignInWithGoogle(idToken: String): SignInWithGoogleResponse
  suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse
  suspend fun sendEmailVerification(): SendEmailVerificationResponse
  suspend fun sendPasswordResetEmail(email: String): SendPasswordResetEmailResponse
  suspend fun sendSmsCode(phoneNumber: String, platformActivity: Any?): Resource<String>
  suspend fun signInWithSmsCode(verificationId: String, smsCode: String): Resource<Boolean>
  fun getProvider(viewModelScope: CoroutineScope): String
  suspend fun firebaseSignInWithEmailAndPassword(email: String, password: String)
  suspend fun reloadFirebaseUser(): ReloadUserResponse
  suspend fun revokeAccess(): Resource<Boolean>
  suspend fun addUserFirestore(): addUserFirestoreResponse
  fun getAuthState(viewModelScope: CoroutineScope): AuthStateResponse
  suspend fun getUserProfile(): UserFirebase
  suspend fun updateUserRoleAndPermissions(
    uid: String,
    addressId: Long?, // Переведено на Long под типы SQLDelight
    userRole: UserRole,
    osbbId: Long?,    // Переведено на Long под типы SQLDelight
    displayName: String? = null,
    fio: String? = null,
    osbb: String? = null // НОВОЕ ПОЛЕ
  )
  suspend fun getUid(): String
  suspend fun getEmail(): String
  suspend fun getDisplayName(): String
  suspend fun addFcmToken()
  suspend fun removeFcmToken()
  fun clearNotifications(chatId: String? = null)
  fun stopAllListeners()
}
