package ink.chyk.neuqrcode.screens

import android.content.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.navigation.*
import ink.chyk.neuqrcode.*


@Composable
fun CoursesScreen(navController: NavController) {
  val context = LocalContext.current
  Button(onClick = {
    val intent = Intent(context, ImportCoursesActivity::class.java)
    context.startActivity(intent)
  }) { Text("导入课表") }
}