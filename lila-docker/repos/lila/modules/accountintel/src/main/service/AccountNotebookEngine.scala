package lila.accountintel.service

import java.time.Instant

import lila.accountintel.*
import lila.accountintel.AccountIntel.*
import lila.accountintel.cluster.ClusterSelector
import lila.accountintel.cluster.StructureClusterer
import lila.accountintel.dossier.NotebookDossierAssembler
import lila.accountintel.primitive.SnapshotFeatureExtractor
import lila.accountintel.snapshot.DecisionSnapshotDetector

final class AccountNotebookEngine(
    source: AccountGameFetcher,
    selectiveEvalRefiner: SelectiveEvalRefiner
)(using Executor):

  val requestedGameLimit = 40

  def build(
      provider: String,
      username: String,
      kind: ProductKind,
      generatedAt: Instant = nowInstant
  ): Fu[Either[String, NotebookBuildArtifact]] =
    source
      .fetchRecentGames(provider, username)
      .flatMap: games =>
        buildFromGames(provider, username, kind, games, generatedAt)

  def buildFromGames(
      provider: String,
      username: String,
      kind: ProductKind,
      games: List[ExternalGame],
      generatedAt: Instant = nowInstant
  ): Fu[Either[String, NotebookBuildArtifact]] =
    (for
      safeProvider <- normalizeProvider(provider).toRight("Unsupported provider.")
      safeUsername <- normalizeUsername(username).toRight("Invalid username.")
      bundle <- SnapshotFeatureExtractor.extract(safeUsername, games)
    yield (safeProvider, safeUsername, bundle)).fold(
      err => fuccess(Left(err)),
      { case (safeProvider, safeUsername, bundle) =>
        val candidates = DecisionSnapshotDetector.detect(bundle.featureRows)
        val clusters = StructureClusterer.cluster(candidates)
        val selectedClusters = ClusterSelector.select(kind, clusters)
        selectiveEvalRefiner
          .refine(safeProvider, kind, selectedClusters, bundle.featureRows)
          .map: refined =>
            val heuristicWarnings =
              Option.when(
                safeProvider == "chesscom" &&
                  refined.clusters.exists(cluster =>
                    cluster.exemplar.candidate.exists(candidate =>
                      !candidate.collapseBacked && candidate.snapshotConfidence < 0.72d
                    )
                  )
              )("snapshot confirmed heuristically").toList
            NotebookDossierAssembler.assemble(
              provider = safeProvider,
              username = safeUsername,
              kind = kind,
              bundle = bundle.copy(
                warnings = (bundle.warnings ++ heuristicWarnings ++ refined.warnings).distinct
              ),
              selectedClusters = refined.clusters,
              allClusters = clusters,
              requestedGameLimit = requestedGameLimit,
              generatedAt = generatedAt
            )
      }
    )
