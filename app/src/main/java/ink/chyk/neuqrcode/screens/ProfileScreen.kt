package ink.chyk.neuqrcode.screens

import android.app.Activity
import android.content.*
import android.content.ClipboardManager
import android.content.pm.*
import android.graphics.BitmapFactory
import android.util.*
import android.widget.Toast
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.core.content.ContextCompat.*
import androidx.navigation.*
import ink.chyk.neuqrcode.*
import ink.chyk.neuqrcode.activities.*
import ink.chyk.neuqrcode.neu.*
import ink.chyk.neuqrcode.viewmodels.*
import ink.chyk.neuqrcode.R
import android.graphics.drawable.Icon as AndroidIcon
import coil3.compose.AsyncImage
import coil3.network.*
import coil3.request.*

fun dataUriToImageBitmap(dataUri: String): ImageBitmap? {
  val base64 = dataUri.substringAfter("base64,")
  val byteArray = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
  return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)?.asImageBitmap()
}

@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  navController: NavController,
  innerPadding: PaddingValues
) {
  val userInfo by viewModel.userInfo.collectAsState()
  val cardBalance by viewModel.cardBalance.collectAsState()
  val netBalance by viewModel.netBalance.collectAsState()
  val mailUnread by viewModel.mailUnread.collectAsState()
  val headers by viewModel.headers.collectAsState()
  val loadComplete by viewModel.loadComplete.collectAsState()

  val showLogoutDialog = remember { mutableStateOf(false) }
  val showAboutDialog = remember { mutableStateOf(false) }
  val showGroupChatDialog = remember { mutableStateOf(false) }
  val showUploadAvatarDialog = remember { mutableStateOf(false) }
  val byteArray = remember { mutableStateOf<ByteArray?>(null) }
  val mimeType = remember { mutableStateOf<String?>(null) }
  val fileName = remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(vertical = 64.dp, horizontal = 8.dp)
  ) {
    Column {
      ProfileHeader(
        userInfo, headers, loadComplete,
        onUploadImage = { _byteArray: ByteArray, _mimeType: String, _fileName: String ->
          showUploadAvatarDialog.value = true
          byteArray.value = _byteArray
          mimeType.value = _mimeType
          fileName.value = _fileName
        }
      )
      Spacer(modifier = Modifier.height(64.dp))
      RowButton(
        iconResource = R.drawable.ic_fluent_contact_card_24_regular,
        text = stringResource(R.string.campus_card),
        content = if (loadComplete) "${stringResource(R.string.campus_card_balance)} ${cardBalance?.valueString} ${cardBalance?.unit}" else null,
        clickable = false
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_desktop_signal_24_regular,
        text = stringResource(R.string.network),
        content = if (loadComplete) "${stringResource(R.string.network_balance)} ${netBalance?.valueString} ${netBalance?.unit}" else null,
        clickable = false
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_mail_24_regular,
        text = stringResource(R.string.email),
        content = if (loadComplete) "${stringResource(R.string.email_unread)} ${mailUnread?.valueString}" else null,
        clickable = true,
        // TODO: 电子邮箱
        onClick = { Toast.makeText(context, "暂未开放", Toast.LENGTH_SHORT).show() }
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_calendar_24_regular,
        text = stringResource(R.string.create_courses_shortcut),
        clickable = true,
        onClick = { createShortcut(context) }
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_person_swap_24_regular,
        text = stringResource(R.string.logout_account),
        clickable = true,
        onClick = { showLogoutDialog.value = true }
      )
      RowButton(
        iconResource = R.drawable.ic_fluent_book_information_24_regular,
        text = stringResource(R.string.about),
        clickable = true,
        onClick = { showAboutDialog.value = true }
      )
    }
  }

  LogoutConfirmationDialog(
    showDialog = showLogoutDialog,
    onConfirm = { viewModel.logout(context) },
    onDismiss = { showLogoutDialog.value = false }
  )

  AboutDialog(
    viewModel = viewModel,
    showDialog = showAboutDialog,
    onDismiss = { showAboutDialog.value = false },
    onGroupChat = {
      showAboutDialog.value = false
      showGroupChatDialog.value = true
    }
  )

  GroupChatDialog(
    showDialog = showGroupChatDialog,
    onDismiss = { showGroupChatDialog.value = false }
  )

  UploadAvatarConfirmDialog(
    showDialog = showUploadAvatarDialog,
    onDismiss = { showUploadAvatarDialog.value = false },
    onConfirm = {
      Toast.makeText(context, context.getString(R.string.uploading), Toast.LENGTH_SHORT).show()
      showUploadAvatarDialog.value = false
      viewModel.uploadAvatar(byteArray.value!!, mimeType.value!!, fileName.value!!, {
      Toast.makeText(context, context.getString(R.string.upload_finished), Toast.LENGTH_LONG).show()
    }) }
  )
}

