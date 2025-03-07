package ink.chyk.neuqrcode.neu

import kotlinx.serialization.*
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

@Serializable
@ExperimentalSerializationApi
@JsonIgnoreUnknownKeys  // 别的一律用不到
data class BeforeRunResponse(
  val run_zone_name: String,
  val run_notes: List<String>,
  val weekData: WeekData,
  val today_run_user_num: Int,
)

@Serializable
data class WeekData(
  val total: String,
  val list: List<WeekDataSingle>,
)

@Serializable
data class WeekDataSingle(
  val date: String,
  val distance: String,
)

@Serializable
data class GetTermRunRecordResponse(
  val total_num: String,
  val total_score_num: String,
  val total_distance: String,
  val total_score_distance: String,
  val list: List<JsonObject> = emptyList(),
)