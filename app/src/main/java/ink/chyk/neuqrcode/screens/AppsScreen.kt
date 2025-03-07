package ink.chyk.neuqrcode.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
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
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(16.dp),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
    ) {
      AppsHeader()
      Box {}
      CampusRunCard(viewModel)
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

@Composable
fun CampusRunCard(
  viewModel: AppsViewModel,
) {
  LaunchedEffect(Unit) {
    viewModel.initCampusRun()
    // 加载步道乐跑数据
  }

  val context = LocalContext.current
  val loadingState = viewModel.campusRunState.collectAsState()
  val termName = viewModel.termName.collectAsState()

  Card {
    Column(
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth(),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_fluent_run_48_regular),
          contentDescription = null,
          modifier = Modifier.size(48.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(stringResource(R.string.campus_run), fontSize = 20.sp, fontWeight = FontWeight.Bold)
          Text(
            when (loadingState.value) {
              AppsViewModel.LoadingState.LOADING -> {
                stringResource(R.string.loading)
              }

              AppsViewModel.LoadingState.SUCCESS -> {
                termName.value
              }

              AppsViewModel.LoadingState.FAILED -> {
                stringResource(R.string.no_network)
              }
            },
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        IconButton(
          onClick = {
            // 进入乐跑
            viewModel.startCampusRun(context)
          },
          colors = IconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_golang),
            contentDescription = null,
            modifier = Modifier.height(32.dp),
          )
        }
      }
    }
  }
}