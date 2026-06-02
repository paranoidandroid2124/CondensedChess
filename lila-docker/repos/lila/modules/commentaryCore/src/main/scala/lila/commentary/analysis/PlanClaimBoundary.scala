package lila.commentary.analysis

import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, ThemeResolver }
import lila.commentary.model.{ PlanMatch, PlanRow, ProbeRequest, StrategicPlanExperiment }
import lila.commentary.model.authoring.{ LatentSeed, PlanHypothesis }
import lila.commentary.model.strategic.PlanContinuity

object PlanClaimBoundary:

  enum PlanKindSource:
    case ExplicitSubplan
    case SupportSubplanTag
    case Missing

  enum PlanThemeSource:
    case ExplicitTheme
    case SupportThemeTag
    case SupportSubplanTag
    case InferredProposal
    case Missing

  enum PlanStage:
    case Proposal
    case Support
    case Admitted
    case Rendered

  final case class PlanProposal(
      planId: String,
      planName: String,
      theme: PlanTheme,
      themeSource: PlanThemeSource,
      kind: Option[PlanKind],
      kindSource: PlanKindSource,
      contract: Option[PlanSemanticsContract.Contract]
  ):
    def supportKind: Option[PlanKind] =
      kindSource match
        case PlanKindSource.ExplicitSubplan | PlanKindSource.SupportSubplanTag => kind
        case PlanKindSource.Missing                                             => None

    def stage: PlanStage =
      PlanStage.Proposal

    def fallbackTheme: Option[PlanTheme] =
      themeSource match
        case PlanThemeSource.ExplicitTheme | PlanThemeSource.SupportThemeTag | PlanThemeSource.SupportSubplanTag =>
          Option.when(theme != PlanTheme.Unknown)(theme)
        case PlanThemeSource.InferredProposal | PlanThemeSource.Missing =>
          None

  object PlanProposal:
    val empty: PlanProposal =
      PlanProposal(
        planId = "",
        planName = "",
        theme = PlanTheme.Unknown,
        themeSource = PlanThemeSource.Missing,
        kind = None,
        kindSource = PlanKindSource.Missing,
        contract = None
      )

    def fromHypothesis(hypothesis: PlanHypothesis): PlanProposal =
      val explicitKind =
        hypothesis.subplanId
          .flatMap(PlanKind.fromId)
          .map(kind => kind -> PlanKindSource.ExplicitSubplan)

      val taggedKind =
        hypothesis.evidenceSources.iterator
          .flatMap(source => ThemeResolver.subplanIdFromSupport(source).flatMap(PlanKind.fromId))
          .toList
          .headOption
          .map(kind => kind -> PlanKindSource.SupportSubplanTag)

      val kindWithSource =
        explicitKind.orElse(taggedKind)

      val kind = kindWithSource.map(_._1)
      val themeWithSource =
        PlanTheme
          .fromId(hypothesis.themeL1)
          .filter(_ != PlanTheme.Unknown)
          .map(_ -> PlanThemeSource.ExplicitTheme)
          .orElse(kindWithSource.map { case (kind, source) =>
            kind.theme -> themeSourceForKindSource(source)
          })
          .orElse(
            hypothesis.evidenceSources.iterator
              .flatMap(source => ThemeResolver.themeIdFromSupport(source).flatMap(PlanTheme.fromId))
              .toList
              .headOption
              .map(_ -> PlanThemeSource.SupportThemeTag)
          )
          .orElse {
            val resolved = ThemeResolver.fromHypothesis(hypothesis)
            Option.when(resolved != PlanTheme.Unknown)(resolved -> PlanThemeSource.InferredProposal)
          }
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)

      PlanProposal(
        planId = Option(hypothesis.planId).getOrElse(""),
        planName = Option(hypothesis.planName).getOrElse(""),
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = kind,
        kindSource = kindWithSource.map(_._2).getOrElse(PlanKindSource.Missing),
        contract = kind.flatMap(PlanSemanticsContract.forKind)
      )

    def fromPlanMatch(planMatch: PlanMatch): PlanProposal =
      val supportKind = firstSupportKind(planMatch.supports)
      val supportTheme = firstSupportTheme(planMatch.supports)
      val inferredTheme =
        firstResolvedTheme(
          List(
            planMatch.plan.id.toString,
            planMatch.plan.name
          )
        )
      val themeWithSource =
        supportKind
          .map(kind => kind.theme -> PlanThemeSource.SupportSubplanTag)
          .orElse(supportTheme.map(_ -> PlanThemeSource.SupportThemeTag))
          .orElse(inferredTheme.map(_ -> PlanThemeSource.InferredProposal))
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)
      PlanProposal(
        planId = Option(planMatch.plan.id.toString).getOrElse(""),
        planName = Option(planMatch.plan.name).getOrElse(""),
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = supportKind,
        kindSource = supportKind.map(_ => PlanKindSource.SupportSubplanTag).getOrElse(PlanKindSource.Missing),
        contract = supportKind.flatMap(PlanSemanticsContract.forKind)
      )

    def fromPlanRow(row: PlanRow): PlanProposal =
      val taggedKind =
        row.supports.iterator
          .flatMap(source => ThemeResolver.subplanIdFromSupport(source).flatMap(PlanKind.fromId))
          .toList
          .headOption
      val taggedTheme =
        row.supports.iterator
          .flatMap(source => ThemeResolver.themeIdFromSupport(source).flatMap(PlanTheme.fromId))
          .toList
          .headOption
      val themeWithSource =
        taggedKind
          .map(kind => kind.theme -> PlanThemeSource.SupportSubplanTag)
          .orElse(taggedTheme.map(_ -> PlanThemeSource.SupportThemeTag))
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)
      PlanProposal(
        planId = Option(row.name).getOrElse(""),
        planName = Option(row.name).getOrElse(""),
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = taggedKind,
        kindSource = taggedKind.map(_ => PlanKindSource.SupportSubplanTag).getOrElse(PlanKindSource.Missing),
        contract = taggedKind.flatMap(PlanSemanticsContract.forKind)
      )

    def fromProbeRequest(request: ProbeRequest): PlanProposal =
      val textSources =
        List(
          request.planId,
          request.planName,
          request.seedId
        ).flatten
      val explicitKind =
        List(request.planId, request.planName)
          .flatten
          .flatMap(PlanKind.fromId)
          .headOption
          .map(kind => kind -> PlanKindSource.ExplicitSubplan)
      val taggedKind =
        textSources
          .iterator
          .flatMap(source =>
            ThemeResolver
              .subplanIdFromSupport(source)
              .flatMap(PlanKind.fromId)
              .orElse(ThemeResolver.subplanFromAnnotatedText(source))
          )
          .toList
          .headOption
          .map(kind => kind -> PlanKindSource.SupportSubplanTag)
      val kindWithSource = explicitKind.orElse(taggedKind)
      val kind = kindWithSource.map(_._1)
      val explicitTheme =
        List(request.planId, request.planName)
          .flatten
          .flatMap(PlanTheme.fromId)
          .find(_ != PlanTheme.Unknown)
      val taggedTheme =
        textSources
          .iterator
          .flatMap(source => ThemeResolver.themeIdFromSupport(source).flatMap(PlanTheme.fromId))
          .toList
          .find(_ != PlanTheme.Unknown)
      val inferredTheme =
        firstResolvedTheme(textSources)
          .orElse(request.seedId.map(ThemeResolver.fromSeedId).filter(_ != PlanTheme.Unknown))
      val themeWithSource =
        explicitTheme
          .map(_ -> PlanThemeSource.ExplicitTheme)
          .orElse(kindWithSource.map { case (kind, source) =>
            kind.theme -> themeSourceForKindSource(source)
          })
          .orElse(taggedTheme.map(_ -> PlanThemeSource.SupportThemeTag))
          .orElse(inferredTheme.map(_ -> PlanThemeSource.InferredProposal))
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)
      PlanProposal(
        planId = request.planId.getOrElse(""),
        planName = request.planName.getOrElse(""),
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = kind,
        kindSource = kindWithSource.map(_._2).getOrElse(PlanKindSource.Missing),
        contract = kind.flatMap(PlanSemanticsContract.forKind)
      )

    def fromSeed(seed: LatentSeed): PlanProposal =
      val explicitKind =
        PlanKind
          .fromId(seed.id)
          .map(kind => kind -> PlanKindSource.ExplicitSubplan)
      val resolvedTheme =
        ThemeResolver.fromSeed(seed)
      val themeWithSource =
        explicitKind
          .map { case (kind, source) => kind.theme -> themeSourceForKindSource(source) }
          .orElse(Option.when(resolvedTheme != PlanTheme.Unknown)(resolvedTheme -> PlanThemeSource.InferredProposal))
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)
      PlanProposal(
        planId = seed.mapsToPlan.map(_.toString).getOrElse(seed.id),
        planName = seed.id,
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = explicitKind.map(_._1),
        kindSource = explicitKind.map(_._2).getOrElse(PlanKindSource.Missing),
        contract = explicitKind.map(_._1).flatMap(PlanSemanticsContract.forKind)
      )

    def fromContinuity(continuity: PlanContinuity): PlanProposal =
      val explicitKind =
        continuity.planId
          .flatMap(PlanKind.fromId)
          .orElse(PlanKind.fromId(continuity.planName))
          .map(kind => kind -> PlanKindSource.ExplicitSubplan)
      val explicitTheme =
        continuity.planId
          .flatMap(PlanTheme.fromId)
          .orElse(PlanTheme.fromId(continuity.planName))
          .filter(_ != PlanTheme.Unknown)
      val inferredTheme =
        firstResolvedTheme(continuity.planId.toList ++ List(continuity.planName))
      val themeWithSource =
        explicitTheme
          .map(_ -> PlanThemeSource.ExplicitTheme)
          .orElse(explicitKind.map { case (kind, _) => kind.theme -> PlanThemeSource.ExplicitTheme })
          .orElse(inferredTheme.map(_ -> PlanThemeSource.InferredProposal))
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)
      PlanProposal(
        planId = continuity.planId.getOrElse(""),
        planName = continuity.planName,
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = explicitKind.map(_._1),
        kindSource = explicitKind.map(_._2).getOrElse(PlanKindSource.Missing),
        contract = explicitKind.map(_._1).flatMap(PlanSemanticsContract.forKind)
      )

    def fromExperiment(experiment: StrategicPlanExperiment): PlanProposal =
      val explicitKind =
        experiment.subplanId
          .flatMap(PlanKind.fromId)
          .map(kind => kind -> PlanKindSource.ExplicitSubplan)
      val themeWithSource =
        PlanTheme
          .fromId(experiment.themeL1)
          .filter(_ != PlanTheme.Unknown)
          .map(_ -> PlanThemeSource.ExplicitTheme)
          .orElse(explicitKind.map { case (kind, source) => kind.theme -> themeSourceForKindSource(source) })
          .getOrElse(PlanTheme.Unknown -> PlanThemeSource.Missing)
      PlanProposal(
        planId = Option(experiment.planId).getOrElse(""),
        planName = Option(experiment.planId).getOrElse(""),
        theme = themeWithSource._1,
        themeSource = themeWithSource._2,
        kind = explicitKind.map(_._1),
        kindSource = explicitKind.map(_._2).getOrElse(PlanKindSource.Missing),
        contract = explicitKind.map(_._1).flatMap(PlanSemanticsContract.forKind)
      )

    private def firstSupportKind(sources: Iterable[String]): Option[PlanKind] =
      sources.iterator
        .flatMap(source => ThemeResolver.subplanIdFromSupport(source).flatMap(PlanKind.fromId))
        .toList
        .headOption

    private def firstSupportTheme(sources: Iterable[String]): Option[PlanTheme] =
      sources.iterator
        .flatMap(source => ThemeResolver.themeIdFromSupport(source).flatMap(PlanTheme.fromId))
        .toList
        .find(_ != PlanTheme.Unknown)

    private def firstResolvedTheme(sources: Iterable[String]): Option[PlanTheme] =
      sources.iterator
        .map(source =>
          val fromId = ThemeResolver.fromPlanId(source)
          if fromId != PlanTheme.Unknown then fromId
          else ThemeResolver.fromPlanName(source)
        )
        .find(_ != PlanTheme.Unknown)

    private def themeSourceForKindSource(source: PlanKindSource): PlanThemeSource =
      source match
        case PlanKindSource.ExplicitSubplan   => PlanThemeSource.ExplicitTheme
        case PlanKindSource.SupportSubplanTag => PlanThemeSource.SupportSubplanTag
        case PlanKindSource.Missing           => PlanThemeSource.Missing

  final case class PlanSupport(
      proposal: PlanProposal,
      supportProbeIds: List[String],
      transpositionProofIds: List[String],
      refuteProbeIds: List[String],
      missingSignals: List[String]
  ):
    def stage: PlanStage =
      PlanStage.Support

  final case class AdmittedPlanClaim(
      proposal: PlanProposal,
      supportProbeIds: List[String],
      transpositionProofIds: List[String]
  ):
    def stage: PlanStage =
      PlanStage.Admitted

  final case class RenderedPlanText(
      claim: AdmittedPlanClaim,
      text: String
  ):
    def stage: PlanStage =
      PlanStage.Rendered
