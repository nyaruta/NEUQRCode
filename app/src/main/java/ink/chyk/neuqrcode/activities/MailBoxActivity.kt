package ink.chyk.neuqrcode.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.ui.theme.*

class MailBoxActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra("url") ?: "https://stu.neu.edu.cn/"

    enableEdgeToEdge()
    setContent {
      AppTheme {
        Scaffold { innerPadding ->
          MailBoxScreen(url, innerPadding)
        }
      }
    }
  }

  @Composable
  fun MailBoxScreen(url: String, innerPadding: PaddingValues) {
    CustomWebView(
      url = url,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      finish = { finish() })
  }
}