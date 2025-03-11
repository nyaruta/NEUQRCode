package ink.chyk.neuqrcode.activities

import android.*
import android.annotation.*
import android.content.pm.*
import android.os.*
import android.webkit.WebSettings.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.core.content.*
import androidx.lifecycle.*
import com.tencent.smtt.export.external.interfaces.*
import ink.chyk.neuqrcode.ui.theme.*
import com.tencent.smtt.sdk.*
import androidx.lifecycle.compose.LocalLifecycleOwner
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
          CampusRunningWebView(url, webViewState, Modifier.padding(innerPadding), isDarkMode)
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

  @SuppressLint("SetJavaScriptEnabled")
  @Composable
  fun CampusRunningWebView(
    url: String,
    state: MutableState<WebView?> = remember { mutableStateOf(null) },
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
  ) {
    // 封装 TBS WebView 组件
    // 部分代码来自 https://stackoverflow.com/questions/34986413/location-is-accessed-in-chrome-doesnt-work-in-webview/34990055

    val ctx = LocalContext.current

    AndroidView(factory = { context ->
      val webView = WebView(context).apply {
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          setGeolocationEnabled(true)
          setAppCacheEnabled(true)
          setGeolocationDatabasePath(ctx.filesDir.path)
          mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
          useWideViewPort = true
          allowFileAccess = true
          allowContentAccess = true
          safeBrowsingEnabled = false


          webChromeClient = object: WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
              origin: String?,
              callback: GeolocationPermissionsCallback?
            ) {
              callback?.invoke(origin, true, false)
            }
          }

          webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
              // 允许页面跳转
              view?.loadUrl(url)
              return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              // 注入暗黑模式 css
              super.onPageFinished(view, url)
              if (isDarkMode) {
                view?.evaluateJavascript(
                  """
                  (() => {
                    const startBtn = document.querySelector(".run__start-btn")
                    if (startBtn) startBtn.click()
                    
                    const css = document.createElement('style')
                    css.type = 'text/css'
                    css.innerHTML = 'html { filter: invert(1) hue-rotate(180deg) saturate(1); }'
                    document.head.appendChild(css)
                  })()
                  """.trimIndent()
                ) {}
              }
            }
          }
        }
      }

      state.value = webView
      webView.loadUrl(url)
      webView
    }, modifier = modifier.fillMaxSize())
  }
}