fun createShortcut(
  context: Context
) {
  val activity = context as Activity
  val shortcutManager = activity.getSystemService(ShortcutManager::class.java)

  // 创建 Intent 指定要启动的 Activity 和携带的参数
  val intent = Intent(activity, MainActivity::class.java).apply {
    action = Intent.ACTION_VIEW
    putExtra("screen", "courses") // 添加参数
  }

  // 创建快捷方式信息
  val shortcut = ShortcutInfo.Builder(activity, "courses_shortcut") // 唯一 ID
    .setShortLabel("课程表") // 显示名称
    .setLongLabel("NEU课程表") // 长名称
    .setIcon(AndroidIcon.createWithResource(activity, R.mipmap.ic_courses)) // 图标
    .setIntent(intent) // 设置 Intent
    .build()

  // 添加快捷方式
  shortcutManager.requestPinShortcut(shortcut, null)

  Toast.makeText(context, "已尝试创建课程表快捷方式\n若未创建请检查权限。", Toast.LENGTH_SHORT)
    .show()
}


@Composable
fun ProfileHeader(
  userInfo: UserInfo?,
  headers: NetworkHeaders?,
  loadComplete: Boolean,
  onUploadImage: (ByteArray, String, String) -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val resolver = context.contentResolver
  // 上传头像选择器
  val pickMedia =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        Log.d("PhotoPicker", "Selected URI: $uri")
        // 上传头像
        resolver.openInputStream(uri).use { stream ->
          val byteArray = stream?.readBytes()
          if (byteArray != null) {
            val mimeType = resolver.getType(uri)!!
            // 生成文件名
            val fileName =
              "avatar${uri.pathSegments.last()}.${mimeType.substringAfter("/")}".replace(
                "jpeg",
                "jpg"
              )
            onUploadImage(byteArray, mimeType, fileName)
          } else {
            Toast.makeText(
              context,
              context.getString(R.string.cannot_read_selected_image),
              Toast.LENGTH_SHORT
            ).show()
          }
        }
      }
    }

  // 大头，学号等
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    if (loadComplete) {
      // 头像
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data("https://personal.neu.edu.cn/portal" + userInfo?.avatar)
          .crossfade(true)
          .apply {
            if (headers != null) {
              httpHeaders(headers)
            }
          }
          .build(),
        contentDescription = "Avatar",
        placeholder = painterResource(R.drawable.neko3),
        modifier = Modifier
          .height(96.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
      )
      Spacer(modifier = Modifier.width(16.dp))
      // 文字
      Column {
        Text(
          text = userInfo?.name ?: "",
          style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "${userInfo?.depart} ${userInfo?.identity}\n学号: ${userInfo?.xgh}\n邮箱: ${userInfo?.email}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.clickable {
            clipboardManager.setText(buildAnnotatedString { userInfo?.xgh })
            Toast.makeText(context, "已复制学号", Toast.LENGTH_SHORT).show()
          }
        )
      }
    } else {
      Box(modifier = Modifier.size(96.dp)) { CircularProgressIndicator() }
    }
  }
}


