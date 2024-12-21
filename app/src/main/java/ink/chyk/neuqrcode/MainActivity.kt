package ink.chyk.neuqrcode

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.google.zxing.*
import com.google.zxing.qrcode.*
import com.google.zxing.qrcode.decoder.*
import com.tencent.mmkv.MMKV
import ink.chyk.neuqrcode.components.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 初始化 mmkv
    MMKV.initialize(this)
    val mmkv = MMKV.defaultMMKV()

    // 检查是否登录？
    if (!mmkv.containsKey("student_id")) {
      Toast.makeText(this, "请先登录校园账号。", Toast.LENGTH_SHORT).show()
      val intent = Intent(this, LoginActivity::class.java)
      startActivity(intent)
      finish()
      return
    }

    enableEdgeToEdge()
    setContent {
      AppBackground {
        ECodeView(mmkv)
      }
    }
  }
}

@Composable
fun ECodeView(mmkv: MMKV) {
  var showCode by remember { mutableStateOf(false) }
  var code by remember { mutableStateOf("") }
  var codeExpiredAt by remember { mutableStateOf(0L) }  // 以 ms 为单位的时间戳

  var userInfo by remember { mutableStateOf<ECodeUserinfoAttributes?>(null) }

  val scope = rememberCoroutineScope()

  suspend fun getPortalTicket(neu: NEUPass, mmkv: MMKV, reLogin: Boolean = false): String {
    val studentId = mmkv.decodeString("student_id")!!
    val password = mmkv.decodeString("password")!!

    var portalTicket: String? = mmkv.decodeString("portal_ticket")
    if (portalTicket == null || reLogin) {
      portalTicket = neu.loginPortalTicket(studentId, password)
      mmkv.encode("portal_ticket", portalTicket)
    }

    return portalTicket
  }

  suspend fun getECodeTicket(neu: NEUPass, mmkv: MMKV): String {
    var eCodeTicket: String? = mmkv.decodeString("ecode_ticket")
    if (eCodeTicket == null) {
      var portalTicket = getPortalTicket(neu, mmkv)
      try {
        // 假设 portalTicket 没过期
        eCodeTicket = neu.loginECodeTicket(portalTicket)
      } catch (e: TicketFailedException) {
        // 重新登录
        portalTicket = getPortalTicket(neu, mmkv, true)
        eCodeTicket = neu.loginECodeTicket(portalTicket)
      }
      mmkv.encode("ecode_ticket", eCodeTicket)
    }
    return eCodeTicket!! // 不可能为空!!
  }

  suspend fun getECodeSession(neu: NEUPass, mmkv: MMKV): ECodeSession {
    var eCodeSession: String? = mmkv.decodeString("ecode_session")
    var xsrfToken: String? = mmkv.decodeString("xsrf_token")
    var expiredAt: Long? = mmkv.decodeLong("expired_at")  // 以秒为单位

    val current = System.currentTimeMillis() / 1000
    if (eCodeSession == null || xsrfToken == null || expiredAt == null || expiredAt < current) {
      Log.d("ECode", "Session 过期")
      val session = neu.newECodeSession()
      eCodeSession = session.session
      xsrfToken = session.xsrfToken
      expiredAt = session.expiredAt
      mmkv.encode("ecode_session", eCodeSession)
      mmkv.encode("xsrf_token", xsrfToken)
      mmkv.encode("expired_at", expiredAt)
    }

    // 不可能为空!!
    return ECodeSession(eCodeSession, xsrfToken, expiredAt)
  }

  LaunchedEffect(Unit) {
    scope.launch {
      while (true) {
        // 计时器
        val currentTime: Long = System.currentTimeMillis()
        // Log.d("ECode", "Current Time: $currentTime, Expired At: $codeExpiredAt")
        if (currentTime >= codeExpiredAt) {
          try {
            // 获取新的二维码逻辑
            val neu = NEUPass()
            val eCodeSession = getECodeSession(neu, mmkv)
            var eCode: ECodeQRCode? = null
            try {
              eCode = neu.getQRCode(eCodeSession)
              if (userInfo == null) {
                userInfo = neu.getUserInfo(eCodeSession).data[0].attributes
              }
            } catch (e: SessionExpiredException) {
              // session 过期
              val session = neu.newECodeSession()
              mmkv.encode("ecode_session", session.session)
              mmkv.encode("xsrf_token", session.xsrfToken)
              mmkv.encode("expired_at", session.expiredAt)

              // 重新登录
              val eCodeTicket = getECodeTicket(neu, mmkv)
              try {
                neu.loginECode(session, eCodeTicket)
              } catch (e: TicketExpiredException) {
                // 重新获取 ticket
                val portalTicket = getPortalTicket(neu, mmkv, true)
                val newECodeTicket = neu.loginECodeTicket(portalTicket)
                mmkv.encode("ecode_ticket", newECodeTicket)
                neu.loginECode(session, newECodeTicket)
              }
              eCode = neu.getQRCode(session)
              if (userInfo == null) {
                userInfo = neu.getUserInfo(eCodeSession).data[0].attributes
              }
            }

            showCode = true
            code = eCode!!.data[0].attributes.qrCode
            codeExpiredAt = eCode.data[0].attributes.qrInvalidTime
          } catch (e: Exception) {
            // 错误处理逻辑
            e.printStackTrace()
          }
        }

        // 每秒检查一次
        delay(1000L)
      }
    }
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
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
          modifier = Modifier.height(48.dp),
          contentDescription = "NEU ECode",
          contentScale = ContentScale.FillHeight
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text("一码通", style = MaterialTheme.typography.headlineLarge)
      }
      Spacer(modifier = Modifier.height(32.dp))
      if (showCode) {
        ECodeImage(code)
      } else {
        ECodeLoading()
      }
      Spacer(modifier = Modifier.height(32.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (userInfo != null) {
            "${userInfo?.userCode} | ${userInfo?.userName} | ${userInfo?.unitName}"
          } else {
            "加载中..."
          },
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }
  }
}

