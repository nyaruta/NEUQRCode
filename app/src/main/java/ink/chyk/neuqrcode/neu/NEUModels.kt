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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class PersonalResponse<T>(
  val e: Int,  // code
  val m: String,  // message
  val d: T  // data
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
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