@Composable
fun RowButton(
  iconResource: Int,
  text: String,
  content: String? = null,
  clickable: Boolean = false,
  onClick: () -> Unit = {}
) {
  var rowModifier = Modifier
    .fillMaxWidth()
    .height(48.dp)
    .padding(horizontal = 16.dp)
    .clip(RoundedCornerShape(8.dp))
  if (clickable) {
    rowModifier = rowModifier.clickable(onClick = onClick)
  }
  Row(
    modifier = rowModifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row {
      Icon(
        painter = painterResource(iconResource),
        contentDescription = text
      )
      Spacer(modifier = Modifier.width(16.dp))
      Text(text)
      if (content != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(content, color = MaterialTheme.colorScheme.secondary)
      }
    }
    if (clickable) {
      Icon(
        painter = painterResource(R.drawable.ic_fluent_chevron_right_20_filled),
        contentDescription = "前往"
      )
    } else {
      Box {}
    }
  }
}

@Composable
fun LogoutConfirmationDialog(
  showDialog: MutableState<Boolean>,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = {
        // 当点击外部区域或返回键时关闭对话框
        onDismiss()
      },
      title = {
        DialogTitle(R.drawable.ic_fluent_person_swap_24_regular, "登出")
      },
      text = {
        Text("确定要登出当前的账号吗？")
      },
      confirmButton = {
        Button(onClick = {
          onConfirm()
          showDialog.value = false // 关闭对话框
        }) {
          Text("确认")
        }
      },
      dismissButton = {
        Button(onClick = {
          onDismiss()
          showDialog.value = false // 关闭对话框
        }) {
          Text("取消")
        }
      }
    )
  }
}


@Composable
fun AboutDialog(
  viewModel: ProfileViewModel,
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
  onGroupChat: () -> Unit = {}
) {
  if (showDialog.value) {
    val packageInfo =
      LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0)
    val versionName = packageInfo.versionName ?: "Development Build"
    val context = LocalContext.current

    Dialog(
      onDismissRequest = onDismiss,
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
        ) {
          DialogTitle(
            R.drawable.ic_fluent_book_information_24_regular,
            stringResource(R.string.about)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.about_content_1),
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.about_version, versionName),
            style = MaterialTheme.typography.bodyMedium
          )


          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${stringResource(R.string.about_copyright_1)}\n${
              stringResource(
                R.string.about_copyright_2
              )
            }",
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          RowButton(
            iconResource = R.drawable.ic_fluent_chat_24_regular,
            text = stringResource(R.string.group_chat),
            clickable = true,
            onClick = onGroupChat
          )
          RowButton(
            iconResource = R.drawable.ic_fluent_code_24_regular,
            text = stringResource(R.string.about_content_source),
            clickable = true,
            onClick = {
              viewModel.openLink(context, R.string.source_url)
              onDismiss()
            }
          )
          RowButton(
            iconResource = R.drawable.ic_fluent_phone_update_checkmark_24_regular,
            text = "检查更新",
            clickable = true,
            onClick = {
              viewModel.checkUpdate(context)
              onDismiss()
            }
          )
        }
      }
    }
  }
}

@Composable
fun GroupChatDialog(
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
) {
  val ctx = LocalContext.current
  val groupNumber = stringResource(R.string.group_number)
  val clipboardManager = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val copied = stringResource(R.string.group_number_copied)

  if (showDialog.value) {
    Dialog(
      onDismissRequest = onDismiss,
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
        ) {
          DialogTitle(
            R.drawable.ic_fluent_chat_24_regular,
            stringResource(R.string.group_chat)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            stringResource(R.string.group_chat_welcome),
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            stringResource(R.string.group_number_content, groupNumber),
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("text", groupNumber))
                Toast.makeText(ctx, copied, Toast.LENGTH_SHORT).show()
              }
            ) {
              Text(stringResource(R.string.group_number_copy))
            }
          }
        }
      }
    }
  }
}

@Composable
fun UploadAvatarConfirmDialog(
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        DialogTitle(
          R.drawable.ic_fluent_person_swap_24_regular,
          stringResource(R.string.upload_avatar)
        )
      },
      text = {
        Text(stringResource(R.string.upload_avatar_confirm))
      },
      confirmButton = {
        Button(onClick = {
          onConfirm()
          showDialog.value = false // 关闭对话框
        }) {
          Text(stringResource(R.string.confirm))
        }
      },
      dismissButton = {
        Button(onClick = {
          onDismiss()
          showDialog.value = false // 关闭对话框
        }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}