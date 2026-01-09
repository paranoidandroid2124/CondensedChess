package lila.core.i18n

import play.api.i18n.Lang

opaque type Translator = Unit
object Translator extends (Lang => (String => String)):
  def apply(lang: Lang): String => String = identity
  def defaultLang = Lang("en")
  val empty: Translator = ()

trait I18nModule:
  def translator: Translator = Translator.empty
  def defaultLang: Lang = Translator.defaultLang
  def keys: lila.i18n.I18nKeys = lila.i18n.I18nKeys

object I18nModule:
  type Selector = lila.i18n.I18nKeys => Any

object LangPicker:
  def apply(_req: play.api.mvc.RequestHeader): Lang = Translator.defaultLang
  def byHref(_lang: scalalib.model.Language, _req: play.api.mvc.RequestHeader): ByHref = ByHref.NotFound
  
  enum ByHref:
    case NotFound
    case Redir(code: String)
    case Found(lang: Lang)
    case Refused(lang: Lang)

case class Translate(translator: Translator, lang: Lang):
  def apply(key: String, _args: Any*): String = key
  def exists(key: String): Boolean = true
  def noTrans(key: String): String = key

object LangList:
  def popularAlternateLanguages = List.empty[Lang]
  def all = List.empty[Lang]
  def sorted = List.empty[Lang]
  def name(lang: Lang) = lang.language
  def name(code: String) = code
