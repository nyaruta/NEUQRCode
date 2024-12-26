package ink.chyk.neuqrcode

import android.content.*
import android.os.*
import android.widget.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.res.*
import androidx.lifecycle.viewmodel.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.components.*
import ink.chyk.neuqrcode.screens.*
import ink.chyk.neuqrcode.viewmodels.*

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
      MainApp()
    }
  }
}


data class BottomNavigationItem(
  val label: String = "",
  val icon: ImageVector = Icons.Default.Home,
  val route: String = ""
) {
  @Composable
  fun navigationItems(): List<BottomNavigationItem> {
    return listOf(
      BottomNavigationItem(
        "一码通",
        ImageVector.vectorResource(
          id = R.drawable.ic_fluent_qr_code_24_filled
        ),
        "ecode"
      ),
      BottomNavigationItem(
        "个人信息",
        ImageVector.vectorResource(
          id = R.drawable.ic_fluent_person_24_filled
        ),
        "profile"
      )
    )
  }
}

@Composable
fun MainApp() {
  val eCodeViewModel: ECodeViewModel = viewModel(factory = ECodeViewModelFactory())
  val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())

  val navController = rememberNavController()
  var selectedItem by remember { mutableIntStateOf(0) }

  AppBackground(
    bottomBar = {
      NavigationBar {
        BottomNavigationItem().navigationItems().forEachIndexed { index, item ->
          NavigationBarItem(
            selected = index == selectedItem,
            label = { Text(item.label) },
            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
            onClick = {
              selectedItem = index
              navController.navigate(item.route) {
                // https://medium.com/@bharadwaj.rns/bottom-navigation-in-jetpack-compose-using-material3-c153ccbf0593
                popUpTo(navController.graph.findStartDestination().id) {
                  saveState = true
                }
                launchSingleTop = true
                restoreState = true
              }
            }
          )
        }
      }
    }
  ) {
    NavHost(navController = navController, startDestination = "ecode") {
      composable("ecode") {
        ECodeScreen(viewModel = eCodeViewModel, navController = navController)
      }
      composable("profile") {
        ProfileScreen(viewModel = profileViewModel, navController = navController)
      }
    }
  }
}