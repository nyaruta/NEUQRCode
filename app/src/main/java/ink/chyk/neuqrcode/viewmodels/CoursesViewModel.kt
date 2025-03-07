package ink.chyk.neuqrcode.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.*
import java.time.format.*
import java.util.Locale
import kotlin.Pair


class CoursesViewModel(
  private val mmkv: MMKV
) : ViewModel() {
  private var _date: MutableStateFlow<String> = MutableStateFlow(
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  )
  val date: StateFlow<String> = _date

  private var _quote = MutableStateFlow<HitokotoQuote?>(null)
  val quote: StateFlow<HitokotoQuote?> = _quote

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  private val today get() = LocalDate.now().format(formatter)

  fun isCourseImported(): Boolean {
    return mmkv.containsKey("course_keys")
  }

  fun getTodayCourses(date: String): List<Course> {
    return mmkv.decodeString("course_${date}")?.let {
      Json.decodeFromString(it)
    } ?: emptyList()
  }

  fun getWeekday(): String {
    val localDate = LocalDate.parse(_date.value, formatter)
    return localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  fun thisWeek(): Int {
    // 计算当前周数
    val currentDate = LocalDate.parse(_date.value, formatter)
    val termStartString = mmkv.decodeString("term_start") ?: return -1
    val termStart = LocalDate.parse(termStartString, formatter)
    return (currentDate.toEpochDay() - termStart.toEpochDay()).toInt() / 7 + 1
  }

  fun thisWeekDates(): List<Pair<Pair<LocalDate, String>, Int>> {
    // 计算本周日期
    // 日期对象，日期字符串，课程数量
    val currentDate = LocalDate.parse(_date.value, formatter)
    val dayOfWeek = currentDate.dayOfWeek.value.toLong().let { if (it == 7L) 0L else it }
    val weekStart = currentDate.minusDays(dayOfWeek)
    return (0..6).map {
      val date = weekStart.plusDays(it.toLong())
      val dateId = date.format(formatter)
      val courseCount = getTodayCourses(dateId).size
      date to dateId to courseCount
    }
  }

  private suspend fun initQuote() {
    try {
      _quote.value = Hitokoto().getQuote()
    } catch (e: Exception) {
      _quote.value = HitokotoQuote(
        id = 0,
        uuid = "",
        hitokoto = "网络错误",
        type = "",
        from = "",
        fromWho = "",
        creator = "",
        creatorUid = 0,
        reviewer = 0,
        commitFrom = "",
        createdAt = 0,
        length = 0
      )
    }
  }

  init {
    viewModelScope.launch {
      initQuote()
    }
  }

  fun setDate(date: String) {
    _date.value = date
  }

  fun isDateInTerm(date: String): Boolean {
    val termStart = mmkv.decodeString("term_start") ?: return false
    return termStart.toInt() <= date.toInt()
  }

  fun prevWeek() {
    val newDate = LocalDate.parse(_date.value, formatter).minusWeeks(1).format(formatter)
    // 边界检查
    if (!isDateInTerm(newDate)) {
      return
    }
    _date.value = newDate
  }

  fun nextWeek() {
    _date.value = LocalDate.parse(_date.value, formatter).plusWeeks(1).format(formatter)
  }

  fun backToday() {
    _date.value = today?: LocalDate.now().format(formatter)
  }

  fun isToday(): Boolean {
    return _date.value == today
  }
}