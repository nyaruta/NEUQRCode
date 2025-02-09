package ink.chyk.neuqrcode.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*

class EAMSViewModelFactory : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(EAMSViewModel::class.java)) {
      return EAMSViewModel(
        MMKV.defaultMMKV(),
        NEUPass()
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}