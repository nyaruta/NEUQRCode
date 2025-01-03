package ink.chyk.neuqrcode.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import androidx.navigation.*
import ink.chyk.neuqrcode.R

@Composable
fun AppsScreen(navController: NavController) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column (
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Image(
        painter = painterResource(R.drawable.neko2),
        contentDescription = "TODO",
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text("功能仍在开发中，敬请期待")
      Spacer(modifier = Modifier.height(4.dp))
      Text("我知道你很急，但你先别急", style = TextStyle(fontSize = 12.sp, color = Color.Gray))
    }
  }
}