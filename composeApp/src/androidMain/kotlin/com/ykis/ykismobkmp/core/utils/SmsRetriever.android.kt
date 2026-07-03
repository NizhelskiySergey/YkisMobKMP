package com.ykis.ykismobkmp.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * [SmsBroadcastReceiver] — Нативный Android-перехватчик событий SMS Retriever API.
 */
class SmsBroadcastReceiver(
    private val onCodeReceived: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras ?: return
            
            // Использование типизированного получения Status для предотвращения Warning в Android 13+
            val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(SmsRetriever.EXTRA_STATUS, Status::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.get(SmsRetriever.EXTRA_STATUS) as? Status
            } ?: return

            when (status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // Извлекаем сообщение целиком (безопасное приведение)
                    val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE) ?: ""
                    println("[YkisLogKMP.SmsBroadcastReceiver]: Получено SMS: $message")

                    // Регулярное выражение для поиска 6-значного цифрового кода
                    val otpRegex = "(\\d{6})".toRegex()
                    val matchResult = otpRegex.find(message)
                    val otpCode = matchResult?.value

                    if (otpCode != null) {
                        println("[YkisLogKMP.SmsBroadcastReceiver]: Извлечен код: $otpCode")
                        onCodeReceived(otpCode)
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    println("[YkisLogKMP.SmsBroadcastReceiver]: Время ожидания SMS истекло (Timeout)")
                }
            }
        }
    }
}

/**
 * [AndroidSmsRetriever] — Реализация ретривера для Android.
 */
class AndroidSmsRetriever(private val context: Context) : com.ykis.ykismobkmp.core.utils.SmsRetriever {
    private var receiver: SmsBroadcastReceiver? = null

    init {
        // ВЫВОД ХЭША ДЛЯ FIREBASE (Скопируйте из логов при открытии экрана)
        logAppHash()
    }

    override fun startRetriever(onCodeReceived: (String) -> Unit) {
        println("[YkisLogKMP.AndroidSmsRetriever]: Запуск Google Play Services SMS Retriever...")
        
        val client = SmsRetriever.getClient(context)
        val task = client.startSmsRetriever()

        task.addOnSuccessListener {
            println("[YkisLogKMP.AndroidSmsRetriever]: Слушатель успешно зарегистрирован в Google Play Services")
            receiver = SmsBroadcastReceiver(onCodeReceived)
            
            // Регистрируем ресивер в системе с учетом флагов экспорта для Android 14+
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            
            // Используем ContextCompat для безопасной регистрации с флагами на всех версиях Android
            @Suppress("WrongConstant")
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                SmsRetriever.SEND_PERMISSION,
                null,
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        task.addOnFailureListener { e ->
            println("[YkisLogKMP.AndroidSmsRetriever]: Ошибка регистрации слушателя: ${e.message}")
        }
    }

    override fun stopRetriever() {
        receiver?.let {
            println("[YkisLogKMP.AndroidSmsRetriever]: Остановка и разрегистрация ресивера")
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Игнорируем, если уже был разрегистрирован
            }
            receiver = null
        }
    }

    /**
     * [logAppHash] — Генерирует и выводит в логи 11-символьный хэш приложения.
     * ЭТОТ ХЭШ НУЖНО ДОБАВИТЬ В КОНЕЦ ТЕКСТА SMS В FIREBASE!
     */
    private fun logAppHash() {
        val packageName = context.packageName
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES).signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    val appInfo = "$packageName ${signature.toCharsString()}"
                    val messageDigest = MessageDigest.getInstance("SHA-256")
                    messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
                    var hashSignature = messageDigest.digest()

                    // Берем первые 9 байт
                    hashSignature = hashSignature.copyOfRange(0, 9)
                    val base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
                    val finalHash = base64Hash.substring(0, 11)

                    println("[YkisLogKMP.SMS_CONFIG]: ВАШ ХЭШ ДЛЯ SMS: $finalHash")
                    println("[YkisLogKMP.SMS_CONFIG]: ПРИМЕР ШАБЛОНА: <#> Ваш код: 123456. $finalHash")
                }
            }
        } catch (e: Exception) {
            println("[YkisLogKMP.SMS_CONFIG_ERROR]: Не удалось рассчитать хэш: ${e.message}")
        }
    }
}

@Composable
actual fun rememberSmsRetriever(): com.ykis.ykismobkmp.core.utils.SmsRetriever {
    val context = LocalContext.current
    return remember(context) { AndroidSmsRetriever(context) }
}
