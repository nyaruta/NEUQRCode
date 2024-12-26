package ink.chyk.neuqrcode

import kotlinx.coroutines.*
import okhttp3.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class NEUAppSession(
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

@Serializable
data class MobileApiUser(
  val data: MobileApiUserData
)

@Serializable
data class MobileApiUserData(
  val id: String,  // 请求 id
  val type: String,
  val attributes: MobileApiUserAttributes
)

@Serializable
data class MobileApiUserAttributes(
  val code: String,  // 学号
  val name: String,  // 姓名
  val avatar: String, // 大头，base64 编码的 datauri
  val remark: String? = null,  //
  val credits: String,
  val createTime: String,
  val updateTime: String,
  val isDeleted: Int
)

@Serializable
data class MobileApiUserInfo(
  val DH: String,  // 学号
  val XM: String,  // 姓名
  val RYLX: String,  // 人员类型
  val ZZJG: String,  // 组织机构
  val ZZJGBH: String,  // 组织机构编号
  val XB: String,  // 性别
  val EMAIL: String,  // 邮箱地址 neu.edu.cn 结尾
)

@Serializable
data class MobileApiCampusCard(
  val createUser: Int,
  val createTime: String,
  val updateUser: Int,
  val updateTime: String,
  val status: Int,
  val isDeleted: Int,
  val balance: String,
  val supply: String,
  val jumpUrl: String
)

@Serializable
data class MobileApiCampusNetwork(
  val createUser: Int,
  val createTime: String,
  val updateUser: Int,
  val updateTime: String,
  val status: Int,
  val isDeleted: Int,
  val balance: String,
  val usedData: String,
  val jumpUrl: String
)


class NEUPass {
  // 智慧东大 API

  private fun useRequestedWith(request: Request.Builder): Request.Builder {
    return request
      .header("X-Requested-With", "com.sunyt.testdemo")
      .header("X-App-Version", R.string.simulated_sunyt_version.toString())
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

  private suspend fun loginNEUAppTicket(
    portalTicket: String,
    callbackUrl: String
  ): String {
    // 使用 CASTGC（总 ticket）拿到用于登录任意小程序的 ticket
    val url =
      "https://pass.neu.edu.cn/tpass/login?service=$callbackUrl"

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

  suspend fun loginECodeTicket(portalTicket: String): String {
    return loginNEUAppTicket(portalTicket, "https://ecode.neu.edu.cn/ecode/api/sso/login")
  }

  suspend fun loginMobileApiTicket(portalTicket: String): String {
    return loginNEUAppTicket(portalTicket, "https://portal.neu.edu.cn/mobile/api/sso/login")
  }

  private suspend fun newNEUAppSession(callbackUrl: String): NEUAppSession {
    // 获取一个新的 session

    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(callbackUrl)
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
        NEUAppSession(session, xsrfToken, System.currentTimeMillis() / 1000 + 86400 * 99)
        // 99 天后过期
      }
    }
  }

  suspend fun newECodeSession(): NEUAppSession {
    return newNEUAppSession("https://ecode.neu.edu.cn/ecode/api/sso/login")
  }

  suspend fun newMobileApiSession(): NEUAppSession {
    return newNEUAppSession("https://portal.neu.edu.cn/mobile/api/sso/login")
  }

  private suspend fun loginNEUApp(
    session: NEUAppSession,
    appTicket: String,
    loginUrl: String,
    redirectUrl: String
  ) {
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val headers = Headers.Builder()
      .add(
        "Cookie",
        "SESSION=${session.session}; XSRF-TOKEN=${session.xsrfToken}; KC_REDIRECT=$redirectUrl"
      )
      .add("X-XSRF-TOKEN", session.xsrfToken)
      .build()

    val loginRequest = useRequestedWith(
      Request.Builder()
        .url("$loginUrl?ticket=$appTicket")
        .headers(headers)
        .get()
    ).build()

    val afterLoginRequest = useRequestedWith(
      Request.Builder()
        .url(loginUrl)
        .headers(headers)
        .get()
    ).build()

    withContext(Dispatchers.IO) {
      client.newCall(loginRequest).execute().use { response ->
        if (response.code == 302) {
          client.newCall(afterLoginRequest).execute().use { response2 ->
            if (!(response2.code == 302 && response2.header("Location") == redirectUrl)) {
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

  suspend fun loginECode(session: NEUAppSession, eCodeTicket: String) {
    return loginNEUApp(
      session = session,
      appTicket = eCodeTicket,
      loginUrl = "https://ecode.neu.edu.cn/ecode/api/sso/login",
      redirectUrl = "https://ecode.neu.edu.cn/ecode/#/"
    )
  }

  suspend fun loginMobileApi(session: NEUAppSession, mobileApiTicket: String) {
    return loginNEUApp(
      session = session,
      appTicket = mobileApiTicket,
      loginUrl = "https://portal.neu.edu.cn/mobile/api/sso/login",
      redirectUrl = "https://portal.neu.edu.cn/mobile/api"
    )
  }

  private suspend inline fun <reified T> basicAppRequest(
    session: NEUAppSession,
    url: String
  ): T {
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
        Json.decodeFromString<T>(body!!)
      }
    }
  }

  suspend fun getQRCode(session:NEUAppSession): ECodeQRCode {
    return basicAppRequest(session, "https://ecode.neu.edu.cn/ecode/api/qr-code")
  }

  suspend fun getECodeUserInfo(session: NEUAppSession): ECodeUserinfo {
    return basicAppRequest(session, "https://ecode.neu.edu.cn/ecode/api/user-info")
  }

  suspend fun getMobileApiUser(session: NEUAppSession): MobileApiUser {
    return basicAppRequest(session, "https://portal.neu.edu.cn/mobile/api/user")
  }

  suspend fun getMobileApiUserInfo(session: NEUAppSession): MobileApiUserInfo {
    return basicAppRequest(session, "https://portal.neu.edu.cn/mobile/api/user/info")
  }

  suspend fun getMobileApiCampusCard(session: NEUAppSession): MobileApiCampusCard {
    return basicAppRequest(session, "https://portal.neu.edu.cn/mobile/api/personal/campusInfo/card")
  }

  suspend fun getMobileApiCampusNetwork(session: NEUAppSession): MobileApiCampusNetwork {
    return basicAppRequest(session, "https://portal.neu.edu.cn/mobile/api/personal/campusInfo/network")
  }

}