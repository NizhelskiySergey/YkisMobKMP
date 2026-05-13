package com.ykis.ykismobkmp.data.responses

// Интерфейс не несет в себе полей для сериализации, только контракт
interface BaseResponse {
  val success: Int
  val message: String
}
