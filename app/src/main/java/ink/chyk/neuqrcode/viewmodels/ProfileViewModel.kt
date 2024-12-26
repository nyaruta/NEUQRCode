package ink.chyk.neuqrcode.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ProfileViewModel(
  override val mmkv: MMKV,
  override val neu: NEUPass
): BasicViewModel(mmkv, neu) {
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

  init {
    viewModelScope.launch {
      refreshUserInfo()
    }
  }
}