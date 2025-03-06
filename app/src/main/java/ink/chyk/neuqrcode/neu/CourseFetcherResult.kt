package ink.chyk.neuqrcode.neu

import java.time.ZonedDateTime

data class CourseFetcherResult(
  val result: List<ImportCourse>? = null,
  val startDay: ZonedDateTime? = null,
  val output: String,
  val hasErrors: Boolean,
)