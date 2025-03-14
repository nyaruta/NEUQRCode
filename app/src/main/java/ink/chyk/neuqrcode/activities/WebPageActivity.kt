package ink.chyk.neuqrcode.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.ui.theme.*

class WebPageActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra("url") ?: "https://stu.neu.edu.cn/"
    val scripts = intent.getStringExtra("scripts")
    val cookies = intent.getStringExtra("cookies")

    enableEdgeToEdge()
    setContent {
      AppTheme {
        Scaffold { innerPadding ->
          WebPageScreen(url, scripts, cookies, innerPadding)
        }
      }
    }
  }

  @Composable
  fun WebPageScreen(
    url: String,
    scripts: String?,
    cookies: String?,
    innerPadding: PaddingValues
  ) {
    CustomWebView(
      url = url,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      finish = { finish() },
      cookies = cookies,
      extraScripts = scripts
    )
  }
}