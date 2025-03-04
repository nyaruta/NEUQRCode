package ink.chyk.neuqrcode.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*

class ECodeViewModelFactory(
  private val onFailed: () -> Boolean
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ECodeViewModel::class.java)) {
      return ECodeViewModel(
        MMKV.defaultMMKV(),
        NEUPass(onFailed)
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}