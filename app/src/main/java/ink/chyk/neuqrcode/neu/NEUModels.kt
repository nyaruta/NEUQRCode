package ink.chyk.neuqrcode.neu

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class UserSSOLoginResponse(
  val code: Int,
  val result: JsonElement, // 直接接收原始 JSON 元素
  val msg: String
) {
  val tgt: String? by lazy {
    when {
      result is JsonObject -> result.jsonObject["tgt"]?.jsonPrimitive?.content
      result is JsonArray && result.isEmpty() -> null
      else -> throw IllegalStateException("Unexpected result format")
    }
  }
}

@Serializable
data class PersonalSession(
  val lc: String,
  val vl: String,
  val sessId: String,
)

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
@Deprecated("Deprecated in 3.x api")
data class MobileApiUser(
  val data: MobileApiUserData
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class MobileApiUserData(
  val id: String,  // 请求 id
  val type: String,
  val attributes: MobileApiUserAttributes
)

@Serializable
@Deprecated("Deprecated in 3.x api")
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
@Deprecated("Deprecated in 3.x api")
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
@Deprecated("Deprecated in 3.x api")
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
@Deprecated("Deprecated in 3.x api")
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
@Deprecated("Deprecated in 3.x api")
data class MessageSourcesResponse(
  val code: Int,
  val success: Boolean,
  val data: List<MessageSource>,
  val msg: String
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class MessageSource(
  val id: String,
  val name: String,
  val noticeCount: Int
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class PagedResponse<T : PagedResponseItem>(
  val data: List<T>,
  val meta: Meta
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class Meta(
  val totalResourceCount: Int,
)

@Deprecated("Deprecated in 3.x api")
interface PagedResponseItem {
  var idx: Int?
}

@Serializable
@Deprecated("Deprecated in 3.x api")
data class Task(
  val id: String,
  val type: String,
  val attributes: TaskAttributes,
  override var idx: Int? = 0
) : PagedResponseItem


@Serializable
@Deprecated("Deprecated in 3.x api")
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
@Deprecated("Deprecated in 3.x api")
data class Notification(
  val id: String,
  val type: String,
  val attributes: NotificationAttributes,
  val relationships: NotificationRelationships,
  override var idx: Int? = 0
) : PagedResponseItem

@Serializable
@Deprecated("Deprecated in 3.x api")
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
@Deprecated("Deprecated in 3.x api")
data class NotificationRelationships(
  val source: NotificationRelationshipsSource
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class NotificationRelationshipsSource(
  val data: NotificationRelationshipsSourceData
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class NotificationRelationshipsSourceData(
  val id: String,
  val type: String
)

@Serializable
@Deprecated("Deprecated in 3.x api")
data class Article(
  val id: String,
  val type: String,
  val attributes: ArticleAttributes,
  override var idx: Int? = 0
) : PagedResponseItem

@Serializable
@Deprecated("Deprecated in 3.x api")
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

@Serializable
data class PersonalResponse<T>(
  val e: Int,  // code
  val m: String,  // message
  val d: T  // data
)

@Serializable
data class UserInfoOuter(
  val info: UserInfo
)

@Serializable
data class UserInfo(
  val uid: String,
  val name: String,
  val xgh: String, // 学工号
  val identity: String,
  val identity_id: String,
  val sex: Int,
  val depart: String,
  val mobile: String,
  val email: String,
  val organ: JsonElement,  // 不明
  val organs: JsonElement,  // 不明
  val avatar: String,  // 其实这个才是真正的 avatar_url
  val avatar_url: String,  // 这个是空字符串，意义不明
  val time: String,
  val is_manager: Boolean
)

@Serializable
data class PersonalDataIdOuter(
  val data: List<PersonalDataId>
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
data class PersonalDataId(
  val key: String,
  val id: String,
  // 只需要这两个字段
)

@Serializable
data class PersonalDataItemOuter(
  val data: PersonalDataItem
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
data class PersonalDataItem(
  val value: JsonElement,
  val unit: String? = null,
  val url: String? = null,
) {
  val valueString: String? by lazy {
    value.jsonPrimitive.content
  }
}