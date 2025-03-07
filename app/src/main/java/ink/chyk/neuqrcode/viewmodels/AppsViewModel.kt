package ink.chyk.neuqrcode.viewmodels

import android.content.*
import android.util.*
import androidx.compose.ui.graphics.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.activities.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlin.Pair

@ExperimentalSerializationApi
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
  private var _beforeRun = MutableStateFlow<BeforeRunResponse?>(null)
  val beforeRun: StateFlow<BeforeRunResponse?> = _beforeRun
  private var _termRunRecord = MutableStateFlow<GetTermRunRecordResponse?>(null)
  val termRunRecord: StateFlow<GetTermRunRecordResponse?> = _termRunRecord

  private val t1 = arrayOf(
    R.string.run_t1_1,
    R.string.run_t1_2,
    R.string.run_t1_3,
  )
  private val t2 = arrayOf(
    R.string.run_t2_1,
    R.string.run_t2_2,
    R.string.run_t2_3,
  )
  private val t3 = arrayOf(
    R.string.run_t3_1,
    R.string.run_t3_2,
    R.string.run_t3_3,
  )
  private val t4 = arrayOf(
    R.string.run_t4_1,
    R.string.run_t4_2,
    R.string.run_t4_3,
  )
  private val t5 = arrayOf(
    R.string.run_t5_1,
    R.string.run_t5_2,
    R.string.run_t5_3,
    R.string.run_t5_4,
    R.string.run_t5_5
  )

  fun initCampusRun() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        try {
          // 登录步道乐跑
          campusRun.loginCampusRun()
          val index = campusRun.getIndex()
          _termName.value = index?.term_name ?: "服务器返回错误"
          _beforeRun.value = campusRun.getBeforeRun()
          _termRunRecord.value = campusRun.getTermRunRecord()
          _campusRunState.value = LoadingState.SUCCESS
        } catch (e: Exception) {
          Log.e("AppsViewModel", "initCampusRun: ${e.message}")
          _campusRunState.value = LoadingState.FAILED
        }
      }
    }
  }

  fun startCampusRun(context: Context) {
    // 进入乐跑

    val intent = Intent(context, CampusRunningActivity::class.java)
    intent.putExtra("url", campusRun.toStartRunUrl())
    context.startActivity(intent)
  }

  fun distanceColorAndTextPair(
    distance: String?,
  ): Pair<Color, Int> {
    // 返回对应的颜色和随机抽取的文字
    val distanceDouble = distance?.toDoubleOrNull()
    return when {
      distanceDouble == null -> Color(255, 0, 0) to t1.random()
      distanceDouble < 20 -> Color(255, 0, 0) to t1.random()
      distanceDouble < 40 -> Color(255, 165, 0) to t2.random()
      distanceDouble < 80 -> Color(0, 165, 255) to t3.random()
      distanceDouble < 96 -> Color(0, 255, 165) to t4.random()
      else -> Color(20, 255, 20) to t5.random()
    }
  }
}