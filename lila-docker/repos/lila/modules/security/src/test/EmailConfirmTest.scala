package lila.security

import play.api.libs.typedmap.TypedMap
import play.api.mvc.{ Cookie, Cookies, Headers, RequestHeader }
import play.api.mvc.request.{ RemoteConnection, RequestTarget }

import lila.core.config.Secret
import lila.core.email.EmailAddress
import lila.core.userId.UserId

class EmailConfirmTest extends munit.FunSuite:

  private final case class TestRequestHeader(
      target: RequestTarget,
      headers: Headers,
      cookieJar: Cookies,
      attrs: TypedMap = TypedMap.empty,
      connection: RemoteConnection = RemoteConnection("127.0.0.1", secure = false, clientCertificateChain = None)
  ) extends RequestHeader:
    val method = "GET"
    val version = "HTTP/1.1"
    override lazy val cookies = cookieJar
    override def withTarget(target: RequestTarget) = copy(target = target)
    override def withHeaders(headers: Headers) = copy(headers = headers)
    override def withAttrs(attrs: TypedMap) = copy(attrs = attrs)
    override def withConnection(connection: RemoteConnection) = copy(connection = connection)

  private def requestWithCookies(cookies: Cookie*): RequestHeader =
    val cookieHeader = cookies.map(c => s"${c.name}=${c.value}").mkString("; ")
    TestRequestHeader(
      target = RequestTarget("/", "/", Map.empty),
      headers = Headers("Cookie" -> cookieHeader),
      cookieJar = Cookies(cookies)
    )

  test("email confirm cookie round-trips pending signup state"):
    EmailConfirm.configure(Secret("email-confirm-secret"), "email_confirm_test", secure = false)

    val cookie = EmailConfirm.cookie.set(UserId("tester01"), EmailAddress("tester@example.com"))
    val req = requestWithCookies(cookie)
    val pending = EmailConfirm.cookie.get(req)

    assertEquals(pending.map(_.userId.value), Some("tester01"))
    assertEquals(pending.map(_.email.value), Some("tester@example.com"))

  test("clear cookie uses configured cookie name"):
    EmailConfirm.configure(Secret("email-confirm-secret"), "email_confirm_test", secure = false)
    assertEquals(EmailConfirm.cookie.clear.name, "email_confirm_test")
