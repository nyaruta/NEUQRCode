package ink.chyk.neuqrcode.viewmodels

import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.neu.*

open class PersonalViewModel(
  private val neu: NEUPass,
  private val mmkv: MMKV,
): ViewModel() {
  // 需要使用 personal API 的 ViewModel 皆可继承此类

  /**
   * 获取智慧东大 统一身份认证 的 ticket
   * 如果 mmkv 中没有记录，或者 reLogin 为 true，则重新登录。
   *
   * @param reLogin 是否强制重新登录
   *
   * @return ticket（TGT-xxxx-tpass）
   */
  suspend fun getPortalTicket(reLogin: Boolean = false): String {
    Log.d("getPortalTicket", "reLogin: $reLogin")
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!

    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = neu.loginPortalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }

    return portalTicket
  }

  fun updateSession(session: PersonalSession) {
    mmkv.encode("personal_lc", session.lc)
    mmkv.encode("personal_vl", session.vl)
    mmkv.encode("personal_sess_id", session.sessId)
  }


  suspend fun <T> prepareSessionAnd(action: suspend (PersonalSession) -> T): T {
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
        } catch (_: TicketFailedException) {
          val newTicket = getPortalTicket(true)
          personalTicket = neu.loginPersonalApiTicket(newTicket)
        }
        mmkv.encode("personal_ticket", personalTicket)
        val session = neu.loginPersonalApi(personalTicket)
        updateSession(session)
        return action(session)
      } else {
        val session = PersonalSession(lc, vl, sessId)
        try {
          return action(session)
        } catch (_: SessionExpiredException) {
          // 重新登录
          val ticket = getPortalTicket(true)
          val personal_ticket = neu.loginPersonalApiTicket(ticket)
          mmkv.encode("personal_ticket", personal_ticket)
          val newSession = neu.loginPersonalApi(personal_ticket)
          updateSession(newSession)
          return action(newSession)
        }
      }
    } catch (e: Exception) {
      // 错误处理逻辑
      e.printStackTrace()
      throw e
    }
  }
}