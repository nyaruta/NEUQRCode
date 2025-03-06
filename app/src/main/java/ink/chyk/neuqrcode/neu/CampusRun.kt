package ink.chyk.neuqrcode.neu

import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import okhttp3.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.util.*


class CampusRun(
  private val neu: NEUPass,
  private val mmkv: MMKV
) {
  // 步道乐跑 API
  private val apiHost = "https://tybzhtypt.neu.edu.cn/v3/api.php"

  private val api = object {
    // API 端点
    val index = "/WpIndex/Index"
    val beforeRun = "/Run2/beforeRunV260"
    val getTimestamp = "/Run/getTimestampV278"
    val getTermList = "/WpRun/getTermList"
    val getTermRunRecord = "/WpRun/getTermRunRecord"
  }

  // 统一身份认证 callback URL
  private val callbackUrl = "https://tybzhtypt.neu.edu.cn/bdlp_h5_fitness_test/public/index.php/index/login/neuLogin"

  private val encryption = object {
    // 加密算法
    private val MD5_SALT = "rDJiNB9j7vD2"
    private val AES_KEY = "Wet2C8d34f62ndi3".toByteArray(Charsets.UTF_8)
    private val AES_IV = "K6iv85jBD8jgf32D".toByteArray(Charsets.UTF_8)

    fun signMD5(obj: Map<String, String>): String {
      val textToHash = obj.keys.sorted().joinToString("") { key ->
        key + obj[key]
      }
      val md5 = MessageDigest.getInstance("MD5")
      md5.update((textToHash + MD5_SALT).toByteArray(Charsets.UTF_8))
      return md5.digest().joinToString("") { "%02x".format(it) }
    }

    fun aesDecrypt(data: String): String {
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      val keySpec = SecretKeySpec(AES_KEY, "AES")
      val ivSpec = IvParameterSpec(AES_IV)
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
      val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(data))
      return String(decryptedBytes, Charsets.UTF_8)
    }

    fun aesEncrypt(data: String): String {
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      val keySpec = SecretKeySpec(AES_KEY, "AES")
      val ivSpec = IvParameterSpec(AES_IV)
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
      val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
      return Base64.getEncoder().encodeToString(encryptedBytes)
    }
  }

  // 步道乐跑 Token
  private var token: String? = null

  // copied from BasicViewModel
  private suspend fun getPortalTicket(reLogin: Boolean = false): String {
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!
    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = neu.loginPersonalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }
    return portalTicket
  }

  /**
   * 执行请求并处理通用逻辑
   */
  private suspend fun executeRequest(
    client: OkHttpClient,
    request: Request,
    errorMessage: String
  ): Response {
    return client.newCall(request).execute().also { response ->
      if (response.code !in 200..399) {
        throw RequestFailedException("$errorMessage: ${response.code}")
      }
    }
  }

  suspend fun loginCampusRun() {
    // 登录步道乐跑

    val portalTicket = getPortalTicket(true)
    val campusRunTicket = neu.loginNEUAppTicket(portalTicket, callbackUrl)

    val client = OkHttpClient()

    val req1 = Request.Builder()
      .url(callbackUrl)
      .header("Cookie", "Path=/; PHPSESSID=$campusRunTicket")
      .build()

    val res1 = executeRequest(client, req1, "登录步道乐跑失败")

    if (res1.code != 302) {
      throw RequestFailedException("登录步道乐跑失败: ${res1.code}")
    }

    val location = res1.header("Location")?: throw RequestFailedException("登录步道乐跑失败: 无法获取 Location")
    val query = location.split("?")[1].split("&").associate {
      val (key, value) = it.split("=")
      key to value
    }
    token = query["token"]?: throw RequestFailedException("登录步道乐跑失败: 无法获取 Token")
  }
}