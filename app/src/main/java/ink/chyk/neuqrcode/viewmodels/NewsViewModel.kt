package ink.chyk.neuqrcode.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.neuqrcode.neu.*
import kotlinx.coroutines.flow.*
import androidx.paging.*


enum class Category {
  NOTICE, TASKS, NEU1, NEU2, NEU3
}

data class DetailState(
  val showDetail: Boolean = false,
  val detail: Notification? = null,
  val source: MessageSource? = null
)

abstract class BasePagingSource<T : Any>(
  private val pageSize: Int = 10
) : PagingSource<Int, T>() {

  // 子类需要实现的方法，用于获取分页数据
  abstract suspend fun fetchData(page: Int, loadSize: Int): List<T>

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
    val page = params.key ?: 0
    return try {
      val data = fetchData(page, params.loadSize)
      val nextKey = if (data.isEmpty()) null else page + (params.loadSize / pageSize)
      val prevKey = if (page == 0) null else page - 1
      LoadResult.Page(
        data = data,
        prevKey = prevKey,
        nextKey = nextKey
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, T>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }
}

class ArticlePagingSource(
  private val neu: NEUPass,
  private val session: NEUAppSession,
  category: Category
) : BasePagingSource<Article>(pageSize = 10) {
  // 这里其实是有一点问题的，因为界面顺序里的 category 1 2 3，和实际的 contentId 是不一样的
  // 所以需要做一下转换
  private val contentId = when (category) {
    Category.NEU1 -> 3
    Category.NEU2 -> 1
    Category.NEU3 -> 2
    else -> 0  // 其实只会传入这三种
  }

  override suspend fun fetchData(page: Int, loadSize: Int): List<Article> {
    val response = neu.getArticles(session, contentId, page, loadSize)
    return response.data
  }
}

class NotificationPagingSource(
  private val neu: NEUPass,
  private val session: NEUAppSession
) : BasePagingSource<Notification>(pageSize = 10) {
  override suspend fun fetchData(page: Int, loadSize: Int): List<Notification> {
    val response = neu.getNotifications(session, page, loadSize)
    return response.data
  }
}

class TaskPagingSource(
  private val neu: NEUPass,
  private val session: NEUAppSession
) : BasePagingSource<Task>(pageSize = 10) {
  override suspend fun fetchData(page: Int, loadSize: Int): List<Task> {
    val response = neu.getTasks(session, page, loadSize)
    return response.data
  }
}

class NewsViewModel(
  override val mmkv: MMKV,
  override val neu: NEUPass
) : BasicViewModel(mmkv, neu) {
  override val appName: String = "mobile"  // 同样使用 mobile，和 profile 页面共享登录状态
  override suspend fun newAppTicket(portalTicket: String): String {
    return neu.loginMobileApiTicket(portalTicket)
  }

  override suspend fun newAppSession(): NEUAppSession {
    return neu.newMobileApiSession()
  }

  override suspend fun loginApp(session: NEUAppSession, appTicket: String) {
    return neu.loginMobileApi(session, appTicket)
  }

  private var _rail = MutableStateFlow(false)
  val rail: StateFlow<Boolean> = _rail

  private var _category = MutableStateFlow(Category.NOTICE)
  val category: StateFlow<Category> = _category

  private var _sources = MutableStateFlow(emptyList<MessageSource>())
  val sources: StateFlow<List<MessageSource>> = _sources

  var notifications: Flow<PagingData<Notification>>? = null
  var tasks: Flow<PagingData<Task>>? = null
  var neu1Articles: Flow<PagingData<Article>>? = null
  var neu2Articles: Flow<PagingData<Article>>? = null
  var neu3Articles: Flow<PagingData<Article>>? = null

  // 快速创建 PagingData
  private fun <T : Any> pager(pagingSourceFactory: () -> PagingSource<Int, T>) = Pager(
    config = PagingConfig(pageSize = 10, enablePlaceholders = false),
    pagingSourceFactory = pagingSourceFactory
  ).flow
    .cachedIn(viewModelScope)

  private val _detailState = MutableStateFlow(DetailState())
  val detailState: StateFlow<DetailState> = _detailState

  suspend fun fetchContents() {
    prepareSessionAnd { session ->
      _sources.value = neu.getMessageSources(session).data
      notifications = pager { NotificationPagingSource(neu, session) }
      tasks = pager { TaskPagingSource(neu, session) }
      neu1Articles = pager { ArticlePagingSource(neu, session, Category.NEU1) }
      neu2Articles = pager { ArticlePagingSource(neu, session, Category.NEU2) }
      neu3Articles = pager { ArticlePagingSource(neu, session, Category.NEU3) }
    }
  }

  fun toggleRail() {
    _rail.value = !_rail.value
  }

  fun setCategory(category: Category) {
    _category.value = category
  }

  fun showDetail(notification: Notification, source: MessageSource) {
    _detailState.value = DetailState(
      showDetail = true,
      detail = notification,
      source = source
    )
  }


  fun hideDetail() {
    _detailState.value = DetailState()
  }
}