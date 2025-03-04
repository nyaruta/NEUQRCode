package ink.chyk.neuqrcode.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.ui.theme.*

class ErrorActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppTheme {
        Scaffold { innerPadding ->
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(R.string.request_failed_title),
                style = MaterialTheme.typography.headlineMedium
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(R.string.request_failed_content)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Image(
                painter = painterResource(R.drawable.potato_server),
                contentDescription = null,
              )
            }
          }
        }
      }
    }
  }
}