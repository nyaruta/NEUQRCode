package ink.chyk.neuqrcode.viewmodels

import android.content.*
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*

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

    return "$year$month$day"
  }

  fun runImport(
    context: Context,
    resource: Int
  ) {
    _importing.value = true
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        _runImport(context, resource)
      }.let { (output, resultContent) ->
        _output.value = output
        _resultContent.value = resultContent
        mmkv.encode("courses", resultContent)
        mmkv.encode("term_start", getTermStart(output))
        _importing.value = false
        _importCompleted.value = true
      }
    }
  }
}
