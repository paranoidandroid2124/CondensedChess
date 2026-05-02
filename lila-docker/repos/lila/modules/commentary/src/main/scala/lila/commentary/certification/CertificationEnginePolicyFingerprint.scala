package lila.commentary.certification

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object CertificationEnginePolicyFingerprint:

  val DefaultTargetDepth = 18
  val DefaultFloorDepth = 16

  def forRole(
      engineConfigFingerprint: String,
      role: CertificationEngineRole,
      targetDepth: Int,
      floorDepth: Int,
      multiPv: Int,
      minPvPlies: Int,
      requiresBaseline: Boolean,
      requiresTransitionReplay: Boolean
  ): String =
    val payload =
      Vector(
        s"engine=${engineConfigFingerprint.trim}",
        s"role=${role.key}",
        s"target=$targetDepth",
        s"floor=$floorDepth",
        s"multiPv=$multiPv",
        s"minPvPlies=$minPvPlies",
        s"requiresBaseline=$requiresBaseline",
        s"requiresTransitionReplay=$requiresTransitionReplay"
      ).mkString("|")
    s"qpolicy-${sha256(payload).take(24)}"

  def defaultForRole(engineConfigFingerprint: String, role: CertificationEngineRole): String =
    forRole(
      engineConfigFingerprint = engineConfigFingerprint,
      role = role,
      targetDepth = defaultTargetDepth(role),
      floorDepth = defaultFloorDepth(role),
      multiPv = defaultMultiPv(role),
      minPvPlies = defaultMinPvPlies(role),
      requiresBaseline = defaultRequiresBaseline(role),
      requiresTransitionReplay = defaultRequiresTransitionReplay(role)
    )

  def defaultTargetDepth(role: CertificationEngineRole): Int =
    role match
      case _ => DefaultTargetDepth

  def defaultFloorDepth(role: CertificationEngineRole): Int =
    role match
      case _ => DefaultFloorDepth

  def defaultMultiPv(role: CertificationEngineRole): Int =
    role match
      case CertificationEngineRole.BestDefenseSurvival |
          CertificationEngineRole.CounterplayDenial |
          CertificationEngineRole.AmbiguityCheck =>
        3
      case _ => 1

  def defaultMinPvPlies(role: CertificationEngineRole): Int =
    role match
      case CertificationEngineRole.TablebaseFinality => 0
      case _ => 1

  def defaultRequiresBaseline(role: CertificationEngineRole): Boolean =
    role match
      case CertificationEngineRole.ComparativeSuperiority |
          CertificationEngineRole.ConversionRouteSurvival |
          CertificationEngineRole.CounterplayDenial =>
        true
      case _ => false

  def defaultRequiresTransitionReplay(role: CertificationEngineRole): Boolean =
    role match
      case CertificationEngineRole.CausalityCheck |
          CertificationEngineRole.AntiCausalityCheck |
          CertificationEngineRole.TacticalReleaseDetection |
          CertificationEngineRole.PersistenceCheck =>
        true
      case _ => false

  private def sha256(value: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_))
      .mkString
