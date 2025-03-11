package ink.chyk.neuqrcode.activities

import android.Manifest
import android.content.pm.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.*
import androidx.lifecycle.*
import ink.chyk.neuqrcode.ui.theme.*
import com.tencent.smtt.sdk.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import ink.chyk.neuqrcode.*
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

    enableEdgeToEdge()

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

    val webViewState = remember { mutableStateOf<WebView?>(null) }
    val isDarkMode = isSystemInDarkTheme()

    AppTheme {
      Scaffold { innerPadding ->
        if (permissionGranted) {
          CustomWebView(
            url,
            webViewState,
            Modifier.padding(innerPadding),
            isDarkMode,
            listenBack = false,
            finish = { finish() })
        } else {
          PermissionGrantScreen(Modifier.padding(innerPadding))
        }
      }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleEvent = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }


    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        lifecycleEvent.value = event
        when (event) {
          Lifecycle.Event.ON_PAUSE -> webViewState.value?.onPause()
          Lifecycle.Event.ON_RESUME -> webViewState.value?.onResume()
          Lifecycle.Event.ON_DESTROY -> webViewState.value?.destroy()
          else -> {}
        }
      }

      lifecycleOwner.lifecycle.addObserver(observer)

      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        webViewState.value?.destroy()
      }
    }

    /*
    BackHandler {
      if (webViewState.value?.canGoBack() == true) {
        webViewState.value?.goBack()
      } else {
        webViewState.value?.destroy()
        finish()
      }
    }
     */
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