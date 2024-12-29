package ink.chyk.neuqrcode.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*

class NewsViewModel(
  override val mmkv: MMKV,
  override val neu: NEUPass
) : BasicViewModel(mmkv, neu) {
  override val appName: String = "news"
  override suspend fun newAppTicket(portalTicket: String): String {
    TODO("Not yet implemented")
  }

  override suspend fun newAppSession(): NEUAppSession {
    TODO("Not yet implemented")
  }

  override suspend fun loginApp(session: NEUAppSession, appTicket: String) {
    TODO("Not yet implemented")
  }
}