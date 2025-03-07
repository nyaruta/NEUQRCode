package ink.chyk.neuqrcode.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*

class AppsViewModelFactory(
  private val onFailed: () -> Boolean
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
      val neu = NEUPass(onFailed)
      val mmkv = MMKV.defaultMMKV()
      return AppsViewModel(
        neu,
        mmkv,
        CampusRun(neu, mmkv)
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}