@Composable
fun ECodeLoading() {
  Box(
    modifier = Modifier
      .size(300.dp)
      .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
fun ECodeImage(code: String) {
  Box(
    modifier = Modifier
      .size(300.dp)
      .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center
  ) {
    val customFont = FontFamily(Font(R.font.iconfont))
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    generateColoredQRCode(
      code,
      dpToPx(300.dp),
      primaryColor.toArgb(),
      backgroundColor.toArgb()
    ).let {
      Image(
        bitmap = it!!.asImageBitmap(),
        contentDescription = "ECode",
        modifier = Modifier.size(300.dp),
        contentScale = ContentScale.FillWidth
      )
    }

    Text(
      text = "●",
      color = backgroundColor,
      fontFamily = customFont,
      fontSize = 70.sp,
    )

    Text(
      text = "\uE6BE",
      color = primaryColor,
      fontFamily = customFont,
      fontSize = 48.sp,
    )
  }
}

// Generated by ChatGPT

@Composable
fun dpToPx(dp: Dp): Int {
  val density = LocalDensity.current
  return with(density) { dp.toPx().toInt() }
}

fun generateColoredQRCode(
  text: String,
  size: Int,
  foregroundColor: Int,
  backgroundColor: Int
): Bitmap? {
  try {
    val qrCodeWriter = QRCodeWriter()
    val hints = mapOf(
      EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
      EncodeHintType.MARGIN to 1
    )
    val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
      for (y in 0 until height) {
        bitmap.setPixel(
          x,
          y,
          if (bitMatrix[x, y]) foregroundColor else backgroundColor
        )
      }
    }
    return bitmap
  } catch (e: WriterException) {
    e.printStackTrace()
    return null
  }
}

