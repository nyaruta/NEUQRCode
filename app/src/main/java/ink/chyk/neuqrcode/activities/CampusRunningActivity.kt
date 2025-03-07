package ink.chyk.neuqrcode.activities

import android.*
import android.content.Intent.*
import android.content.pm.*
import android.net.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.browser.customtabs.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.*
import ink.chyk.neuqrcode.ui.theme.*
import ink.chyk.neuqrcode.R

class CampusRunningActivity : ComponentActivity() {
  // Declare permission launcher at class level
  private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

  // Permission state for the Composable to observe
  private val permissionState = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val url = intent.getStringExtra("url")

    // Register the launcher before the activity starts
    requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) {
      permissionState.value = it
    }

    // Check permission status initially
    checkLocationPermission()

    setContent {
      CampusRunningScreen(url ?: "https://www.neu.edu.cn/")
    }
  }

  private fun checkLocationPermission() {
    ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACCESS_FINE_LOCATION
    ).let {
      if (it == PackageManager.PERMISSION_GRANTED) {
        permissionState.value = true
      } else {
        permissionState.value = false
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
      }
    }
  }

  @Composable
  fun CampusRunningScreen(url: String) {
    // Use the permission state from the activity
    val permissionGranted by remember { permissionState }

    val context = LocalContext.current

    AppTheme {
      Scaffold { innerPadding ->
        if (permissionGranted) {
          Text(stringResource(R.string.loading))

          val customTabsIntent = CustomTabsIntent.Builder()
            .setUrlBarHidingEnabled(true)
            .setShowTitle(false)
            .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
            .setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .build()
          customTabsIntent.intent.addFlags(FLAG_ACTIVITY_NEW_DOCUMENT)
          customTabsIntent.launchUrl(context, Uri.parse(url))

          // 退出 activity
          finish()
        } else {
          PermissionGrantScreen(Modifier.padding(innerPadding))
        }
      }
    }
  }

  @Composable
  fun PermissionGrantScreen(modifier: Modifier = Modifier) {
    Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(id = R.drawable.neko1),
        contentDescription = "Neko",
      )
      Spacer(modifier = Modifier.height(32.dp))
      Text(
        text = stringResource(id = R.string.location_permission_required),
        style = MaterialTheme.typography.headlineMedium,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(id = R.string.location_permission_required_content),
        textAlign = TextAlign.Center,
      )
    }
  }
}