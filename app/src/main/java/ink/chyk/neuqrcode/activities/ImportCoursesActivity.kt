package ink.chyk.neuqrcode.activities

import android.content.*
import android.graphics.Color.*
import android.os.*
import android.widget.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.viewmodels.*

class ImportCoursesActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 初始化 mmkv
    MMKV.initialize(this)

    val lightTransparentStyle = SystemBarStyle.light(
      scrim = TRANSPARENT,
      darkScrim = TRANSPARENT
    )
    enableEdgeToEdge(
      statusBarStyle = lightTransparentStyle,
      navigationBarStyle = lightTransparentStyle
    )


    setContent {
      ImportCoursesScreen()
    }
  }
}


@Composable
fun ImportCoursesScreen() {
  val ctx = LocalContext.current
  val viewModel = viewModel<ImportCoursesViewModel>(factory = ImportCoursesViewModelFactory())
  val importing = viewModel.importing.collectAsState()
  val importCompleted = viewModel.importCompleted.collectAsState()
  val output = viewModel.output.collectAsState()
  val resultContent = viewModel.resultContent.collectAsState()

  val eliseBinaryResource: Int
  try {
    // elise 可执行文件的资源 ID
    eliseBinaryResource = viewModel.getEliseBinaryResource()
  } catch (e: Exception) {
    return AppBackground {
      UnsupportedArchitectureCard()
    }
  }

  try {
    val (studentId, password) = viewModel.getStudentIdAndPassword()
  } catch (e: Exception) {
    // 此情形一般不会发生，因为 MainActivity 中已经检查过登录状态
    // 但如果有用户手贱，用 Activity Manager 之类工具启动本 Activity，就会发生
    // 保险起见，还是检查
    Toast.makeText(ctx, "请先登录校园账号。", Toast.LENGTH_SHORT).show()
    val intent = Intent(ctx, LoginActivity::class.java)
    ctx.startActivity(intent)
    return
  }

  AppBackground {
    Box(
      modifier = Modifier
        .fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          NEUTitle(ctx.getString(R.string.import_courses))
          Spacer(modifier = Modifier.height(16.dp))
          if (!importCompleted.value) {
            ImportContent(
              importing = importing,
              onClick = {
                viewModel.runImport(
                  ctx, eliseBinaryResource
                )
              }
            )
          } else {
            AfterImportContent(
              output = output,
              resultContent = resultContent
            )
          }
        }
      }
    }
  }
}

@Composable
fun ImportContent(
  importing: State<Boolean>,
  onClick: () -> Unit
) {
  val ctx = LocalContext.current
  Text(
    ctx.getString(R.string.import_courses_content_1)
  )
  Text(
    buildAnnotatedString {
      append(ctx.getString(R.string.import_courses_content_2))
      withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(ctx.getString(R.string.import_courses_content_3))
      }
      append(ctx.getString(R.string.import_courses_content_4))
    }
  )
  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = onClick,
    enabled = !importing.value,
    modifier = Modifier.fillMaxWidth()
  ) {
    if (importing.value) {
      CircularProgressIndicator()
    } else {
      Text(ctx.getString(R.string.import_courses))
    }
  }
}

@Composable
fun AfterImportContent(
  output: State<String>,
  resultContent: State<String?>
) {
  val scrollState = rememberScrollState()
  val ctx = LocalContext.current

  OutlinedTextField(
    value = output.value,
    onValueChange = {},
    label = { Text(ctx.getString(R.string.logs)) },
    readOnly = true,
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .height(400.dp)
  )
  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = {
      val intent = Intent(ctx, MainActivity::class.java)
      ctx.startActivity(intent)
    },
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(ctx.getString(R.string.confirm))
  }
}

@Composable
fun UnsupportedArchitectureCard() {
  val ctx = LocalContext.current

  Card(
    modifier = Modifier.padding(16.dp),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_fluent_phone_24_regular),
          contentDescription = "Device",
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          ctx.getString(R.string.unsupported_architecture),
          style = MaterialTheme.typography.headlineMedium
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        ctx.getString(R.string.unsupported_architecture_content_1),
        style = MaterialTheme.typography.bodyMedium
      )
      Text(
        ctx.getString(R.string.unsupported_architecture_content_2),
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}