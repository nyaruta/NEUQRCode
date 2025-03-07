package ink.chyk.neuqrcode.neu

import android.util.*
import ink.chyk.neuqrcode.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import okhttp3.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.Pair


class NEUPass(
  val onFailed: () -> Boolean
) {
  // 智慧东大 API

  var userAgent: String? = null
    get() {
      if (field == null) {
        field = System.getProperty("http.agent")
          ?.replace(Regex("Dalvik/\\d+(\\.\\d+)*"), "Mozilla/5.0")
          ?.replace(")", "; NEUQRCode (Simulated com.sunyt.testdemo/3.0.0))")
        Log.d("NEUPass", "User-Agent: $field")
      }
      return field
    }


  private fun useRequestedWith(request: Request.Builder): Request.Builder {
    val version = "3.0.0"
    return request
      .header("User-Agent", userAgent?: "Mozilla/5.0 (Linux; Android)")
      .header("X-Requested-With", "com.sunyt.testdemo")
      .header("X-App-Version", version)
  }

  suspend fun loginPersonalTicket(studentId: String, password: String): String {
    // 登录账号（智慧东大 3.x API）
    val url = "https://personal.neu.edu.cn/prize/Front/Oauth/User/sso"

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
          val resp = Json.decodeFromString<UserSSOLoginResponse>(body!!)
          if (resp.code == 1) {
            // 登录成功
            resp.tgt ?: throw PasswordIncorrectException()
          } else {
            throw PasswordIncorrectException()
          }
        } else {
          throw RequestFailedException(url)
        }
      }
    }
  }

  suspend fun loginNEUAppTicket(
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
          if (location != null && location.contains("ticket=")) {
            // 提取新的 ticket 参数
            location.substringAfter("ticket=")
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

  suspend fun loginPersonalApiTicket(portalTicket: String): String {
    return loginNEUAppTicket(
      portalTicket,
      "https://personal.neu.edu.cn/portal/manage/common/cas_login/1?redirect=https%3A%2F%2Fpersonal.neu.edu.cn%2Fportal"
    )
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

  suspend fun loginPersonalApi(personalApiTicket: String): PersonalSession {
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val loginRequest = useRequestedWith(
      Request.Builder()
        .url("https://personal.neu.edu.cn/portal/manage/common/cas_login/1?redirect=https://personal.neu.edu.cn/portal&ticket=$personalApiTicket")
        .get()
    ).build()

    return withContext(Dispatchers.IO) {
      client.newCall(loginRequest).execute().use { response ->
        if (response.code == 302) {
          // 登录成功，提取 cookie
          val setCookieHeaders = response.headers("Set-Cookie")
          val getCookie = { name: String ->
            setCookieHeaders
              .first({ it.startsWith(name) })
              .substringBefore(";")
              .substringAfter("=")
          }
          PersonalSession(
            getCookie("CK_LC"),
            getCookie("CK_VL"),
            getCookie("SESS_ID")
          )
        } else {
          throw TicketExpiredException()
        }
      }
    }
  }

  private suspend inline fun <reified T> basicAppRequest(
    session: NEUAppSession,
    url: String
  ): T? {
    //Log.d("NEUPass", "Request: $url")
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
      try {
        client.newCall(request).execute().use { response ->
          if (response.code != 200) {
            throw SessionExpiredException()
          }
          val body = response.body?.string()
          //Log.d("NEUPass", "Response: $body")
          Json.decodeFromString<T>(body!!)
        }
      } catch (e: Exception) {
        // 请求失败
        if (e is SessionExpiredException) {
          throw e
        }
        if (!onFailed()) {
          throw RequestFailedException(url)
        }
        return@withContext null as T
      }
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  private suspend inline fun <reified T> basicPersonalApiRequest(
    session: PersonalSession,
    url: String,
    postBody: MultipartBody? = null,
    putBody: MultipartBody? = null
  ): Pair<T?, PersonalSession> {
    // 新的 3.x api 在每次请求后都有可能更新 sess_id
    // 返回结果和新的 session
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()

    val headers = Headers.Builder()
      .add(
        "Cookie",
        "CK_LC=${session.lc}; CK_VL=${session.vl}; SESS_ID=${session.sessId}"
      )
      .apply {
        if (postBody != null) {
          add("Content-Type", "multipart/form-data; boundary=${postBody.boundary}")
        } else if (putBody != null) {
          add("Content-Type", "multipart/form-data; boundary=${putBody.boundary}")
        }
      }
      .build()

    val request = useRequestedWith(
      Request.Builder()
        .url(url)
        .headers(headers)
        .apply {
          if (postBody != null) {
            post(postBody)
          } else if (putBody != null) {
            put(putBody)
          } else {
            get()
          }
        }
    ).build()

    return withContext(Dispatchers.IO) {
      try {
        client.newCall(request).execute().use { response ->
          if (response.code != 200) {
            Log.d("NEUPass", "Error: ${response.body?.string()}")
            throw SessionExpiredException()
          }
          val body = response.body?.string()
          val result: PersonalResponse<T>
          try {
            result = Json.decodeFromString<PersonalResponse<T>>(body!!)
          } catch (e: MissingFieldException) {
            throw SessionExpiredException()
          }
          if (result.e > 10000) {
            Log.d("NEUPass", "Error: ${result.e}, ${result.m}")
            throw SessionExpiredException()
          }
          // 更新 sess_id
          val newSessId = response.headers("Set-Cookie")
            .firstOrNull { it.startsWith("SESS_ID") }

          if (newSessId == null) {
            // 没更新
            Pair(result.d, session)
          } else {
            // 更新了
            Pair(
              result.d,
              PersonalSession(
                session.lc,
                session.vl,
                newSessId.substringBefore(";").substringAfter("=")
              )
            )
          }
        }
      } catch (e: Exception) {
        if (e is SessionExpiredException) {
          throw e
        } else {
          Log.d("NEUPass", "Error: ${e.message}")
          e.printStackTrace()
          // 请求失败
          if (!onFailed()) {
            throw RequestFailedException(url)
          }
          // 返回空数据 即这个T的默认值
          return@withContext Pair(null as T, session)
        }
      }
    }
  }

  suspend fun getQRCode(session: NEUAppSession): ListedResponse<ECodeResponse>? {
    return basicAppRequest(session, "https://ecode.neu.edu.cn/ecode/api/qr-code")
  }

  suspend fun getECodeUserInfo(session: NEUAppSession): ListedResponse<ECodeUserInfoResponse>? {
    return basicAppRequest(session, "https://ecode.neu.edu.cn/ecode/api/user-info")
  }

  suspend fun getUserInfo(session: PersonalSession): Pair<UserInfoOuter?, PersonalSession> {
    return basicPersonalApiRequest(
      session,
      "https://personal.neu.edu.cn/portal/personal/frontend/data/info"
    )
  }

  suspend fun getPersonalDataIds(session: PersonalSession): Pair<PersonalDataIdOuter?, PersonalSession> {
    return basicPersonalApiRequest(
      session,
      "https://personal.neu.edu.cn/portal/personal/frontend/data/items?type=personal_data"
    )
  }

  suspend fun getMailList(
    session: PersonalSession,
  ): Pair<MailListResponse?, PersonalSession> {
    return basicPersonalApiRequest(
      session,
      "https://personal.neu.edu.cn/portal/neu/frontend/mail/list"
    )
  }

  suspend fun getPersonalDataItem(
    session: PersonalSession,
    ids: PersonalDataIdOuter,
    key: String
  ): Pair<PersonalDataItemOuter?, PersonalSession> {
    val id = ids.data.first { it.key == key }.id
    return basicPersonalApiRequest(
      session,
      "https://personal.neu.edu.cn/portal/personal/frontend/data/detail?id=$id"
    )
  }

  suspend fun uploadImage(
    session: PersonalSession,
    image: ByteArray,
    mimeType: String,
    fileName: String
  ): Pair<UploadImageResponse?, PersonalSession> {
    val requestBody = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart("category", "image")
      .addFormDataPart(
        "upfile",
        fileName,
        image.toRequestBody(mimeType.toMediaTypeOrNull())
      )
      .build()
    return basicPersonalApiRequest(
      session,
      "https://personal.neu.edu.cn/portal/frontend/upload/image",
      postBody = requestBody
    )
  }

  suspend fun updateAvatar(
    session: PersonalSession,
    imageUrl: String
  ): Pair<List<String>?, PersonalSession> {
    val requestBody = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart("avatar", imageUrl)
      .build()
    return basicPersonalApiRequest(
      session,
      "https://personal.neu.edu.cn/portal/frontend/user/update-avatar",
      putBody = requestBody
    )
  }

  suspend fun getCoreMailUrl(
    session: PersonalSession,
    redirector: String
  ): Pair<String, PersonalSession> {
    val client = OkHttpClient.Builder()
      .followRedirects(false)
      .build()
    val headers = Headers.Builder()
      .add(
        "Cookie",
        "CK_LC=${session.lc}; CK_VL=${session.vl}; SESS_ID=${session.sessId}"
      )
      .build()
    val request = useRequestedWith(
      Request.Builder()
        .url(redirector)
        .headers(headers)
        .get()
    ).build()
    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.code != 302) {
          throw SessionExpiredException()
        }
        val coreMailUrl = response.header("Location")!!
        return@withContext Pair(coreMailUrl, session)
      }
    }
  }
}