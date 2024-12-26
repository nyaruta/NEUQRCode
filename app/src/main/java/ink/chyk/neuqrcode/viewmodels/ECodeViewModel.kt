package ink.chyk.neuqrcode.viewmodels

import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ECodeViewModel(
  private val mmkv: MMKV,
  private val neu: NEUPass
) : ViewModel() {
  // 一些参数，Kotlin 状态流机制

  // ChatGPT: 带下划线的是私有变量，Mutable 意为可以在这个 ViewModel 内部修改
  // 而不带下划线的是公开的，并且它是 StateFlow，只读，只能接收不能修改

  // 是否加载完毕，显示二维码？
  private val _showCode = MutableStateFlow(false)
  val showCode: StateFlow<Boolean> = _showCode

  // 二维码数据
  private val _code = MutableStateFlow("")
  val code: StateFlow<String> = _code

  // 用户信息
  private val _userInfo = MutableStateFlow<ECodeUserinfoAttributes?>(null)
  val userInfo: StateFlow<ECodeUserinfoAttributes?> = _userInfo

  // 二维码生成时间
  private val _codeGenerateTime = MutableStateFlow(0L)
  val codeGenerateTime: StateFlow<Long> = _codeGenerateTime

  // 二维码过期时间
  private val _codeExpiredAt = MutableStateFlow(0L)
  val codeExpiredAt: StateFlow<Long> = _codeExpiredAt


  /**
   * 检查 mmkv 中是否有 student_id 记录判断用户是否已登录。
   *
   * @return 如果用户已登录，返回 `true`；如果未登录，返回 `false`。
   */
  fun checkIsLogin(): Boolean {
    return mmkv.containsKey("student_id")
  }

  /**
   * 获取智慧东大 统一身份认证 的 ticket
   * 如果 mmkv 中没有记录，或者 reLogin 为 true，则重新登录。
   *
   * @param reLogin 是否强制重新登录
   *
   * @return ticket（TGT-xxxx-tpass）
   */
  private suspend fun getPortalTicket(reLogin: Boolean = false): String {
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!

    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = neu.loginPortalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }

    return portalTicket
  }

  /**
   * 通过 portal ticket 获取一码通 portal ticket
   * 如果 mmkv 中没有记录，则重新登录。
   *
   * @return ticket
   */
  private suspend fun getECodeTicket(): String {
    var eCodeTicket: String? = mmkv.decodeString("ecode_ticket")
    if (eCodeTicket == null) {
      var portalTicket = getPortalTicket()
      try {
        // 假设 portalTicket 没过期
        eCodeTicket = neu.loginECodeTicket(portalTicket)
      } catch (e: TicketFailedException) {
        // 重新登录
        portalTicket = getPortalTicket(true)
        eCodeTicket = neu.loginECodeTicket(portalTicket)
      }
      mmkv.encode("ecode_ticket", eCodeTicket)
    }
    return eCodeTicket!! // 不可能为空!!
  }

  /**
   * 获取或新建一码通 Session
   * 如果 mmkv 中没有记录，则新建空 Session。
   *
   * @return Session
   */
  private suspend fun getECodeSession(neu: NEUPass, mmkv: MMKV): ECodeSession {
    var eCodeSession: String? = mmkv.decodeString("ecode_session")
    var xsrfToken: String? = mmkv.decodeString("xsrf_token")
    var expiredAt: Long? = mmkv.decodeLong("expired_at")  // 以秒为单位

    val current = System.currentTimeMillis() / 1000
    if (eCodeSession == null || xsrfToken == null || expiredAt == null || expiredAt < current) {
      Log.d("ECode", "Session 过期")
      val session = neu.newECodeSession()
      eCodeSession = session.session
      xsrfToken = session.xsrfToken
      expiredAt = session.expiredAt
      mmkv.encode("ecode_session", eCodeSession)
      mmkv.encode("xsrf_token", xsrfToken)
      mmkv.encode("expired_at", expiredAt)
    }

    // 不可能为空!!
    return ECodeSession(eCodeSession, xsrfToken, expiredAt)
  }


  /**
   * 获取二维码
   */
  fun refreshECode() {
    viewModelScope.launch {
      while (true) {
        // 计时器
        val currentTime: Long = System.currentTimeMillis()
        // Log.d("ECode", "Current Time: $currentTime, Expired At: $codeExpiredAt")
        if (currentTime >= _codeExpiredAt.value) {
          try {
            // 获取新的二维码逻辑
            val neu = NEUPass()
            val eCodeSession = getECodeSession(neu, mmkv)
            var eCode: ECodeQRCode? = null
            try {
              eCode = neu.getQRCode(eCodeSession)
              if (_userInfo.value == null) {
                _userInfo.value = neu.getUserInfo(eCodeSession).data[0].attributes
              }
            } catch (e: SessionExpiredException) {
              // session 过期
              val session = neu.newECodeSession()
              mmkv.encode("ecode_session", session.session)
              mmkv.encode("xsrf_token", session.xsrfToken)
              mmkv.encode("expired_at", session.expiredAt)

              // 重新登录
              val eCodeTicket = getECodeTicket()
              try {
                neu.loginECode(session, eCodeTicket)
              } catch (e: TicketExpiredException) {
                // 重新获取 ticket
                val portalTicket = getPortalTicket(true)
                val newECodeTicket = neu.loginECodeTicket(portalTicket)
                mmkv.encode("ecode_ticket", newECodeTicket)
                neu.loginECode(session, newECodeTicket)
              }
              eCode = neu.getQRCode(session)
              if (_userInfo.value == null) {
                _userInfo.value = neu.getUserInfo(eCodeSession).data[0].attributes
              }
            }

            _showCode.value = true
            _code.value = eCode!!.data[0].attributes.qrCode
            _codeGenerateTime.value = eCode.data[0].attributes.createTime
            _codeExpiredAt.value = eCode.data[0].attributes.qrInvalidTime
          } catch (e: Exception) {
            // 错误处理逻辑
            e.printStackTrace()
          }
        }

        // 每秒检查一次
        delay(1000L)
      }
    }
  }

  init {
    refreshECode()
  }
}