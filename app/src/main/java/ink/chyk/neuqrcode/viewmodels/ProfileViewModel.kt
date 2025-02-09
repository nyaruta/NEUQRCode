package ink.chyk.neuqrcode.viewmodels

import android.content.Context
import android.content.Intent
import android.util.*
import android.widget.Toast
import androidx.lifecycle.*
import coil3.network.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import ink.chyk.neuqrcode.activities.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.Pair

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
data class GitHubRelease(
  @SerialName("tag_name")
  val tagName: String,
  val body: String
)

class ProfileViewModel(
  val mmkv: MMKV,
  val neu: NEUPass
) : ViewModel() {
  private val _userInfo = MutableStateFlow<UserInfo?>(null)
  val userInfo: StateFlow<UserInfo?> = _userInfo
  private val _mailUnread = MutableStateFlow<PersonalDataItem?>(null)
  val mailUnread: StateFlow<PersonalDataItem?> = _mailUnread
  private val _cardBalance = MutableStateFlow<PersonalDataItem?>(null)
  val cardBalance: StateFlow<PersonalDataItem?> = _cardBalance
  private val _netBalance = MutableStateFlow<PersonalDataItem?>(null)
  val netBalance: StateFlow<PersonalDataItem?> = _netBalance
  private val _loadComplete = MutableStateFlow(false)
  private val _headers = MutableStateFlow<NetworkHeaders?>(null)
  val headers: StateFlow<NetworkHeaders?> = _headers
  val loadComplete: StateFlow<Boolean> = _loadComplete


  /**
   * 获取智慧东大 统一身份认证 的 ticket
   * 如果 mmkv 中没有记录，或者 reLogin 为 true，则重新登录。
   *
   * @param reLogin 是否强制重新登录
   *
   * @return ticket（TGT-xxxx-tpass）
   */
  private suspend fun getPortalTicket(reLogin: Boolean = false): String {
    Log.d("getPortalTicket", "reLogin: $reLogin")
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!

    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = neu.loginPersonalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }

    return portalTicket
  }

  private fun updateSession(session: PersonalSession) {
    mmkv.encode("personal_lc", session.lc)
    mmkv.encode("personal_vl", session.vl)
    mmkv.encode("personal_sess_id", session.sessId)
  }

  suspend fun <T> prepareSessionAnd(action: suspend (PersonalSession) -> T) {
    try {
      val lc = mmkv.decodeString("personal_lc")
      val vl = mmkv.decodeString("personal_vl")
      val sessId = mmkv.decodeString("personal_sess_id")
      if (lc == null || vl == null || sessId == null) {
        // 登录
        Log.d("prepareSessionAnd", "No session found, logging in")
        val ticket = getPortalTicket()
        var personalTicket: String
        try {
          personalTicket = neu.loginPersonalApiTicket(ticket)
        } catch (e: TicketFailedException) {
          val newTicket = getPortalTicket(true)
          personalTicket = neu.loginPersonalApiTicket(newTicket)
        }
        mmkv.encode("personal_ticket", personalTicket)
        val session = neu.loginPersonalApi(personalTicket)
        updateSession(session)
        action(session)
      } else {
        val session = PersonalSession(lc, vl, sessId)
        try {
          action(session)
        } catch (e: SessionExpiredException) {
          // 重新登录
          val ticket = getPortalTicket(true)
          val personal_ticket = neu.loginPersonalApiTicket(ticket)
          mmkv.encode("personal_ticket", personal_ticket)
          val newSession = neu.loginPersonalApi(personal_ticket)
          updateSession(newSession)
          action(newSession)
        }
      }
    } catch (e: Exception) {
      // 错误处理逻辑
      e.printStackTrace()
    }
  }

  private suspend fun refreshUserInfo() {
    prepareSessionAnd { session ->
      try {
        val userInfoResponse = neu.getUserInfo(session)
        _userInfo.value = userInfoResponse.first.info
        updateSession(userInfoResponse.second)

        val idsResponse = neu.getPersonalDataIds(session)
        val ids = idsResponse.first
        updateSession(idsResponse.second)

        val mailUnreadResponse = neu.getPersonalDataItem(session, ids, "mail.coremailStudent")
        _mailUnread.value = mailUnreadResponse.first.data
        updateSession(mailUnreadResponse.second)

        val cardBalanceResponse = neu.getPersonalDataItem(session, ids, "card.balance")
        _cardBalance.value = cardBalanceResponse.first.data
        updateSession(cardBalanceResponse.second)

        val netBalanceResponse = neu.getPersonalDataItem(session, ids, "net.balance")
        _netBalance.value = netBalanceResponse.first.data
        updateSession(netBalanceResponse.second)

        _headers.value = NetworkHeaders.Builder()
          .add("Cookie", "SESS_ID=${session.sessId}; CK_LC=${session.lc}; CK_VL=${session.vl}")
          .add("Referer", "https://personal.neu.edu.cn/portal/")
          .build()

        _loadComplete.value = true
      } catch (e: SessionExpiredException) {
        // 重新登录
        mmkv.remove("personal_lc")
        mmkv.remove("personal_vl")
        mmkv.remove("personal_sess_id")
        refreshUserInfo()
      }
    }
  }

  fun logout(context: Context) {
    mmkv.clearAll()
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
  }

  fun openLink(context: Context, c: Int) {
    val browserIntent = Intent(
      Intent.ACTION_VIEW,
      android.net.Uri.parse(context.getString(c))
    )
    context.startActivity(browserIntent)
  }

  fun checkUpdate(context: Context) {
    viewModelScope.launch {
      _checkUpdate(context)
    }
  }

  private suspend fun _checkUpdate(context: Context) {
    suspend fun toast(msg: Int? = null, msgStr: String? = null) {
      withContext(Dispatchers.Main) {
        Toast.makeText(
          context,
          msgStr ?: context.getString(msg!!), Toast.LENGTH_SHORT
        ).show()
      }
    }

    toast(R.string.checking_update)

    val packageInfo =
      context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName

    val client = OkHttpClient.Builder()
      .build()

    val request = Request.Builder()
      .url("https://r.chyk.ink/https://api.github.com/repos/chiyuki0325/NEUQRCode/releases/latest")
      .header("User-Agent", "NEUQRCode/$versionName")
      .build()

    withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string()
          val responseJson = Json.decodeFromString<GitHubRelease>(body!!)
          val tagName = responseJson.tagName.removePrefix("v")
          if (tagName != versionName) {
            val sb = StringBuilder()
            sb.append(context.getString(R.string.found_latest_release, tagName))
              .append("\n")
              .append(responseJson.body)
            toast(msgStr = sb.toString())
            openLink(context, R.string.releases_url)
          } else {
            toast(R.string.already_latest_release)
          }
        } else {
          toast(R.string.update_check_failed)
        }
      }
    }
  }

  init {
    viewModelScope.launch {
      refreshUserInfo()
    }
  }
}