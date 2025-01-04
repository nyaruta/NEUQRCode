package ink.chyk.neuqrcode.screens

import android.net.*
import android.util.*
import android.widget.*
import androidx.browser.customtabs.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.paging.*
import androidx.paging.compose.*
import dev.darkokoa.pangu.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.R
import ink.chyk.neuqrcode.neu.*
import ink.chyk.neuqrcode.viewmodels.*

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

  WithTopBar(viewModel) {
    MessageList(it)
  }

  val detailState by viewModel.detailState.collectAsState()

  if (detailState.showDetail) {
    NotificationDialog(
      notification = detailState.detail!!,
      source = detailState.source!!,
      onDismiss = {
        viewModel.hideDetail()
      }
    )
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
  // 左侧抽屉
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

  val notifications = viewModel.notifications?.collectAsLazyPagingItems()
  val tasks = viewModel.tasks?.collectAsLazyPagingItems()
  val neu1Articles = viewModel.neu1Articles?.collectAsLazyPagingItems()
  val neu2Articles = viewModel.neu2Articles?.collectAsLazyPagingItems()
  val neu3Articles = viewModel.neu3Articles?.collectAsLazyPagingItems()

  val category by viewModel.category.collectAsState()

  val pagingItems = when (category) {
    Category.NOTICE -> notifications
    Category.TASKS -> tasks
    Category.NEU1 -> neu1Articles
    Category.NEU2 -> neu2Articles
    Category.NEU3 -> neu3Articles
  }!!

  val ctx = LocalContext.current

  when {
    pagingItems.loadState.refresh is LoadState.Loading -> {
      // 显示加载动画
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }

    pagingItems.loadState.refresh is LoadState.Error -> {
      // 显示错误信息
      val error = (pagingItems.loadState.refresh as LoadState.Error).error
      NekoPlaceholder("加载失败: ${error.message}")

    }

    pagingItems.loadState.refresh is LoadState.NotLoading && pagingItems.itemCount == 0 -> {
      // 显示 Placeholder
      NekoPlaceholder(stringResource(R.string.no_message_in_category))
    }

    else -> {
      // 显示列表
      LazyColumn {
        items(
          pagingItems.itemCount,
          key = pagingItems.itemKey { it.idx ?: 0 }
        ) {
          when (category) {
            Category.NEU1, Category.NEU2, Category.NEU3 -> {
              val article = pagingItems[it] as Article
              MessageCard(
                message = article.attributes.title,
                source = article.attributes.indexTime,
                onClick = {
                  val intent: CustomTabsIntent = CustomTabsIntent.Builder()
                    .setStartAnimations(ctx, R.anim.slide_in_right, R.anim.slide_out_left)
                    .setExitAnimations(ctx, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .build()
                  val uri = Uri.parse(article.attributes.wbcontenturl)
                  intent.launchUrl(ctx, uri)
                },
                modifier = Modifier.animateItem()
              )
            }

            Category.NOTICE -> {
              val notification = pagingItems[it] as Notification
              val source = sources.first { source -> source.id == notification.attributes.sourceId }
              MessageCard(
                message = notification.attributes.data,
                source = source.name,
                onClick = {
                  viewModel.showDetail(notification, source)
                },
                isDone = notification.attributes.status == 0,
                modifier = Modifier.animateItem()
              )
            }

            Category.TASKS -> {
              val task = pagingItems[it] as Task
              MessageCard(
                message = task.attributes.title,
                source = task.attributes.finishTime,
                onClick = {
                  /* TODO: webview */
                  Toast.makeText(ctx, "当前版本尚未支持 WebView。\n请前往官方 App 使用此功能。", Toast.LENGTH_SHORT).show()
                },
                isDone = task.attributes.status == 1,
                modifier = Modifier.animateItem()
              )
            }
          }
        }

        pagingItems.apply {
          when (loadState.append) {
            is LoadState.Loading -> {
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center
                ) {
                  CircularProgressIndicator()
                }
              }
            }

            is LoadState.Error -> {
              item {
                val error = (loadState.append as LoadState.Error).error
                Text(
                  text = "加载更多失败: ${error.message}",
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.Center,
                  color = Color.Red
                )
              }
            }

            else -> Unit
          }
        }
      }
    }
  }


}

@Composable
fun NekoPlaceholder(
  text: String
) {
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
        text,
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
  isDone: Boolean = false,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
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

@Composable
fun NotificationDialog(
  notification: Notification,
  source: MessageSource,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      DialogTitle(
        icon = R.drawable.ic_fluent_alert_24_regular,
        text = stringResource(R.string.message_detail)
      )
    },
    text = {
      Column {
        Row {
          Icon(
            painter = painterResource(R.drawable.ic_fluent_person_20_regular),
            contentDescription = "Source",
          )
          Text(
            text = source.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = Pangu.spacingText(notification.attributes.data)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss
      ) {
        Text(stringResource(R.string.confirm))
      }
    }
  )
}