package ink.chyk.neuqrcode.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.fleeksoft.ksoup.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class EAMSViewModel(
  private val mmkv: MMKV,
  private val neu: NEUPass
) : ViewModel() {
  // 由于教务系统的土豆性，所以不在本地存储 session，而是每次打开界面都重新登录

  // 请求失败直接报错跳脸「仅内网访问」
  private val _isRequestFailed = MutableStateFlow(false)
  val isRequestFailed : StateFlow<Boolean> = _isRequestFailed

  // 是否正在登录？
  private val _isInitializing = MutableStateFlow(true)
  val isInitializing : StateFlow<Boolean> = _isInitializing

  // 会话
  private var session: Pair<String, String>? = null

  // 左侧菜单
  // 菜单名 -> 菜单 id
  private val _leftMenu = MutableStateFlow<List<Pair<String, String>>>(emptyList())
  val leftMenu: StateFlow<List<Pair<String, String>>> = _leftMenu

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

  init {
    viewModelScope.launch {
      // 每次启动都重新走一遍登录流程，因为教务系统的服务器过于土豆
      val portalTicket = getPortalTicket()
      try {
        session = neu.eams.newSession()
        val eamsTicket = neu.eams.loginEAMSTicket(session!!, portalTicket)
        neu.eams.loginEAMS(session!!, eamsTicket)
        getLeftMenu()
        _isInitializing.value = false
      } catch (e: RequestFailedException) {
        _isRequestFailed.value = true
      }
    }
  }

  private suspend fun getLeftMenu() {
    // 获取左侧菜单
    val leftMenu = neu.eams.getLeftMenuHtml(session!!)
    val doc = Ksoup.parse(leftMenu)
    val menuItems = doc.select("li.expand ul.acitem li a")
    _leftMenu.value = menuItems.map {
      Pair(it.text().trim(), it.attr("href"))
      // 菜单名 -> 菜单 id
      // 公共课表查询 -> /eams/studentPublicScheduleQuery.action
    }
  }
}