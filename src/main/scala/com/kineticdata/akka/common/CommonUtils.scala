package com.kineticdata.akka.common

import play.api.libs.json.{JsString, JsValue}

object CommonUtils {
  def getJsonString(json: JsValue, field: String, default: String = ""): String = {
    val ret = (json \ field).getOrElse(JsString(default)).as[String]
    ret
  }
}