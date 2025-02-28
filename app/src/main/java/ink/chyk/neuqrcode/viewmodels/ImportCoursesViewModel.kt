package ink.chyk.neuqrcode.viewmodels

import android.content.*
import android.util.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import net.fortuna.ical4j.data.*
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.*
import net.fortuna.ical4j.model.parameter.*
import net.fortuna.ical4j.model.property.*
import java.io.*
import java.time.*
import java.time.format.*
import kotlin.Pair

@Serializable
data class Course(
  val name: String,
  val location: String,
  val start: String,
  val end: String,
  val period: CoursePeriod
)

enum class CoursePeriod {
  MORNING, AFTERNOON, EVENING
}

class ImportCoursesViewModel(
  private val mmkv: MMKV,
) : ViewModel() {

  private var _importing = MutableStateFlow(false)
  val importing: StateFlow<Boolean> = _importing
  private var _importCompleted = MutableStateFlow(false)
  val importCompleted: StateFlow<Boolean> = _importCompleted
  private var _output = MutableStateFlow("")
  val output: StateFlow<String> = _output
  private var _resultContent = MutableStateFlow<String?>(null)
  val resultContent: StateFlow<String?> = _resultContent
  private var _hasErrors = MutableStateFlow(false)
  val hasErrors: StateFlow<Boolean> = _hasErrors

  fun getEliseBinaryResource(): Int {
    val arch: String = System.getProperty("os.arch") ?: "unknown"
    when {
      arch.startsWith("arm64") || arch.startsWith("aarch64") -> {
        return R.raw.elise_arm64
      }

      else -> throw Exception("Unsupported architecture: $arch")
    }
  }

  fun extractExecutable(context: Context, resource: Int): File {
    val executableFile = File(context.cacheDir, "elise_arm64")
    if (!executableFile.exists()) {
      context.resources.openRawResource(resource).use { input ->
        executableFile.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      // 设置执行权限
      executableFile.setExecutable(true)
    }
    return executableFile
  }

  fun getStudentIdAndPassword() = Pair(
    mmkv.decodeString("student_id")!!,
    mmkv.decodeString("password")!!
  )

  private fun _runImport(
    context: Context,
    resource: Int,
  ): Pair<String, String?> {
    val executable = extractExecutable(context, resource)
    val (studentId, password) = getStudentIdAndPassword()
    val resultFile = File(context.cacheDir, "courses.ics")
    if (resultFile.exists()) {
      resultFile.delete() // 清除旧的结果文件
    }

    val processBuilder = ProcessBuilder(
      executable.absolutePath,
      "-u", studentId, "-p", password, "-o", resultFile.absolutePath
    )
      .directory(context.filesDir) // 设置工作目录
      .redirectErrorStream(true)  // 将错误流重定向到标准输出

    val process = processBuilder.start()

    // 获取标准输出
    val output = process.inputStream.bufferedReader().use { it.readText() }

    // 等待进程完成
    process.waitFor()

    // 检查结果文件
    val resultContent = if (resultFile.exists()) {
      resultFile.readText()
    } else {
      null
    }

    return output to resultContent
  }

  private fun getTermStart(text: String): String {
    // 定义正则表达式
    val regex = Regex("""本学期开始于 (\d{4})-(\d{2})-(\d{2})""")

    // 匹配并提取日期
    val matchResult = regex.find(text)
    val year = matchResult?.groupValues?.get(1)
    val month = matchResult?.groupValues?.get(2)
    val day = matchResult?.groupValues?.get(3)

    val ret= "$year$month$day"  // 20240901
    Log.d("ImportCoursesViewModel", "Term start: $ret")
    return ret
  }


  private fun getStartTime(event: VEvent): LocalTime {
    return event.startDate.date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
  }

  private fun getEndTime(event: VEvent): LocalTime {
    return event.endDate.date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
  }

  private suspend fun importCourses(
    calendarString: String,
    termStart: String
  ) = withContext(Dispatchers.IO) {
    // 解析前 22 周的课表并且存入数据库
    val calendar = CalendarBuilder().build(calendarString.byteInputStream())  // 日历对象
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")  // 日期格式化器（20240901 -> 2024-09-01 对象）

    val courseKeys = mutableSetOf<String>()  // 之后将存入数据库，以在下一次导入课表时删除旧数据
    // ["course_20240901", "course_20240902", ...]

    // 删除上一次导入的数据
    mmkv.getStringSet("course_keys", emptySet())?.forEach {
      mmkv.remove(it)
    }
    mmkv.remove("term_start")

    LocalDate.parse(termStart, formatter).let { termStartDate ->
      for (daysDelta in 0..(22 * 7)) {
        val date = termStartDate.plusDays(daysDelta.toLong())
        val dateId = date.format(formatter)  // 20240901
        val key = "course_$dateId"  // course_20240901

        // 当天的开始和结束
        val todayStart = date.atStartOfDay()
        val todayEnd = todayStart.plusDays(1).minusSeconds(1)
        val todayStartDateTime = DateTime(todayStart.toInstant(ZoneOffset.UTC).toEpochMilli())
        val todayEndDateTime = DateTime(todayEnd.toInstant(ZoneOffset.UTC).toEpochMilli())

        // 解析当日的课程
        val todayCourses = calendar.getComponents<VEvent>(Component.VEVENT)?.filter {
          val recur = it.getProperty<RRule>(Property.RRULE).recur
          val dates = recur.getDates(
            it.startDate.date,
            Period(
              todayStartDateTime,
              todayEndDateTime
            ),
            Value.DATE_TIME
          )
          dates.isNotEmpty()
        }?.sortedBy { getStartTime(it) }?.map {
          Course(
            it.summary.value,
            it.location.value,
            getStartTime(it).format(DateTimeFormatter.ofPattern("HH:mm")),
            getEndTime(it).format(DateTimeFormatter.ofPattern("HH:mm")),
            when (getStartTime(it)) {
              in LocalTime.of(7, 0)..LocalTime.of(12, 30) -> CoursePeriod.MORNING
              in LocalTime.of(12, 31)..LocalTime.of(18, 0) -> CoursePeriod.AFTERNOON
              else -> CoursePeriod.EVENING
            }
          )
        }

        // 存入数据库
        courseKeys.add(key)
        mmkv.encode(key, Json.encodeToString(todayCourses ?: emptyList()))
      }

      // for 循环结束
      mmkv.encode("course_keys", courseKeys)
      mmkv.encode("term_start", termStart)
    }
  }

  fun runImport(
    context: Context,
    resource: Int
  ) {
    _importing.value = true
    _importCompleted.value = false
    _hasErrors.value = false
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        _runImport(context, resource)
      }.let { (output, resultContent) ->
        val ctx = context.applicationContext
        Log.d("ImportCoursesViewModel", output)
        _output.value = output
        _resultContent.value = resultContent ?: ctx.getString(R.string.import_failed)
        if (resultContent != null) {
          withContext(Dispatchers.IO) {
            importCourses(resultContent, getTermStart(output))
          }
          _importCompleted.value = true
        } else {
          handleErrors(output)
        }
        _importing.value = false
      }
    }
  }

  fun handleErrors(
    output: String
  ) {
    _importCompleted.value = true
    _hasErrors.value = true
    if (output.contains("解析课表时出错")) {
      if (output.contains("Timeout") || output.contains("Client.")) {
        _output.value += "\n无法连接到教务系统，可能是开启了仅限内网访问。\n请连接到校园网后重试。"
      }
    } else {
      _output.value += "\n未知错误，请联系开发者！"
    }
  }
}
