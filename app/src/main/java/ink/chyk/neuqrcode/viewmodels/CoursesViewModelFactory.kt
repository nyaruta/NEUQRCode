package ink.chyk.neuqrcode.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.*

class CoursesViewModelFactory : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(CoursesViewModel::class.java)) {
      return CoursesViewModel(
        MMKV.defaultMMKV()
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}