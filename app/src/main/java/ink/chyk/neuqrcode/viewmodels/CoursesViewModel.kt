package ink.chyk.neuqrcode.viewmodels

import androidx.core.graphics.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.fortuna.ical4j.data.*
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.*
import net.fortuna.ical4j.model.property.*
import java.time.*
import java.time.format.*
import java.util.*


class CoursesViewModel(
  private val mmkv: MMKV
) : ViewModel() {
  private var _date: MutableStateFlow<String> = MutableStateFlow(
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  )
  val date: StateFlow<String> = _date

  private var _calendar: MutableStateFlow<Calendar?> = MutableStateFlow(null)
  val calendar: StateFlow<Calendar?> = _calendar

  private var _loadCalendar = MutableStateFlow(false)
  val loadCalendar: StateFlow<Boolean> = _loadCalendar

  private var _quote = MutableStateFlow<HitokotoQuote?>(null)
  val quote: StateFlow<HitokotoQuote?> = _quote

  val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  fun isCalendarImported(): Boolean = mmkv.containsKey("courses")

  fun loadCalendar() {
    if (mmkv.containsKey("courses")) {
      val calendar = mmkv.decodeString("courses")!!
      _calendar.value = CalendarBuilder().build(calendar.byteInputStream())
      _loadCalendar.value = true
    }
  }

  private fun getTodayEvents(__date: String, __calendar: Calendar?): List<VEvent>? {
    val todayStart = LocalDate.parse(__date, formatter).atStartOfDay()
    val todayEnd = todayStart.plusDays(1).minusSeconds(1)
    return __calendar?.getComponents<VEvent>(Component.VEVENT)?.filter {
      val eventStart = it.getDateTimeStart<ZonedDateTime>().get().date.toLocalDateTime()
      val recur = it.getProperty<RRule<LocalDateTime>>(Property.RRULE).get().recur
      val dates = recur.getDates(eventStart, todayStart, todayEnd)
      dates.isNotEmpty()
    }?.sortedBy { getStartTime(it) }
  }

  private fun isCourseStopped(event: VEvent): Boolean =
    getCourseLocation(event).startsWith("停课")


  var todayEvents: StateFlow<List<VEvent>?> = combine(_date, _calendar) { __date, __calendar ->
    getTodayEvents(__date, __calendar)
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


  /*
  fun getRows(): List<Pair<LocalTime, LocalTime>> {
    // 返回去重并排序的开始时间，只要时间不要日期
    return _calendar.value?.getComponents<VEvent>(Component.VEVENT)?.filter {
      !isCourseStopped(it)
    }?.map {
      it.getDateTimeStart<ZonedDateTime>()
        .get().date.toLocalTime() to it.getDateTimeEnd<ZonedDateTime>().get().date.toLocalTime()
    }?.distinct()?.sortedBy { it.first } ?: emptyList()
  }
  */

  fun getStartTime(event: VEvent): LocalTime {
    return event.getDateTimeStart<ZonedDateTime>().get().date.toLocalTime()
  }

  fun getEndTime(event: VEvent): LocalTime {
    return event.getDateTimeEnd<ZonedDateTime>().get().date.toLocalTime()
  }

  fun getCourseName(event: VEvent): String {
    return event.getProperty<Summary>(Property.SUMMARY).get().value
  }

  fun getCourseLocation(event: VEvent): String {
    return event.getProperty<Location>(Property.LOCATION).get().value
  }

  fun getWeekday(): String {
    val localDate = LocalDate.parse(_date.value, formatter)
    return localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  fun calcCourseColor(
    event: VEvent,
    darkMode: Boolean
  ): Int {
    // 根据名称的哈希值计算颜色
    val courseName = getCourseName(event)
    return if (isCourseStopped(event)) {
      0xFFB0B0B0.toInt()
    } else {
      ColorUtils.HSLToColor(
        floatArrayOf(
          courseName.hashCode().toFloat() % 360,
          0.5f,
          if (darkMode) 0.5f else 0.8f
        )
      )
    }
  }

  fun thisWeek(): Int {
    // 计算当前周数
    val today = LocalDate.parse(_date.value, formatter)
    val termStart = LocalDate.parse(mmkv.decodeString("term_start")!!, formatter)
    return (today.toEpochDay() - termStart.toEpochDay()).toInt() / 7 + 1
  }

  fun thisWeekDates(): List<Pair<Pair<LocalDate, String>, Int>> {
    // 计算本周日期
    // 日期对象，日期字符串，课程数量
    val today = LocalDate.parse(_date.value, formatter)
    val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    return (0..6).map {
      val date = weekStart.plusDays(it.toLong())
      val dateId = date.format(formatter)
      val courseCount = getTodayEvents(
        dateId, _calendar.value
      )?.size ?: 0
      date to dateId to courseCount
    }
  }

  suspend fun initQuote() {
    _quote.value = Hitokoto().getQuote()
  }

  init {
    viewModelScope.launch {
      initQuote()
    }
  }

  fun setDate(date: String) {
    _date.value = date
  }

  fun prevWeek() {
    _date.value = LocalDate.parse(_date.value, formatter).minusWeeks(1).format(formatter)
  }

  fun nextWeek() {
    _date.value = LocalDate.parse(_date.value, formatter).plusWeeks(1).format(formatter)
  }

  fun backToday() {
    _date.value = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  }
}