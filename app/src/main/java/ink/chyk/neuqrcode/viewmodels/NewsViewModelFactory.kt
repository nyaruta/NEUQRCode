package ink.chyk.neuqrcode.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*

class NewsViewModelFactory() : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
      return NewsViewModel(
        MMKV.defaultMMKV(),
        NEUPass()
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}