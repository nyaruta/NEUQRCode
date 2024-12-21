package ink.chyk.neuqrcode

import android.util.*
import kotlinx.coroutines.*
import okhttp3.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class ECodeSession(
  val session: String,
  val xsrfToken: String,
  val expiredAt: Long
)

@Serializable
data class ECodeQRCode(
  val data: List<ECodeData>
)

@Serializable
data class ECodeData(
  val type: Nothing? = null,
  val attributes: ECodeAttributes
)

@Serializable
data class ECodeAttributes(
  val qrCode: String,
  val createTime: Long,
  val qrInvalidTime: Long
)

@Serializable
data class ECodeUserinfo(
  val data: List<ECodeUserinfoData>
)

@Serializable
data class ECodeUserinfoData(
  val type: Nothing? = null,
  val attributes: ECodeUserinfoAttributes
)

@Serializable
data class ECodeUserinfoAttributes(
  val userCode: String,
  val userName: String,
  val unitName: String,
  val idType: String,
)


class NEUPass {
  // 智慧东大 API

  fun useRequestedWith(request: Request.Builder): Request.Builder {
    return request.header("X-Requested-With", "com.sunyt.testdemo")
  }

  suspend fun loginPortalTicket(studentId: String, password: String): String {
    // 登录账号
    val url = "https://portal.neu.edu.cn/mobile/api/auth/tickets"

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val requestBody = FormBody.Builder()
      .add("username", studentId)
      .add("password", password)
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(url)
        .post(requestBody)
    ).build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string()
          if (body?.startsWith("TGT") == true) {
            // 登录成功
            body
          } else {
            throw PasswordIncorrectException()
          }
        } else {
          throw PasswordIncorrectException()
        }
      }
    }
  }

  suspend fun loginECodeTicket(portalTicket: String): String {
    // 拿二维码接口的ticket
    val url =
      "https://pass.neu.edu.cn/tpass/login?service=https://ecode.neu.edu.cn/ecode/api/sso/login"

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(url)
        .header("Cookie", "CASTGC=$portalTicket")
    ).build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        val code = response.code
        if (code == 302) {
          val location = response.header("Location")
          if (location != null && location.contains("?ticket=")) {
            // 提取新的 ticket 参数
            location.substringAfter("?ticket=")
          } else {
            throw TicketFailedException()
          }
        } else {
          throw TicketFailedException()
        }
      }
    }
  }

  suspend fun newECodeSession(): ECodeSession {
    // 获取一个新的 session
    val url = "https://ecode.neu.edu.cn/ecode/api/sso/login"

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(url)
    ).build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        val setCookieHeaders = response.headers("Set-Cookie")
        val session = setCookieHeaders
          .first({ it.startsWith("SESSION") })
          .substringBefore(";")
          .substringAfter("=")
        val xsrfToken = setCookieHeaders
          .first({ it.startsWith("XSRF-TOKEN") })
          .substringBefore(";")
          .substringAfter("=")
        ECodeSession(session, xsrfToken, System.currentTimeMillis() / 1000 + 86400 * 99)
        // 99 天后过期
      }
    }
  }

  suspend fun loginECode(session: ECodeSession, eCodeTicket: String) {
    // 登录二维码
    val url = "https://ecode.neu.edu.cn/ecode/api/sso/login"
    val redirectTo = "https://ecode.neu.edu.cn/ecode/#/"

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val headers = Headers.Builder()
      .add(
        "Cookie",
        "SESSION=${session.session}; XSRF-TOKEN=${session.xsrfToken}; KC_REDIRECT=$redirectTo"
      )
      .add("X-XSRF-TOKEN", session.xsrfToken)
      .build()

    val loginRequest = useRequestedWith(
      Request.Builder()
        .url("$url?ticket=$eCodeTicket")
        .headers(headers)
        .get()
    ).build()

    val afterLoginRequest = useRequestedWith(
      Request.Builder()
        .url(url)
        .headers(headers)
        .get()
    ).build()

    withContext(Dispatchers.IO) {
      client.newCall(loginRequest).execute().use { response ->
        if (response.code == 302) {
          client.newCall(afterLoginRequest).execute().use { response2 ->
            if (!(response2.code == 302 && response2.header("Location") == redirectTo)) {
              throw TicketExpiredException()
            }
            // 否则登录成功
          }
        } else {
          throw TicketExpiredException()
        }
      }
    }
  }

  suspend fun getQRCode(session: ECodeSession): ECodeQRCode {
    // 获取二维码
    val url = "https://ecode.neu.edu.cn/ecode/api/qr-code"

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val headers = Headers.Builder()
      .add(
        "Cookie",
        "SESSION=${session.session}; XSRF-TOKEN=${session.xsrfToken}"
      )
      .add("X-XSRF-TOKEN", session.xsrfToken)
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(url)
        .headers(headers)
        .get()
    ).build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.code != 200) {
          throw SessionExpiredException()
        }
        val body = response.body?.string()
        Json.decodeFromString<ECodeQRCode>(body!!)
      }
    }
  }

  suspend fun getUserInfo(session: ECodeSession): ECodeUserinfo {
    // 获取二维码
    val url = "https://ecode.neu.edu.cn/ecode/api/user-info"

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val headers = Headers.Builder()
      .add(
        "Cookie",
        "SESSION=${session.session}; XSRF-TOKEN=${session.xsrfToken}"
      )
      .add("X-XSRF-TOKEN", session.xsrfToken)
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(url)
        .headers(headers)
        .get()
    ).build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.code != 200) {
          throw SessionExpiredException()
        }
        val body = response.body?.string()
        Json.decodeFromString<ECodeUserinfo>(body!!)
      }
    }
  }
}