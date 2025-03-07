package ink.chyk.neuqrcode.neu

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class CampusRunResponse(
  val data: String,
  val status: Int,
  val info: String,
  val is_encrypt: Int,
)

@Serializable
data class WpIndexResponse(
  val uid: Int,
  val school_id: Int,
  val school_name: String,
  val name: String,
  val number: String,  // 并非 Number
  val role: Int,
  val term_name: String,
  val module_list: List<JsonObject>,
  val school_notice: JsonObject,
  val reading_list: List<JsonObject>,
  val whiteNavs: List<String>,
)