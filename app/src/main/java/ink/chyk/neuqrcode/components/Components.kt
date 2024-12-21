package ink.chyk.neuqrcode.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ink.chyk.neuqrcode.ui.theme.AppTheme

@Composable
fun AppBackground(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  AppTheme {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            brush = Brush.radialGradient(
              colors = if (darkTheme) {
                listOf(Color(0xFF1A1F35), Color(0xFF202020))
              } else {
                listOf(Color(0xFFEFF4F9), Color(0xFFF3F3F3))
              },
              center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f), // 中心点
              radius = Float.POSITIVE_INFINITY // 使用最大半径来模拟渐变效果
            )
          )
      ) {
        Box(
          modifier = Modifier
            .padding(innerPadding)
        ) {
          content()
        }
      }
    }
  }
}