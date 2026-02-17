package views.base

import scalalib.StringUtils.escapeHtmlRaw

import lila.app.UiEnv.{ *, given }
import scalalib.model.Language
import lila.common.String.html.safeJsonValue
import lila.ui.{ RenderedPage, PageFlags }

object page:

  val pieceSetImages = lila.web.ui.PieceSetImages(assetHelper)

  val ui = lila.web.ui.layout(helpers, assetHelper)
  import ui.*

  private val topnav = lila.web.ui.TopNav(helpers)

  private def metaThemeColor(using ctx: Context): Frag =
    raw(s"""<meta name="theme-color" content="${ctx.pref.themeColor}">""")

  private def boardPreload(using ctx: Context) = frag(
    imagePreload(assetUrl(s"images/board/${ctx.pref.currentTheme.file}")),
    ctx.pref.is3d.option:
      imagePreload(assetUrl(s"images/staunton/board/${ctx.pref.currentTheme3d.file}"))
  )

  def boardStyle(zoomable: Boolean)(using ctx: lila.api.Context) =
    s"---board-opacity:${ctx.pref.board.opacity.toDouble / 100};" +
      s"---board-brightness:${ctx.pref.board.brightness.toDouble / 100};" +
      s"---board-hue:${ctx.pref.board.hue};" +
      zoomable.so(s"---zoom:$pageZoom;")

  def apply(p: Page)(using ctx: lila.api.PageContext): RenderedPage =
    import ctx.pref
    val allModules: EsmList = p.modules ++
      p.pageModule.fold(Nil)(module => esmPage(module.name)) ++
      (if (ctx.needsFp) fingerprintTag else Nil)
    val zenable = p.flags(PageFlags.zen)
    val playing = p.flags(PageFlags.playing)
    val pageFrag = frag(
      doctype,
      page.ui.htmlTag(
        (ctx.impersonatedBy.isEmpty && !ctx.blind)
          .option(cls := ctx.pref.themeColorClass),
        topComment,
        head(
          charset,
          viewport,
          metaCsp(p.csp.map(_(defaultCsp))),
          metaThemeColor,
          st.headTitle:
            val prodTitle = p.fullTitle | s"${p.title} • $siteName"
            if env.mode.isProd then prodTitle
            else s"${ctx.me.so(_.username.value + " ")} $prodTitle"
          ,
          cssTag("lib.theme.all"),
          cssTag("site"),
          pref.is3d.option(cssTag("lib.board-3d")),
          ctx.impersonatedBy.isDefined.option(cssTag("mod.impersonate")),
          ctx.blind.option(cssTag("bits.blind")),
          p.cssKeys.map(cssTag),
          meta(
            content := p.openGraph.fold("Chesstory - AI Chess Analysis")(o => o.description),
            name := "description"
          ),
          link(rel := "mask-icon", href := staticAssetUrl("logo/chesstory.svg"), attr("color") := "black"),
          favicons,
          (p.flags(PageFlags.noRobots) || !netConfig.crawlable).option:
            raw("""<meta content="noindex, nofollow" name="robots">""")
          ,
          noTranslate,
          p.openGraph.map(lila.web.ui.openGraph),
          p.atomLinkTag | dailyNewsAtom,
          (pref.bg == lila.pref.Pref.Bg.TRANSPARENT).option(pref.bgImgOrDefault).map { loc =>
            val url =
              if loc.startsWith("/assets/") then assetUrl(loc.drop(8))
              else escapeHtmlRaw(loc).replace("&amp;", "&")
            raw(s"""<style id="bg-data">html.transp::before{background-image:url("$url");}</style>""")
          },
          fontsPreload,
          boardPreload,
          manifests,
          p.withHrefLangs.map(hrefLangs),
          sitePreload(allModules),
          chesstoryFontFaceCss,
          pieceSetImages.load(ctx.pref.currentPieceSet.name),
          (ctx.pref.bg === lila.pref.Pref.Bg.SYSTEM || ctx.impersonatedBy.isDefined)
            .so(systemThemeScript(ctx.nonce))
        ).pipe(p.transformHead),
        st.body(
          cls := {
            val baseClass = s"${pref.currentBg} coords-${pref.coordsClass}"
            List(
              baseClass -> true,
              "simple-board" -> pref.simpleBoard,
              "piece-letter" -> pref.pieceNotationIsLetter,
              "blind-mode" -> ctx.blind,
              "kid" -> ctx.kid.yes,
              "mobile" -> lila.common.HTTPRequest.isMobileBrowser(ctx.req),
              "playing fixed-scroll" -> playing,
              "no-rating" -> !pref.showRatings,
              "zen" -> (pref.isZen || (playing && pref.isZenAuto)),
              "zenable" -> zenable,
              "zen-auto" -> (zenable && pref.isZenAuto)
            )
          },
          dataUser := ctx.userId,
          dataUsername := ctx.username,
          dataSoundSet := pref.currentSoundSet.toString,
          attr("data-socket-domains") := netConfig.socketDomains.mkString(","),
          dataAssetUrl := netConfig.assetBaseUrl.value,
          dataAssetVersion := assetVersion,
          dataNonce := ctx.nonce,
          dataTheme := pref.currentBg,
          dataBoard := pref.currentTheme.name,
          dataPieceSet := pref.currentPieceSet.name,
          dataBoard3d := pref.currentTheme3d.name,
          dataPieceSet3d := pref.currentPieceSet3d.name,
          dataAnnounce := env.announceApi.current.map(a => safeJsonValue(a.json).value),
          attr("data-brand-v2-header") := "1",
          attr("data-brand-explorer-proxy") := "1",
          attr("data-brand-legacy-lichess-alias") := "1",
          style := boardStyle(p.flags(PageFlags.zoom))
        )(
          zenable.option(div()),
          Option.unless(p.flags(PageFlags.noHeader)):
            ui.siteHeader(
              zenable = zenable,
              isAppealUser = ctx.isAppealUser,
              challenges = 0,
              notifications = ctx.nbNotifications,
              error = ctx.data.error,
              topnav = topnav(using ctx)
            )
          ,
          div(
            id := "main-wrap",
            cls := List(
              "full-screen-force" -> p.flags(PageFlags.fullScreen),
              "is2d" -> pref.is2d,
              "is3d" -> pref.is3d
            )
          )(p.transform(p.body)),
          bottomHtml,
          ctx.nonce.map(inlineJs(_, allModules)),
          modulesInit(allModules, ctx.nonce),
          p.pageModule.map { mod =>
            frag(
              jsonScript(mod.data),
              ctx.nonce.map(n => embedJsUnsafe(s"site.load.then(() => site.asset.loadEsmPage('${mod.name}'))")(n.some))
            )
          }
        )
      )
    )
    RenderedPage(pageFrag.render)
