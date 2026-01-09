package lila.core
package pref

import lila.core.userId.UserId

trait PrefTheme:
  def name: String
  def file: String

trait PrefPieceSet:
  def name: String

trait PrefBoard:
  def opacity: Int
  def brightness: Int
  def hue: Int

trait Pref:
  val id: UserId
  val coords: Int
  val keyboardMove: Int
  val voice: Option[Int]
  val rookCastle: Int
  val animation: Int
  val destination: Boolean
  val moveEvent: Int
  val highlight: Boolean
  val is3d: Boolean
  val resizeHandle: Int
  val theme: String
  val pieceSet: String

  def hasKeyboardMove: Boolean
  def hasVoice: Boolean
  def hasSpeech: Boolean
  def animationMillis: Int
  def pieceNotationIsLetter: Boolean
  def currentBg: String
  def showRatings: Boolean = true

  def themeColor: String
  def themeColorClass: Option[String]

  def board: PrefBoard

  def currentTheme: PrefTheme
  def currentTheme3d: PrefTheme
  def realTheme: PrefTheme

  def currentPieceSet: PrefPieceSet
  def realPieceSet: PrefPieceSet
