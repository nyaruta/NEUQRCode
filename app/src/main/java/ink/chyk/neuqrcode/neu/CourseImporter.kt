package ink.chyk.neuqrcode.neu

import com.tencent.mmkv.*
import dev.darkokoa.pangu.*
import kotlinx.serialization.json.*
import java.time.format.*

class CourseImporter {
  /*
    NEUQRCode EAMS Course Importer v2
    2025-03-06

    credits:
    - https://github.com/neucn/elise
    - https://github.com/whoisnian/getMyCourses
   */

  companion object {
    // 浑南作息时间
    private val classStartTimeHunnan = arrayOf(
      "08:30",
      "09:25",
      "10:30",
      "11:25",
      "14:00",
      "14:55",
      "16:00",
      "16:55",
      "18:30",
      "19:25",
      "20:20",
      "21:15"
    )
    private val classEndTimeHunnan = arrayOf(
      "09:15",
      "10:10",
      "11:15",
      "12:10",
      "14:45",
      "15:40",
      "16:45",
      "17:40",
      "19:15",
      "20:10",
      "21:05",
      "22:00"
    )

    // 南湖作息时间
    private val classStartTimeNanhu = arrayOf(
      "08:00",
      "08:55",
      "10:00",
      "10:55",
      "14:00",
      "14:55",
      "16:00",
      "16:55",
      "18:30",
      "19:25",
      "20:20",
      "21:15"
    )
    private val classEndTimeNanhu = arrayOf(
      "08:45",
      "09:45",
      "10:45",
      "11:40",
      "14:45",
      "15:40",
      "16:45",
      "17:40",
      "19:15",
      "20:10",
      "21:05",
      "22:00"
    )

    @JvmStatic
    fun importCourses(
      fetchResult: CourseFetcherResult,
      mmkv: MMKV
    ) {
      // 通过 CourseFetcher 得到的结果导入课程
      // 解析 53 周的课程表并存入数据库
      // 注: v1 是 22 周，但考虑到教务系统直接返回 52 周的课程表，所以采用 52

      val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")  // 日期格式化器（20240901 -> 2024-09-01 对象）
      val termStart = fetchResult.startDay ?: throw Exception("无法解析学期开始日期")
      val termStartString = termStart.format(formatter)

      val courseKeys = mutableSetOf<String>()  // 之后将存入数据库，以在下一次导入课表时删除旧数据
      // ["course_20240901", "course_20240902", ...]

      // 删除上一次导入的数据
      mmkv.getStringSet("course_keys", emptySet())?.forEach {
        mmkv.remove(it)
      }
      mmkv.remove("term_start")

      // 预处理课程表
      val fetchedCourses =
        preprocessCourses(fetchResult.result ?: throw Exception("无法解析课程表"))

      //Log.d("CourseImporter", "Term start: $termStartString")

      // 遍历 53 周
      for (weekDelta: Long in 0L..51L) {
        // 从第 1 周开始，到第 52 周
        for (weekDayDelta: Long in 0L..6L) {
          // 周日为 0，周一为 1，...，周六为 6
          val date = termStart.plusWeeks(weekDelta).plusDays(weekDayDelta)
          val dateId = date.format(formatter)  // 20240901
          val key = "course_$dateId"  // course_20240901

          // 遍历所有课程找到当天的
          val fetchedCoursesToday: List<ImportCourse> = fetchedCourses.filter {
            it.weeks[weekDelta.toInt() + 1] == '1' && it.weekDayDelta == weekDayDelta.toInt()
          }

          val coursesToday: List<Course> = fetchedCoursesToday.map {
            Course(
              name = it.name,
              location = it.roomName,
              start = it.startTime!!,
              end = it.endTime!!,
              period = it.period!!
            )
          }

          // 存入数据库
          if (coursesToday.isNotEmpty()) {
            courseKeys.add(key)
            mmkv.encode(key, Json.encodeToString(coursesToday))
          }
        }
      }

      // 遍历结束，存入 term_start 和 course_keys
      mmkv.encode("course_keys", courseKeys)
      mmkv.encode("term_start", termStartString)
    }

    private fun preprocessCourses(courses: List<ImportCourse>): List<ImportCourse> {
      // 对获取到的 courses 进行预处理
      return courses.map { course ->

        // 使用 Pangu 加空格，去掉多余的 "校区" 二字
        val pre = course.copy(
          name = Pangu.spacingText(course.name),
          roomName = Pangu.spacingText(course.roomName).replace("校区", "")
        )

        // 上下课时间数组索引
        var st = 12
        var en = -1

        // 从两端逼近以获取上下课时间
        pre.courseTimes.forEach { courseTime ->
          if (st > courseTime.timeOfTheDay) {
            st = courseTime.timeOfTheDay
          }
          if (en < courseTime.timeOfTheDay) {
            en = courseTime.timeOfTheDay
          }
        }

        // 加上上下课时间字符串
        if (pre.roomName.contains("浑南")) {
          pre.startTime = classStartTimeHunnan[st]
          pre.endTime = classEndTimeHunnan[en]
        } else {
          // 不是浑南就是南湖呗
          pre.startTime = classStartTimeNanhu[st]
          pre.endTime = classEndTimeNanhu[en]
        }

        // 纠正周几（校历第一天是周日，weekDayDelta=0，但获取到的课表第一天是周一，dayOfTheWeek=0）
        // 也就是说，如果某一节课是周二上，那么它本来的 dayOfTheWeek=1，但是我们要把它改成 2
        pre.weekDayDelta = (pre.courseTimes.first().dayOfTheWeek + 1) % 7

        // 课程时间段

        if (en <= 3) {
          // 早间课程
          pre.period = CoursePeriod.MORNING
        } else if (en <= 7) {
          // 下午课程
          pre.period = CoursePeriod.AFTERNOON
        } else {
          // 晚间课程
          pre.period = CoursePeriod.EVENING
        }

        return@map pre
      }.sortedBy { it.startTime }
    }
  }
}