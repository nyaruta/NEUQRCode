package ink.chyk.neuqrcode.neu

import kotlinx.serialization.Serializable


@Serializable
data class NEUAppSession(
  val session: String,
  val xsrfToken: String,
  val expiredAt: Long
)

@Serializable
data class ListedResponse<T>(
  val data: List<ResponseData<T>>
)

@Serializable
data class ResponseData<T>(
  val type: Nothing? = null,
  val attributes: T
)

@Serializable
data class ECodeResponse(
  val qrCode: String,
  val createTime: Long,
  val qrInvalidTime: Long
)

@Serializable
data class ECodeUserInfoResponse(
  val userCode: String,
  val userName: String,
  val unitName: String,
  val idType: String,
)

@Serializable
data class MobileApiUser(
  val data: MobileApiUserData
)

@Serializable
data class MobileApiUserData(
  val id: String,  // 请求 id
  val type: String,
  val attributes: MobileApiUserAttributes
)

@Serializable
data class MobileApiUserAttributes(
  val code: String,  // 学号
  val name: String,  // 姓名
  val avatar: String, // 大头，base64 编码的 datauri
  val remark: String? = null,  //
  val credits: String,
  val createTime: String,
  val updateTime: String,
  val isDeleted: Int
)

@Serializable
data class MobileApiUserInfo(
  val DH: String,  // 学号
  val XM: String,  // 姓名
  val RYLX: String,  // 人员类型
  val ZZJG: String,  // 组织机构
  val ZZJGBH: String,  // 组织机构编号
  val XB: String,  // 性别
  val EMAIL: String,  // 邮箱地址 neu.edu.cn 结尾
)

@Serializable
data class MobileApiCampusCard(
  val createUser: Int,
  val createTime: String,
  val updateUser: Int,
  val updateTime: String,
  val status: Int,
  val isDeleted: Int,
  val balance: String,
  val supply: String,
  val jumpUrl: String
)

@Serializable
data class MobileApiCampusNetwork(
  val createUser: Int,
  val createTime: String,
  val updateUser: Int,
  val updateTime: String,
  val status: Int,
  val isDeleted: Int,
  val balance: String,
  val usedData: String,
  val jumpUrl: String
)

@Serializable
data class MessageSourcesResponse(
  val code: Int,
  val success: Boolean,
  val data: List<MessageSource>,
  val msg: String
)

@Serializable
data class MessageSource(
  val id: String,
  val name: String,
  val noticeCount: Int
)

@Serializable
data class PagedResponse<T : PagedResponseItem>(
  val data: List<T>,
  val meta: Meta
)

@Serializable
data class Meta(
  val totalResourceCount: Int,
)

interface PagedResponseItem {
  var idx: Int?
}

@Serializable
data class Task(
  val id: String,
  val type: String,
  val attributes: TaskAttributes,
  override var idx: Int? = 0
) : PagedResponseItem


@Serializable
data class TaskAttributes(
  val sourceId: String,
  val source: String? = null,
  val title: String,
  val url: String,
  val userCode: String,
  val clientId: String,
  val bizId: String,
  val status: Int,
  val startTime: String,
  val finishTime: String,
  val createTime: String,
  val updateTime: String,
  val isDeleted: Int,
)

@Serializable
data class Notification(
  val id: String,
  val type: String,
  val attributes: NotificationAttributes,
  val relationships: NotificationRelationships,
  override var idx: Int? = 0
) : PagedResponseItem

@Serializable
data class NotificationAttributes(
  val sourceId: String,
  val userCode: String,
  val clientId: String,
  val data: String,
  val status: Int,
  val createTime: String,
  val createUser: String,
  val updateTime: String,
  val updateUser: String,
  val isDeleted: Int
)

@Serializable
data class NotificationRelationships(
  val source: NotificationRelationshipsSource
)

@Serializable
data class NotificationRelationshipsSource(
  val data: NotificationRelationshipsSourceData
)

@Serializable
data class NotificationRelationshipsSourceData(
  val id: String,
  val type: String
)

@Serializable
data class Article(
  val id: String,
  val type: String,
  val attributes: ArticleAttributes,
  override var idx: Int? = 0
) : PagedResponseItem

@Serializable
data class ArticleAttributes(
  val title: String,
  val wbnewsid: String,
  val wbdate: String,
  val wbcontenturl: String,
  val ownerid: String? = null,
  val contentid: String? = null,
  val isDefaultRead: String,
  val wbupdatedate: String? = null,
  val defaultLink: String? = null,
  val treeid: String? = null,
  val indexStatus: String,
  val indexTime: String,
  val createUser: String? = null,
  val createTime: String? = null,
  val updateUser: String? = null,
  val updateTime: String? = null,
  val status: String? = null,
  val isDeleted: Int
)