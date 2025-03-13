package ink.chyk.neuqrcode

import android.content.Context

class Utilities {
  // 此类存放一些可复用的工具方法

  companion object {
    fun isDebug(context: Context): Boolean {
      // 判断是否是 Debug 构建
      val packageInfo =
        context.packageManager.getPackageInfo(context.packageName, 0)
      val versionName = packageInfo.versionName

      return isDebug(versionName)
    }

    fun isDebug(versionName: String?): Boolean {
      return (versionName?.substringAfterLast("-")?.length == 8) or (versionName?.contains("beta") == true)
    }
  }
}