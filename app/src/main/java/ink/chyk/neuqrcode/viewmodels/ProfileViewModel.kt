package ink.chyk.neuqrcode.viewmodels

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.activities.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
@JsonIgnoreUnknownKeys
data class GitHubRelease(
  @SerialName("tag_name")
  val tagName: String,
  val body: String
)

class ProfileViewModel(
  override val mmkv: MMKV,
  override val neu: NEUPass
) : BasicViewModel(mmkv, neu) {
  override val appName: String = "mobile"

  override suspend fun newAppTicket(portalTicket: String): String {
    return neu.loginMobileApiTicket(portalTicket)
  }

  override suspend fun newAppSession(): NEUAppSession {
    return neu.newMobileApiSession()
  }

  override suspend fun loginApp(session: NEUAppSession, appTicket: String) {
    return neu.loginMobileApi(session, appTicket)
  }

  private val _user = MutableStateFlow<MobileApiUserAttributes?>(null)
  val user: StateFlow<MobileApiUserAttributes?> = _user
  private val _userInfo = MutableStateFlow<MobileApiUserInfo?>(null)
  val userInfo: StateFlow<MobileApiUserInfo?> = _userInfo
  private val _campusCard = MutableStateFlow<MobileApiCampusCard?>(null)
  val campusCard: StateFlow<MobileApiCampusCard?> = _campusCard
  private val _campusNetwork = MutableStateFlow<MobileApiCampusNetwork?>(null)
  val campusNetwork: StateFlow<MobileApiCampusNetwork?> = _campusNetwork

  private suspend fun refreshUserInfo() {
    prepareSessionAnd { session ->
      _user.value = neu.getMobileApiUser(session).data.attributes
      _userInfo.value = neu.getMobileApiUserInfo(session)
      _campusCard.value = neu.getMobileApiCampusCard(session)
      _campusNetwork.value = neu.getMobileApiCampusNetwork(session)
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