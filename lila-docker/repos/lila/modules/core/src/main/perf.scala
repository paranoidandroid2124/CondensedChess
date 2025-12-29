package lila.core

import _root_.chess.variant.Variant
import _root_.chess.{ Speed, variant as ChessVariant }

object perf:

  opaque type PerfKey = String
  object PerfKey:
    val bullet: PerfKey = "bullet"
    val blitz: PerfKey = "blitz"
    val rapid: PerfKey = "rapid"
    val classical: PerfKey = "classical"
    val correspondence: PerfKey = "correspondence"
    val standard: PerfKey = "standard"
    val ultraBullet: PerfKey = "ultraBullet"
    val chess960: PerfKey = "chess960"
    val kingOfTheHill: PerfKey = "kingOfTheHill"
    val threeCheck: PerfKey = "threeCheck"
    val antichess: PerfKey = "antichess"
    val atomic: PerfKey = "atomic"
    val horde: PerfKey = "horde"
    val racingKings: PerfKey = "racingKings"
    val crazyhouse: PerfKey = "crazyhouse"
    val puzzle: PerfKey = "puzzle"
    val list: List[PerfKey] = List(
      bullet,
      blitz,
      rapid,
      classical,
      correspondence,
      standard,
      ultraBullet,
      chess960,
      kingOfTheHill,
      threeCheck,
      antichess,
      atomic,
      horde,
      racingKings,
      crazyhouse,
      puzzle
    )
    val all: Set[PerfKey] = list.toSet

    extension (key: PerfKey)
      def value: String = key

    def apply(key: String): Option[PerfKey] = Option.when(all.contains(key))(key)
    def apply(variant: Variant, speed: Speed): PerfKey = byVariant(variant) | standardBySpeed(speed)

    def byVariant(variant: Variant): Option[PerfKey] = variant match
      case ChessVariant.Standard => none
      case ChessVariant.FromPosition => none
      case ChessVariant.Crazyhouse => crazyhouse.some
      case ChessVariant.Chess960 => chess960.some
      case ChessVariant.KingOfTheHill => kingOfTheHill.some
      case ChessVariant.ThreeCheck => threeCheck.some
      case ChessVariant.Antichess => antichess.some
      case ChessVariant.Atomic => atomic.some
      case ChessVariant.Horde => horde.some
      case ChessVariant.RacingKings => racingKings.some

    def standardBySpeed(speed: Speed): PerfKey = speed match
      case Speed.Bullet => bullet
      case Speed.Blitz => blitz
      case Speed.Rapid => rapid
      case Speed.Classical => classical
      case Speed.Correspondence => correspondence
      case Speed.UltraBullet => ultraBullet
