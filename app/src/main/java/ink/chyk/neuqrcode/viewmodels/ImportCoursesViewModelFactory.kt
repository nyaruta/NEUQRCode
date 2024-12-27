package ink.chyk.neuqrcode.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*

class ImportCoursesViewModelFactory() : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ImportCoursesViewModel::class.java)) {
      return ImportCoursesViewModel(
        MMKV.defaultMMKV()
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}