package ink.chyk.neuqrcode.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.ui.theme.*
import kotlinx.coroutines.*

class ErrorActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val monitor = NetworkMonitor(this)

    setContent {
      ErrorScreen(monitor)
    }
  }

  @Composable
  fun ErrorScreen(monitor: NetworkMonitor) {
    val isConnected = monitor.isConnected.collectAsState()

    LaunchedEffect(Unit) {
      withContext(Dispatchers.IO) {
        monitor.startMonitoring()
      }
    }

    AppTheme {
      Scaffold { innerPadding ->
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Image(
                painter = painterResource(R.drawable.no_internet),
                contentDescription = "No Internet Icon",
                modifier = Modifier.size(32.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.request_failed_title),
                style = MaterialTheme.typography.headlineMedium,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.request_failed_content),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(
              painter = painterResource(R.drawable.potato_server),
              contentDescription = "Potato Server",
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
              onClick = {
                finish()
              },
              modifier = Modifier.width(96.dp),
              enabled = isConnected.value
            ) {
              Text(stringResource(R.string.retry))
            }
          }
        }
      }
    }
  }
}