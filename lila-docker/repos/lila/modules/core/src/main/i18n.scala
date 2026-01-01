package lila.core.i18n

import play.api.i18n.Lang

opaque type Translator = Unit
object Translator extends (Lang => (String => String)):
  def apply(lang: Lang): String => String = identity
  def defaultLang = Lang("en")

object LangPicker:
  def apply(req: play.api.mvc.RequestHeader): Lang = Translator.defaultLang
  def byHref(lang: scalalib.model.Language, req: play.api.mvc.RequestHeader): ByHref = ByHref.NotFound
  
  enum ByHref:
    case NotFound
    case Redir(code: String)
    case Found(lang: Lang)
    case Refused(lang: Lang)

case class Translate(translator: Translator, lang: Lang):
  def apply(key: String, args: Any*): String = key
  def exists(key: String): Boolean = true
  def noTrans(key: String): String = key
