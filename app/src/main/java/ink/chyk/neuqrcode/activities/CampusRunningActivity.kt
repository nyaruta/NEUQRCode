package ink.chyk.neuqrcode.activities

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

class CampusRunningActivity: ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      CampusRunningScreen()
    }
  }

  @Composable
  fun CampusRunningScreen() {
    Text("Hello, Campus Running!")
  }
}