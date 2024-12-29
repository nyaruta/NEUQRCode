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
