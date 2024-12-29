package ink.chyk.neuqrcode.screens

import android.util.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.viewmodels.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// https://developer.android.com/develop/ui/compose/components/app-bars?hl=zh-cn#center
fun WithTopBar(
  viewModel: NewsViewModel,
  content: @Composable (NewsViewModel) -> Unit = {}
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
  val rail = viewModel.rail.collectAsState()
  val category = viewModel.category.collectAsState()

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

    topBar = {
      CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        title = {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 14.sp)) {
              Text(
                text = stringResource(R.string.message_center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            DrawerItem.items.filter { it.category == category.value }.forEach {
              Row(
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  painter = painterResource(it.icon),
                  contentDescription = "Category Icon",
                  tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 20.sp)) {
                  Text(
                    stringResource(it.label),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 4.dp)
                  )
                }
              }
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = {
            viewModel.toggleRail()
          }) {
            Icon(
              painter = painterResource(R.drawable.ic_fluent_list_24_filled),
              contentDescription = "Categories"
            )
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      RailedDrawer(rail, category, viewModel)
      Box(
        modifier = Modifier
          .fillMaxSize(),
      ) {
        content(viewModel)
      }
    }
  }
}

@Composable
fun NewsScreen(viewModel: NewsViewModel, navController: NavController) {
  val loadComplete by viewModel.loadComplete.collectAsState()
  LaunchedEffect(Unit) {
    viewModel.fetchContents()
  }

  if (!loadComplete) {
    return WithTopBar(viewModel) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }
  }

  WithTopBar(viewModel) { it ->
    MessageList(it)
  }
}

data class DrawerItem(
  val label: Int,
  val icon: Int,
  val category: Category
) {
  companion object {
    val items = listOf(
      DrawerItem(
        R.string.unread, R.drawable.ic_fluent_channel_24_regular, Category.UNREAD
      ),
      DrawerItem(
        R.string.notice, R.drawable.ic_fluent_alert_24_regular, Category.NOTICE
      ),
      DrawerItem(
        R.string.tasks,
        R.drawable.ic_fluent_clipboard_task_list_24_regular_ltr,
        Category.TASKS
      ),
      DrawerItem(R.string.neu1, R.drawable.ic_fluent_megaphone_loud_24_regular, Category.NEU1),
      DrawerItem(R.string.neu2, R.drawable.ic_fluent_megaphone_24_regular, Category.NEU2),
      DrawerItem(R.string.neu3, R.drawable.ic_fluent_content_view_24_regular, Category.NEU3),
    )
  }
}

@Composable
fun RailedDrawer(
  rail: State<Boolean>,
  category: State<Category>,
  viewModel: NewsViewModel
) {
  Box(
    modifier = Modifier
      .padding(vertical = 8.dp)
      .fillMaxHeight()
  ) {
    Column {
      DrawerItem.items.forEach { item ->
        // 动画背景颜色
        val backgroundColor by animateColorAsState(
          targetValue = if (category.value == item.category)
            MaterialTheme.colorScheme.secondaryContainer
          else
            Color.Transparent
        )

        // 动画宽度
        val boxWidth by animateDpAsState(
          targetValue = if (rail.value) 120.dp else 40.dp,
        )

        Box(
          modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor) // 使用动画背景颜色
            .width(boxWidth) // 添加动画宽度
            .height(40.dp)
            .clickable {
              viewModel.setCategory(item.category)
              if (rail.value) {
                viewModel.toggleRail()
              }
            }
        ) {
          Row(
            modifier = Modifier
              .padding(8.dp)
              .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AnimatedVisibility(
              visible = true,
              enter = fadeIn(),
              exit = fadeOut()
            ) {
              Icon(
                painter = painterResource(item.icon),
                contentDescription = "Category Icon",
              )
            }
            AnimatedVisibility(
              visible = rail.value,
              enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
              exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
              Text(
                text = stringResource(item.label),
                modifier = Modifier.padding(start = 8.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MessageList(viewModel: NewsViewModel) {
  val sources by viewModel.sources.collectAsState()
  val notifications by viewModel.notifications.collectAsState()
  val tasks by viewModel.tasks.collectAsState()
  val category by viewModel.category.collectAsState()

  var empty by remember { mutableStateOf(true) }

  Column {
    notifications.filter {
      when (category) {
        Category.UNREAD -> it.attributes.status == 1
        Category.NOTICE -> true
        else -> false
      }
    }.forEach {
      empty = false
      MessageCard(
        message = it.attributes.data,
        source = sources.first { source -> source.id == it.attributes.sourceId }.name,
        onClick = {},
        isDone = it.attributes.status == 0
      )
    }
    tasks.filter {
      when (category) {
        Category.UNREAD -> it.attributes.status == 0
        Category.TASKS -> true
        else -> false
      }
    }.forEach {
      empty = false
      MessageCard(
        message = it.attributes.title,
        source = it.attributes.finishTime,
        onClick = {},
        // TODO: open url with session in webview
        // will implement after embedding webview
        isDone = it.attributes.status == 1
      )
    }
  }

  if (empty) {
    NekoPlaceholder()
  }
}

@Composable
fun NekoPlaceholder() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .padding(48.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(
          listOf(R.drawable.neko1, R.drawable.neko2, R.drawable.neko3).random()
        ),
        contentDescription = "Neko Placeholder",
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        stringResource(R.string.no_message_in_category),
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun MessageCard(
  message: String,
  source: String,
  onClick: () -> Unit,
  isDone: Boolean = false
) {
  Box(
    modifier = Modifier
      .padding(4.dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(4.dp)
    ) {
      Column {
        Text(
          message,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = if (isDone) Color.Gray else MaterialTheme.colorScheme.onBackground,
          style = TextStyle(
            fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold,
            fontSize = 16.sp
          )
        )
        Text(
          source,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
          color = if (isDone) Color.Gray else MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}