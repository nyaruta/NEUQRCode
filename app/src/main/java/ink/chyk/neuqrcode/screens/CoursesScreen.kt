package ink.chyk.neuqrcode.screens

import android.content.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.*
import dev.darkokoa.pangu.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.viewmodels.*
import net.fortuna.ical4j.model.component.*
import java.time.*
import java.time.format.TextStyle as TimeTextStyle
import java.util.Locale


@Composable
fun CoursesScreen(viewModel: CoursesViewModel, navController: NavController) {
  val loadCalendar by viewModel.loadCalendar.collectAsState()

  if (!viewModel.isCalendarImported()) {
    return ImportCoursesSplash()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    if (loadCalendar) {
      TodayCourses(viewModel)
    } else {
      CircularProgressIndicator()
    }
  }
}

val MORNING = R.drawable.ic_fluent_weather_sunny_24_regular
val AFTERNOON = R.drawable.ic_fluent_weather_sunny_low_24_regular
val NIGHT = R.drawable.ic_fluent_weather_partly_cloudy_night_24_regular

@Composable
fun TodayCourses(viewModel: CoursesViewModel) {

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      TodayTitle(viewModel)
      Spacer(modifier = Modifier.height(32.dp))
      CoursesCard(viewModel)
    }
    DaySelector(viewModel)
  }
}

@Composable
fun TodayTitle(viewModel: CoursesViewModel) {
  val date by viewModel.date.collectAsState()
  val quote by viewModel.quote.collectAsState()
  val ctx = LocalContext.current

  Text(
    ctx.getString(R.string.date).format(
      date.slice(4..5).toInt(),
      date.slice(6..7).toInt(),
      viewModel.getWeekday()
    ),
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(modifier = Modifier.height(8.dp))
  Column(
    modifier = Modifier.fillMaxWidth()
  ) {
    if (quote != null) {
      val quote = quote!!
      Text(
        quote.hitokoto,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        "—— ${quote.from}",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
      )
    }
  }
}

@Composable
fun CoursesCard(viewModel: CoursesViewModel) {
  val todayEvents by viewModel.todayEvents.collectAsState()
  val scrollState = rememberScrollState()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(0.dp)
      .verticalScroll(scrollState),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
    ) {
      if (todayEvents.isNullOrEmpty()) {
        NoCoursesSplash(viewModel)
      } else {
        TodayCoursesList(todayEvents, viewModel)
      }
    }
  }
}

@Composable
fun TodayCoursesList(
  todayEvents: List<VEvent>?,
  viewModel: CoursesViewModel,
  dark: Boolean = isSystemInDarkTheme()
) {
  val ctx = LocalContext.current
  var previousIcon = 0
  todayEvents?.forEach {
    val startTime = viewModel.getStartTime(it)
    val endTime = viewModel.getEndTime(it)
    val icon = if (endTime.isBefore(LocalTime.of(12, 30))) {
      MORNING
    } else if (startTime.isAfter(LocalTime.of(18, 0))) {
      NIGHT
    } else {
      AFTERNOON
    }

    if (icon != previousIcon) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          when (icon) {
            MORNING -> ctx.getString(R.string.morning_courses)
            AFTERNOON -> ctx.getString(R.string.afternoon_courses)
            NIGHT -> ctx.getString(R.string.evening_courses)
            else -> ""
          },
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
          modifier = Modifier
            .height(1.dp)
            .fillMaxWidth(),
          color = if (dark) Color.DarkGray else Color.LightGray
        ) {}
      }
      previousIcon = icon
    }

    Box(
      modifier = Modifier
        .height(64.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(startTime.toString())
            Text(endTime.toString())
          }
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            modifier = Modifier
              .width(4.dp)
              .height(40.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = Color(viewModel.calcCourseColor(it, dark))
          ) {}
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              Pangu.spacingText(viewModel.getCourseName(it)),
              style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
            )
            Text(
              locationToAnnotated(viewModel.getCourseLocation(it)),
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
        Icon(
          painter = painterResource(icon),
          contentDescription = "Time Delimeter",
        )
      }
    }
  }
}

@Composable
fun DaySelector(
  viewModel: CoursesViewModel,
  dark: Boolean = isSystemInDarkTheme()
) {
  val ctx = LocalContext.current
  val dateState by viewModel.date.collectAsState()

  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      viewModel.thisWeekDates().forEach {
        // 简单解包
        val (pack1, courseCount) = it
        val (date, dateId) = pack1

        val backgroundColor = if (dateId == dateState) {
          if (dark) Color.DarkGray else Color.LightGray
        } else Color.Transparent

        Box(
          modifier = Modifier
            .weight(1f) // 每个子项占相等比例的空间
            .aspectRatio(1f) // 保持正方形
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable {
              viewModel.setDate(dateId)
            },
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              date.dayOfMonth.toString(),
              // style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              "${
                date.dayOfWeek.getDisplayName(
                  TimeTextStyle.SHORT,
                  Locale.getDefault()
                )
              } ${courseCount}",
              style = TextStyle(fontSize = 8.sp)
            )
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      PrevNextButton(
        icon = R.drawable.ic_fluent_chevron_left_20_filled,
        onClick = { viewModel.prevWeek() }
      )
      Text(
        ctx.getString(R.string.current_week).format(viewModel.thisWeek()),
        modifier = Modifier.clickable {
          viewModel.backToday()
        }
      )
      PrevNextButton(
        icon = R.drawable.ic_fluent_chevron_right_20_filled,
        onClick = { viewModel.nextWeek() }
      )
    }
  }
}

@Composable
fun PrevNextButton(
  icon: Int,
  onClick: () -> Unit
) {
  IconButton(
    onClick = onClick
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = "Prev/Next Button",
    )
  }
}

@Composable
fun NoCoursesSplash(viewModel: CoursesViewModel) {
  val ctx = LocalContext.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(48.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      painter = painterResource(
        listOf(R.drawable.neko1, R.drawable.neko2, R.drawable.neko3).random()
      ),
      contentDescription = "No Courses",
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      ctx.getString(R.string.no_course),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = Modifier.height(16.dp))
    ImportCoursesButton()
  }
}

@Composable
fun ImportCoursesSplash() {
  val ctx = LocalContext.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            painter = painterResource(R.drawable.ic_fluent_arrow_download_24_regular),
            contentDescription = "Import Courses",
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            ctx.getString(R.string.import_courses),
            style = MaterialTheme.typography.headlineMedium
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(ctx.getString(R.string.import_courses_splash_1))
        Text(ctx.getString(R.string.import_courses_splash_2))
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          ImportCoursesButton()
        }
      }
    }
  }
}

@Composable
fun ImportCoursesButton(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  Button(
    modifier = modifier,
    onClick = {
      val intent = Intent(context, ImportCoursesActivity::class.java)
      context.startActivity(intent)
    }) { Text(context.getString(R.string.import_courses)) }
}


fun locationToAnnotated(text: String): AnnotatedString {
  return if (text.endsWith("校区)")) {
    val location = text.substringBefore("(")
    val campus = text.substringAfter("(").substringBefore("校区")
    buildAnnotatedString {
      withStyle(style = SpanStyle(fontSize = 14.sp)) {
        append(Pangu.spacingText(location))
      }
      withStyle(
        style = SpanStyle(
          fontSize = 14.sp,
          color = Color.Gray
        )
      ) {
        append("（")
        append(campus)
        append("校区）")
      }
    }
  } else {
    buildAnnotatedString {
      withStyle(style = SpanStyle(fontSize = 14.sp)) {
        append(Pangu.spacingText(text))
      }
    }
  }
}