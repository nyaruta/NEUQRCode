package ink.chyk.neuqrcode

import android.content.*
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tencent.mmkv.MMKV
import ink.chyk.neuqrcode.components.AppBackground
import kotlinx.coroutines.*

class LoginActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val lightTransparentStyle = SystemBarStyle.light(
      scrim = TRANSPARENT,
      darkScrim = TRANSPARENT
    )
    enableEdgeToEdge(
      statusBarStyle = lightTransparentStyle,
      navigationBarStyle = lightTransparentStyle
    )


    setContent {
      AppBackground {
        LoginForm(openMainPage = {
          val intent = Intent(this, MainActivity::class.java)
          startActivity(intent)
          finish()
        })
      }
    }
  }
}

@Composable
fun LoginForm(openMainPage: () -> Unit = {}) {
  var studentId by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var showErrorMessage by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var buttonEnabled by remember { mutableStateOf(true) }

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
        UniformAuthTitle()
        StudentIdInput(studentId, onStudentIdChange = {
          studentId = it
          buttonEnabled = it.length == 8
        })
        PasswordInput(
          password = password,
          onPasswordChange = { password = it },
          passwordVisible = passwordVisible,
          onPasswordVisibleChange = { passwordVisible = !passwordVisible }
        )
        LoginButton(
          studentId, password, openErrorDialog = {
            errorMessage = it
            showErrorMessage = true
          },
          openMainPage = openMainPage,
          enabled = buttonEnabled,
          onClickAnd = {
            buttonEnabled = false
          }
        )
        if (showErrorMessage) {
          ErrorDialog(errorMessage, onDismissRequest = {
            showErrorMessage = false
          })
        }
      }
    }
  }
}

@Composable
fun UniformAuthTitle() {
  // 统一身份认证标题
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .padding(vertical = 16.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Image(
      painter = painterResource(
        if (isSystemInDarkTheme()) {
          R.drawable.logo_white
        } else {
          R.drawable.logo_black
        }
      ),
      contentDescription = "NEU Logo",
    )
    Text("统一身份认证", style = MaterialTheme.typography.headlineSmall)
  }
}

@Composable
fun StudentIdInput(studentId: String, onStudentIdChange: (String) -> Unit) {
  // 学号输入框
  OutlinedTextField(
    value = studentId,
    onValueChange = onStudentIdChange,
    label = { Text("学号") },
    leadingIcon = {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_person_24_filled),
        contentDescription = "学号"
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
  )
}

@Composable
fun PasswordInput(
  password: String,
  onPasswordChange: (String) -> Unit,
  passwordVisible: Boolean,
  onPasswordVisibleChange: () -> Unit
) {
  // 密码输入框
  OutlinedTextField(
    value = password,
    onValueChange = onPasswordChange,
    label = { Text("密码") },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    visualTransformation = if (passwordVisible) {
      VisualTransformation.None
    } else {
      PasswordVisualTransformation()
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    leadingIcon = {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_password_24_filled),
        contentDescription = "密码"
      )
    },
    trailingIcon = {
      val icon = painterResource(
        if (passwordVisible) {
          R.drawable.ic_fluent_eye_24_filled
        } else {
          R.drawable.ic_fluent_eye_off_24_filled
        }
      )
      val description = if (passwordVisible) "隐藏密码" else "显示密码"
      IconButton(onClick = onPasswordVisibleChange) {
        Icon(painter = icon, contentDescription = description)
      }
    },
  )
}

@Composable
fun LoginButton(
  studentId: String,
  password: String,
  openErrorDialog: (String) -> Unit,
  openMainPage: () -> Unit,
  enabled: Boolean,
  onClickAnd: () -> Unit
) {
  // 登录按钮
  val coroScope = rememberCoroutineScope()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 16.dp),
    horizontalArrangement = Arrangement.End
  ) {
    Button(
      onClick = {
        onClickAnd()
        val mmkv = MMKV.defaultMMKV()
        mmkv.encode("student_id", studentId)
        mmkv.encode("password", password)

        coroScope.launch {
          try {
            val neu = NEUPass()
            val ticket = neu.loginPortalTicket(studentId, password)
            // 登录成功
            mmkv.encode("portal_ticket", ticket)
            openMainPage()
          } catch (e: PasswordIncorrectException) {
            // 密码错误
            openErrorDialog("密码错误，请检查后重试。")
          } catch (e: RequestFailedException) {
            // 请求失败
            openErrorDialog("请求失败，请检查网络连接。")
          }
        }
      },
      enabled = enabled
    ) {
      Text("登录")
    }
  }
}

@Composable
fun ErrorDialog(errorMessage: String, onDismissRequest: () -> Unit = {}) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text("登录失败") },
    text = { Text(errorMessage) },
    confirmButton = {
      Button(
        onClick = onDismissRequest
      ) {
        Text("确定")
      }
    }
  )
}