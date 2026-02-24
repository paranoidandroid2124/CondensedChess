package lila.llm

class MoveAnchorCodecTest extends munit.FunSuite:

  private val refs = BookmakerRefsV1(
    startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    startPly = 1,
    variations = List(
      VariationRefV1(
        lineId = "line_01",
        scoreCp = 20,
        mate = None,
        depth = 0,
        moves = List(
          MoveRefV1(
            refId = "l01_m01",
            san = "e4",
            uci = "e2e4",
            fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
            ply = 1,
            moveNo = 1,
            marker = Some("1.")
          ),
          MoveRefV1(
            refId = "l01_m02",
            san = "e5",
            uci = "e7e5",
            fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            ply = 2,
            moveNo = 1,
            marker = Some("1...")
          )
        )
      )
    )
  )

  test("encode/decode should preserve SAN commentary while inserting stable anchors") {
    val prose = "Main line: 1. e4 e5 keeps central tension."
    val encoded = MoveAnchorCodec.encode(prose, Some(refs))

    assert(encoded.anchoredText.contains("[[MV_l01_m01]]"))
    assert(encoded.anchoredText.contains("[[MV_l01_m02]]"))
    assert(encoded.anchoredText.contains("[[MK_l01_m01]]"))

    val decoded = MoveAnchorCodec.decode(encoded.anchoredText, encoded.refById)
    assert(decoded.contains("1."))
    assert(decoded.contains("e4"))
    assert(decoded.contains("e5"))
  }

  test("validateAnchors should detect missing anchors") {
    val prose = "Main line: 1. e4 e5 keeps central tension."
    val encoded = MoveAnchorCodec.encode(prose, Some(refs))
    val broken = encoded.anchoredText.replace("[[MV_l01_m02]]", "")
    val reasons = MoveAnchorCodec.validateAnchors(
      text = broken,
      expectedMoveOrder = encoded.expectedMoveOrder,
      expectedMarkerOrder = encoded.expectedMarkerOrder
    )
    assert(reasons.contains("anchor_missing"))
  }

  test("validateAnchors should detect move-anchor order violations") {
    val reasons = MoveAnchorCodec.validateAnchors(
      text = "Sequence: [[MV_l01_m02]] then [[MV_l01_m01]].",
      expectedMoveOrder = List("l01_m01", "l01_m02"),
      expectedMarkerOrder = Nil
    )
    assert(reasons.contains("anchor_order_violation"))
  }
