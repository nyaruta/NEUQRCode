package ink.chyk.neuqrcode.viewmodels

import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.flow.*

enum class Category {
  UNREAD, NOTICE, TASKS, NEU1, NEU2, NEU3
}

class NewsViewModel(
  override val mmkv: MMKV,
  override val neu: NEUPass
) : BasicViewModel(mmkv, neu) {
  override val appName: String = "mobile"  // 同样使用 mobile，和 profile 页面共享登录状态
  override suspend fun newAppTicket(portalTicket: String): String {
    return neu.loginMobileApiTicket(portalTicket)
  }
  override suspend fun newAppSession(): NEUAppSession {
    return neu.newMobileApiSession()
  }
  override suspend fun loginApp(session: NEUAppSession, appTicket: String) {
    return neu.loginMobileApi(session, appTicket)
  }

  private var _title = MutableStateFlow("智慧东大消息中心")
  val title: StateFlow<String> = _title

  private var _rail = MutableStateFlow(false)
  val rail: StateFlow<Boolean> = _rail

  private var _category = MutableStateFlow(Category.UNREAD)
  val category: StateFlow<Category> = _category

  private var _messageSources = MutableStateFlow(emptyList<MessageSource>())
  private var _messages = MutableStateFlow(emptyList<Mess>())

  fun toggleRail() {
    _rail.value = !_rail.value
  }

  fun setCategory(category: Category) {
    _category.value = category
  }
}