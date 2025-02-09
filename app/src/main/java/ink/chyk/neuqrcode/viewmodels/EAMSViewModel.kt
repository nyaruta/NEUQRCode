package ink.chyk.neuqrcode.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.flow.*

class EAMSViewModel(
  private val mmkv: MMKV,
  private val neu: NEUPass
) : ViewModel() {
  // 由于教务系统的土豆性，所以不在本地存储 session，而是每次打开界面都重新登录

  // 请求失败直接报错跳脸「仅内网访问」
  private val _isRequestFailed = MutableStateFlow(false)
  val isRequestFailed : StateFlow<Boolean> = _isRequestFailed
}