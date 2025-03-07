package ink.chyk.neuqrcode.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import androidx.navigation.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.viewmodels.*

@Composable
fun AppsScreen(
  viewModel: AppsViewModel,
  @Suppress("UNUSED_PARAMETER")
  navController: NavController,
  innerPadding: PaddingValues
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
  ) {
    Column (
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
    ) {
      AppsHeader()
      Box {}
    }
  }
}

@Composable
fun AppsHeader() {
  // 顶部间距和标题
  Spacer(modifier = Modifier.height(32.dp))
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.apps_center),
      style = MaterialTheme.typography.headlineLarge,
    )
  }
  Spacer(modifier = Modifier.height(16.dp))
}