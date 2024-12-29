package ink.chyk.neuqrcode

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import ink.chyk.neuqrcode.ui.theme.AppTheme

@Composable
fun NEUTitle(
  text: String
) {
  // 统一身份认证标题
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .padding(vertical = 16.dp, horizontal = 8.dp),
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
      contentDescription = "NEU Logo",
    )
    Text(text, style = MaterialTheme.typography.headlineSmall)
  }
}