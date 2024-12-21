package ink.chyk.neuqrcode

class RequestFailedException : Exception("请求失败")  // 应该不会被用到，只是作为兜底
class PasswordIncorrectException : Exception("密码错误")
class TicketFailedException : Exception("获取 ticket 失败")
class TicketExpiredException : Exception("ticket 过期")
class SessionExpiredException : Exception("session 过期")