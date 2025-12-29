package lila.core
package pref

import lila.core.userId.UserId

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
