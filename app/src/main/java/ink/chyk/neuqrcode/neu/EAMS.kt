package ink.chyk.neuqrcode.neu

import android.util.*
import ink.chyk.neuqrcode.*
import kotlinx.coroutines.*
import okhttp3.*
import kotlin.Pair

class EAMS(
  private val neu: NEUPass
) {
  // 你说得对，但是《树维教务系统》是一款...

  private val eamsRootUrl = "http://219.216.96.4/eams"
  private val eamsHomeExtUrl = "$eamsRootUrl/homeExt.action"
  private val debugMode = true  // 是否记录请求日志

  private suspend fun newSession(): Pair<String, String> {
    // 请求一次教务系统主页，获取一个新的 jsessionid 在登录时使用
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()
    val request = Request.Builder()
      .url(eamsHomeExtUrl)
      .build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val headers = response.headers
          headers["Set-Cookie"]?.let { cookies ->
            val getCookie = fun(name: String): String? {
              return cookies.split(";").find { it.startsWith(name) }?.substringAfter("=")
                ?.substringBefore(";")
            }
            val sessionId = getCookie("JSESSIONID")
            // JSESSIONID 和 GSESSIONID 后续请求需要都带上
            // 但它们内容是一样的
            // 所以只记录一个
            val serverName = getCookie("SERVERNAME")
            // 教务系统后端有多台服务器
            // 同一个 jsessionid 只能在同一台服务器上使用
            // 所以要一并记录下服务器名
            // 后续请求都用这台服务器
            if (sessionId != null && serverName != null) {
              if (debugMode)
                Log.d("EAMS", "New session id: $sessionId, server name: $serverName")
              return@withContext Pair(sessionId, serverName)
            } else {
              throw RequestFailedException()
            }
          }
        }
        throw RequestFailedException()
      }
    }
  }

  suspend fun loginEAMSTicket(
    portalTicket: String
  ): String {
    // 登录教务系统，获取教务系统的 ticket
    val session = newSession()
    val sessionId = session.first
    val eamsTicket =
      neu.loginNEUAppTicket(portalTicket, "$eamsHomeExtUrl;jsessionid=$sessionId")
    if (debugMode)
      Log.d("EAMS", "EAMS ticket: $eamsTicket")
    return eamsTicket
  }

  private suspend fun buildEAMSRequest(
    session: Pair<String, String>,
    url: String
  ): Request.Builder {
    // 构建一个带 session 信息的 Request.Builder
    return Request.Builder()
      .url(url)
      .header(
        "Cookie",
        "JSESSIONID=${session.first}; GSESSIONID=${session.first}; SERVERNAME=${session.second}"
      )
  }

  private suspend fun loginEAMS(
    session: Pair<String, String>,
    eamsTicket: String
  ) {
    // 获取完 ticket 之后，对这个 session 进行登录
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val loginRequest =
      buildEAMSRequest(session, "$eamsHomeExtUrl;jsessionid=${session.first}?ticket=$eamsTicket")
        .get().build()
    val afterLoginRequest =
      buildEAMSRequest(session, "$eamsHomeExtUrl;jsessionid=${session.first}").build()

    withContext(Dispatchers.IO) {
      client.newCall(loginRequest).execute().use { response ->
        if (response.code == 302) {
          client.newCall(afterLoginRequest).execute().use { response2 ->
            if (response2.code != 200) {
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

  suspend fun eamsRequest(
    session: Pair<String, String>,
    url: String
  ): String {
    // 在登录完毕的情况下
    // 使用教务系统的 session 发起请求
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()
    val request = buildEAMSRequest(session, url).build()
    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful && response.code == 200)
          return@withContext response.body?.string() ?: ""
      }
      throw RequestFailedException()
    }
  }
}