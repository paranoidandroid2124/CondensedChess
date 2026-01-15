package views.base

import lila.app.UiEnv.{ *, given }
import lila.ui.ContentSecurityPolicy

object embed:
  /* a minimalist embed that doesn't load site.ts */
  def minimal(title: String, cssKeys: List[String] = Nil, modules: EsmList = Nil)(body: Modifier*)(using
      ctx: EmbedContext
  ) = lila.ui.Snippet:
    frag(
      page.ui.doctype,
      page.ui.htmlTag(
        cls := ctx.bg,
        head(
          page.ui.charset,
          page.ui.viewport,
          page.ui.metaCsp(embedCsp.withNonce(ctx.nonce).withInlineIconFont),
          st.headTitle(title),
          (ctx.bg == "system").option(page.ui.systemThemeScript(ctx.nonce.some)),
          page.pieceSetImages.load(ctx.pieceSet.name),
          cssTag("lib.theme.embed"),
          cssKeys.map(cssTag),
          page.ui.scriptsPreload(modules.flatMap(_.map(_.key)))
        ),
        st.body(
          bodyModifiers,
          body,
          page.ui.inlineJs(ctx.nonce),
          page.ui.modulesInit(modules, ctx.nonce.some)
        )
      )
    )

  private def bodyModifiers(using ctx: EmbedContext) = List(
    cls := List("simple-board" -> ctx.pref.simpleBoard),
    page.ui.dataSoundSet := lila.pref.SoundSet.silent.key,
    page.ui.dataAssetUrl,
    page.ui.dataAssetVersion := assetVersion,
    page.ui.dataTheme := ctx.bg,
    page.ui.dataPieceSet := ctx.pieceSet.name,
    page.ui.dataBoard := ctx.boardClass,
    page.ui.dataSocketDomains,
    style := page.boardStyle(zoomable = false)(using ctx.ctx)
  )

  /* a heavier embed that loads site.ts and connects to WS */
  def site(
      title: String,
      cssKeys: List[String] = Nil,
      modules: EsmList = Nil,
      pageModule: Option[PageModule] = None,
      csp: Update[ContentSecurityPolicy] = identity,
  )(body: Modifier*)(using ctx: EmbedContext) = lila.ui.Snippet:
    val allModules: EsmList = modules ++ pageModule.fold(Nil)(module => esmPage(module.name))
    frag(
      page.ui.doctype,
      page.ui.htmlTag(
        cls := ctx.bg,
        head(
          page.ui.charset,
          page.ui.viewport,
          page.ui.metaCsp(csp(basicCsp.withNonce(ctx.nonce).withInlineIconFont)),
          st.headTitle(title),
          (ctx.bg == "system").option(page.ui.systemThemeScript(ctx.nonce.some)),
          page.pieceSetImages.load(ctx.pieceSet.name),
          cssTag("lib.theme.embed"),
          cssKeys.map(cssTag),
          page.ui.sitePreload(allModules),
          page.ui.chesstoryFontFaceCss
        ),
        st.body(bodyModifiers)(
          body,
          page.ui.modulesInit(allModules, ctx.nonce.some),
          pageModule.map { mod => frag(jsonScript(mod.data)) }
        )
      )
    )
