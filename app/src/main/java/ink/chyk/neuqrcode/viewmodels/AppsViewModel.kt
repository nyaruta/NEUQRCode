package ink.chyk.neuqrcode.viewmodels

import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AppsViewModel(
  private val neu: NEUPass,
  private val mmkv: MMKV,
  private val campusRun: CampusRun
) : ViewModel() {

  enum class LoadingState {
    LOADING,
    SUCCESS,
    FAILED
  }

  // 加载状态：步道乐跑
  private var _campusRunState = MutableStateFlow(LoadingState.LOADING)
  val campusRunState: StateFlow<LoadingState> = _campusRunState

  // 当前学期名称
  private var _termName = MutableStateFlow("")
  val termName: StateFlow<String> = _termName

  fun initCampusRun() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        try {
          // 登录步道乐跑
          campusRun.loginCampusRun()
          val index = campusRun.getIndex()
          _termName.value = index?.term_name ?: ""
        } catch (e: Exception) {
          Log.e("AppsViewModel", "initCampusRun: ${e.message}")
          _campusRunState.value = LoadingState.FAILED
        } finally {
          _campusRunState.value = LoadingState.SUCCESS
        }
      }
    }
  }
}