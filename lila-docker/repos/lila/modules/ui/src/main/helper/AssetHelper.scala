package lila.ui

import lila.ui.ScalatagsTemplate.*
import lila.core.data.Url

trait AssetHelper:
  def assetUrl(path: String): String = s"/assets/$path"
  def assetVersion: String = "1"
  def safeJsonValue(v: play.api.libs.json.JsValue): lila.core.data.SafeJsonStr = 
    lila.core.data.SafeJsonStr(v.toString)
  def imagePreload(url: Option[Url]): Frag = 
    url.fold(emptyFrag)(u => link(rel := "preload", href := u.value, attr("as") := "image"))
  
  // Stub methods for layout.scala
  def staticAssetUrl(path: String): String = s"/assets/$path"
  def staticCompiledUrl(path: String): String = s"/assets/compiled/$path"
  def assetBaseUrl: String = ""
  def netBaseUrl: String = ""
  def embedJsUnsafe(code: String)(nonce: Option[Nonce]): Frag = 
    nonce.fold(script(raw(code)))(n => script(attr("nonce") := n.value, raw(code)))
  def preload(url: String, as: String, crossOrigin: Boolean, tpe: Option[String]): Frag =
    link(rel := "preload", href := url, attr("as") := as, tpe.map(t => attr("type") := t))
  def esmInit(key: String, name: String): Frag = raw("")
  def iifeModule(path: String): Frag = script(src := assetUrl(path))
