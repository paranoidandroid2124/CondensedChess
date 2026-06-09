package lila.commentary.tools.claim

private[commentary] object SourceWitnessCatalog:

  final case class CandidatePlyRange(start: Int, end: Int):
    def contains(ply: Int): Boolean = ply >= start && ply <= end
    override def toString: String = s"$start-$end"

  final case class SourceCandidate(
      id: String,
      gameName: String,
      sourceUrl: String,
      pgn: String,
      candidatePlyRange: CandidatePlyRange,
      reviewGroup: String,
      intendedVerdict: String,
      validationNote: String
  )

  private val fixedBatch: List[SourceCandidate] =
    List(
      SourceCandidate(
        id = "source-evans-opsahl-1950",
        gameName = "Evans-Opsahl, Dubrovnik Olympiad 1950",
        sourceUrl = "https://www.chessgames.com/perl/chessgame?gid=1152575",
        pgn =
          """[Event "Dubrovnik Olympiad"]
            |[Site "Dubrovnik YUG"]
            |[Date "1950.08.30"]
            |[Round "8"]
            |[White "Larry Melvyn Evans"]
            |[Black "Haakon Opsahl"]
            |[Result "1-0"]
            |[ECO "D51"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nc3 d5 4. Bg5 Nbd7 5. e3 Be7 6. Qc2 O-O
            |7. cxd5 exd5 8. Nf3 c6 9. Bd3 Re8 10. O-O Nf8 11. Rab1 Ne4
            |12. Bxe7 Qxe7 13. b4 a6 14. a4 Nxc3 15. Qxc3 Bg4 16. Nd2 Qg5
            |17. Rfc1 Re6 18. b5 axb5 19. axb5 Bh3 20. g3 Rae8
            |21. bxc6 bxc6 22. Bf1 Bxf1 23. Nxf1 Ng6 24. Rb6 Ne7
            |25. Qb4 h5 26. Rb8 Rxb8 27. Qxb8+ Kh7 28. Qf4 Qxf4 29. gxf4 g6
            |30. Nd2 Rd6 31. Kf1 Kg7 32. Ra1 Rd7 33. Nb3 Rb7 34. Nc5 Rb2
            |35. Ra7 Kf6 36. Ra6 Rb1+ 37. Kg2 Rb2 38. Ra7 Rb1 39. Rc7 Ra1
            |40. Nd3 Ke6 41. Nc5+ Kf6 42. Nd7+ Ke6 43. Nf8+ Kf6 44. Nh7+ Ke6
            |45. Ng5+ Kd6 46. Rb7 f6 47. Nh7 Ke6 48. Nf8+ Kf7 49. Nxg6 Kxg6
            |50. Rxe7 Kf5 51. Rc7 Rc1 52. Rc8 Kg6 53. Kg3 Rc2 54. h4 Kf5
            |55. Rh8 Kg6 56. f5+ Kxf5 57. Rxh5+ Kg6 58. Rh8 Kf5 59. Rg8 Rc1
            |60. Kg2 Ra1 61. h5 Ra7 62. Rg3 Rh7 63. Rh3 Kg5 64. Kf3 Rh6
            |65. Rh1 Kf5 66. Kg3 Kg5 67. Rh4 Kf5 68. Rf4+ Kg5 69. Rg4+ Kf5
            |70. Kh4 Rh8 71. Rg7 Ra8 72. h6 Ra1 73. Rg3 Rh1+ 74. Rh3 Rg1
            |75. Rf3+ Kg6 76. Rg3+ Rxg3 77. Kxg3 Kxh6 78. Kg4 Kg6
            |79. Kf4 Kg7 80. Kf5 Kf7 81. f3 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(25, 43),
        reviewGroup = "B:carlsbad_fixed_target",
        intendedVerdict = "screen_only",
        validationNote = "Replay around b4-b5xc6; promote only if engine PV proves target persistence and owner path."
      ),
      SourceCandidate(
        id = "source-karpov-unzicker-1974-break-prevention",
        gameName = "Karpov-Unzicker, Nice Olympiad 1974",
        sourceUrl = "https://chesspro.ru/guestnew/upload/viewer/724520.pgn",
        pgn =
          """[Event "Olympiad Final-A"]
            |[Site "Nice FRA"]
            |[Date "1974.06.18"]
            |[Round "4"]
            |[White "Anatoly Karpov"]
            |[Black "Wolfgang Unzicker"]
            |[Result "1-0"]
            |[ECO "C98"]
            |
            |1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7
            |6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Na5 10. Bc2 c5
            |11. d4 Qc7 12. Nbd2 Nc6 13. d5 Nd8 14. a4 Rb8
            |15. axb5 axb5 16. b4 Nb7 17. Nf1 Bd7 18. Be3 Ra8
            |19. Qd2 Rfc8 20. Bd3 g6 21. Ng3 Bf8 22. Ra2 c4
            |23. Bb1 Qd8 24. Ba7 Ne8 25. Bc2 Nc7 26. Rea1 Qe7
            |27. Bb1 Be8 28. Ne2 Nd8 29. Nh2 Bg7 30. f4 f6
            |31. f5 g5 32. Bc2 Bf7 33. Ng3 Nb7 34. Bd1 h6
            |35. Bh5 Qe8 36. Qd1 Nd8 37. Ra3 Kf8 38. R1a2 Kg8
            |39. Ng4 Kf8 40. Ne3 Kg8 41. Bxf7+ Nxf7 42. Qh5 Nd8
            |43. Qg6 Kf8 44. Nh5 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(31, 47),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Natural break-prevention source candidate around 16.b4 through 24.Ba7; admit no authority unless exact replay proves neutralize_key_break owner packet, tactical veto, and claim-only surface."
      ),
      SourceCandidate(
        id = "source-karpov-andersson-1975-hedgehog-break-screen",
        gameName = "Karpov-Andersson, Milan 1975",
        sourceUrl = "https://www.pgnmentor.com/events/Milan1975.pgn",
        pgn =
          """[Event "Milan"]
            |[Site "Milan"]
            |[Date "1975.??.??"]
            |[Round "8"]
            |[White "Karpov, Anatoly"]
            |[Black "Andersson, Ulf"]
            |[Result "0-1"]
            |[WhiteElo "2705"]
            |[BlackElo "2565"]
            |[ECO "B44"]
            |
            |1. e4 c5 2. Nf3 e6 3. d4 cxd4 4. Nxd4 Nc6 5. Nb5 d6
            |6. c4 Nf6 7. N1c3 a6 8. Na3 Be7 9. Be2 O-O
            |10. O-O b6 11. Be3 Bb7 12. Rc1 Re8 13. Qb3 Nd7
            |14. Rfd1 Rc8 15. Rd2 Qc7 16. Qd1 Qb8 17. f3 Ba8
            |18. Qf1 Nce5 19. Nab1 Nf6 20. Kh1 h6 21. Rdd1 Bf8
            |22. Nd2 Rcd8 23. Qf2 Ned7 24. a3 d5 25. cxd5 exd5
            |26. exd5 Bd6 27. Nf1 Rxe3 28. Nxe3 Bxh2 29. Nf1 Bf4
            |30. Rc2 b5 31. Bd3 Nb6 32. Be4 Nc4 33. a4 Re8
            |34. axb5 axb5 35. Re2 Be5 36. Qc5 Nd6 37. Na2 Ndxe4
            |38. fxe4 Bd6 39. Qc2 Re5 40. g3 Qe8 41. Rde1 Bb7
            |42. Kg1 Nh7 43. Nc1 Ng5 44. Nd2 Bb4 45. Kf2 Bxd2
            |46. Rxd2 Nxe4+ 47. Rxe4 Rxe4 48. Ne2 Bc8 49. Nc3 Re1
            |50. Ne2 Ra1 51. Rd4 Qd8 52. Qc6 Bd7 53. Qd6 Qe8
            |54. Qf4 Qc8 55. b4 Bh3 56. Qe4 Bf5 57. Qe3 Qc2
            |58. g4 Bd7 59. Qe4 Qb3 60. Qd3 Qb2 61. Qe4 Ra8
            |62. Qe3 Ra2 63. d6 Ra8 64. Re4 Bc6 65. Qd4 Qb1
            |66. Re7 Qh1 67. Qf4 Qg2+ 68. Ke1 Ra1+ 69. Kd2 Qd5+
            |70. Qd4 Ra2+ 71. Kc3 Qf3+ 72. Re3 Ra3+ 73. Kd2 Ra2+
            |74. Ke1 Qh1+ 75. Kf2 Qg2+ 76. Ke1 Qh1+ 77. Kf2 Ra1
            |78. Rc3 Qg2+ 79. Ke3 Qf3+ 0-1
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(47, 50),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Hedgehog break-window screen candidate around 24.a3 ...d5 and captures; keep screen-only unless exact owner proof shows a bounded break-prevention packet rather than tactical or rival release."
      ),
      SourceCandidate(
        id = "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
        gameName = "Lokvenc-Czerniak, Helsinki Olympiad 1952",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "Helsinki olm"]
            |[Site "Helsinki"]
            |[Date "1952.??.??"]
            |[Round "?"]
            |[White "Lokvenc, Josef"]
            |[Black "Czerniak, Moshe"]
            |[Result "1-0"]
            |[ECO "A76"]
            |
            |1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6
            |6. e4 g6 7. Be2 Bg7 8. Nf3 O-O 9. O-O Re8 10. Qc2 b6
            |11. Re1 Na6 12. Bb5 Re7 13. Qa4 Nc7 14. Bc6 Rb8
            |15. Bf4 Nh5 16. Bg5 f6 17. Be3 a6 18. h3 Bf8
            |19. Rad1 b5 20. Qc2 b4 21. Nb1 Nb5 22. Nbd2 Bd7
            |23. Bxd7 Qxd7 24. Nc4 Rbe8 25. Bc1 Rf7 26. Qd3 Nc7
            |27. g4 Ng7 28. Nh2 Rfe7 29. b3 Rd8 30. Bb2 Nge8
            |31. Na5 Rf7 32. Nc6 Ra8 33. f4 Bg7 34. Re2 Rf8
            |35. f5 g5 36. Qg3 a5 37. h4 h6 38. Nf3 a4
            |39. Nd2 Nb5 40. Nc4 Qc7 41. hxg5 fxg5 42. e5 dxe5
            |43. Bxe5 Bxe5 44. Rxe5 Nc3 45. Ne7+ Kg7 46. Rde1 axb3
            |47. axb3 Ra2 48. f6+ Rxf6 49. Nf5+ Kg6 50. Qd3 Rfa6
            |51. Nfd6+ 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(23, 23),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp scanner candidate: exact ply 23 Bb5 denies the board-derived ...b6-b5 route with no same-destination transform; admit only if SourceReview materializes a neutralize_key_break packet and claim-only SupportedLocal surface."
      ),
      SourceCandidate(
        id = "source-maderna-palermo-1955-a6-a5-break-prevention",
        gameName = "Maderna-Palermo, Argentine Championship 1955",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "ARG-ch"]
            |[Site "Buenos Aires"]
            |[Date "1955.??.??"]
            |[Round "18"]
            |[White "Maderna, Carlos"]
            |[Black "Palermo, Vicente"]
            |[Result "1-0"]
            |[ECO "A69"]
            |
            |1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6
            |6. e4 g6 7. Nf3 Bg7 8. Be2 O-O 9. Nd2 Nbd7
            |10. O-O a6 11. a4 Re8 12. f4 Rb8 13. Re1 Nb6
            |14. Bf3 Nfd7 15. a5 Na8 16. Nc4 Qc7 17. e5 dxe5
            |18. d6 Qd8 19. Nd5 e4 20. Ne7+ Kf8 21. Rxe4 Nf6
            |22. Re1 Be6 23. Ne5 Ng8 24. f5 Bxf5 25. Bg5 f6
            |26. Qd5 fxe5 27. Qxg8+ 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(29, 29),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp scanner candidate: exact ply 29 a5 denies the board-derived ...a6-a5 route with no same-destination transform; keep screen-only unless exact neutralize_key_break authority and claim-only surface appear."
      ),
      SourceCandidate(
        id = "source-camara-bazan-1960-b7-b5-break-prevention",
        gameName = "Camara-Bazan, Sao Paulo Zonal Tournament 1960",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "Sao Paulo zt"]
            |[Site "Sao Paulo"]
            |[Date "1960.??.??"]
            |[Round "13"]
            |[White "Camara, Ronald"]
            |[Black "Bazan, Osvaldo"]
            |[Result "0-1"]
            |[ECO "A67"]
            |
            |1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6
            |6. e4 g6 7. f4 Bg7 8. Bb5+ Nfd7 9. Bd3 O-O
            |10. Nge2 Na6 11. O-O Re8 12. h3 Rb8 13. Kh1 Nb4
            |14. Bb5 Na6 15. Bd3 Nc7 16. a4 b6 17. Nb5 Nf6
            |18. Nec3 a6 19. Nxc7 Qxc7 20. Qf3 c4 21. Bc2 Nd7
            |22. e5 dxe5 23. f5 Nc5 24. fxg6 fxg6 25. d6 Qb7
            |26. Nd5 Kh8 27. Bg5 e4 28. Qd1 Nd3 29. Nc7 Re5
            |30. Qd2 Bxh3 31. Qe3 Bf5 32. b3 c3 33. b4 Qc6
            |34. Nxa6 Rg8 35. Be7 Bc8 36. Bb3 Bxa6 37. Bxg8 Kxg8
            |38. b5 Qd5 39. bxa6 Rh5+ 40. Kg1 Bd4 41. Rf8+ Kg7
            |42. Bf6+ Bxf6 43. Rf1 Kxf8 44. Qxb6 Qc5+ 45. Qxc5 Rxc5 0-1
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(27, 27),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp source triage admission: exact ply 27 Bb5 denies the board-derived ...b7-b5 route with no same-destination transform; admitted only under neutralize_key_break owner packet proof and claim-only SupportedLocal surfaces."
      ),
      SourceCandidate(
        id = "source-sliwa-gromek-1960-a6-a5-break-prevention",
        gameName = "Sliwa-Gromek, Polish Championship 1960",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "POL-ch"]
            |[Site "Wroclaw"]
            |[Date "1960.??.??"]
            |[Round "4"]
            |[White "Sliwa, Bogdan"]
            |[Black "Gromek, Jozef"]
            |[Result "1-0"]
            |[ECO "A67"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nc3 c5 4. d5 exd5 5. cxd5 d6
            |6. e4 g6 7. f4 Bg7 8. Bb5+ Nfd7 9. Bd3 Qh4+
            |10. g3 Qe7 11. Nf3 O-O 12. O-O Nb6 13. Re1 Bg4
            |14. Bf1 Na6 15. h3 Bxf3 16. Qxf3 Nb4 17. Qd1 Rfe8
            |18. Be3 Rac8 19. a3 Na6 20. Bf2 Nc7 21. Qb3 Rb8
            |22. Rad1 Red8 23. a4 Kh8 24. Qa3 Na6 25. Nb5 Nb4
            |26. Qb3 a6 27. Na3 Qd7 28. a5 Qa4 29. Qxa4 Nxa4
            |30. b3 Nc3 31. Rd2 Re8 32. Bg2 Rbd8 33. Nc4 Kg8
            |34. Kf1 f5 35. Nxd6 Rxd6 36. Bxc5 Rdd8 37. Bxb4 fxe4
            |38. d6 Rd7 39. Rc2 Nd5 40. Ba3 Bc3 41. Ree2 Bd4
            |42. Bxe4 Ne3+ 43. Rxe3 Bxe3 44. Bd5+ Kf8 45. Rc7 Red8
            |46. Bxb7 Bd2 47. Bc6 Rxd6 48. Bd5 Ke8 49. Bf7+ Kf8
            |50. Bc4 Bxa5 51. Rc6 Ke7 52. Bxa6 Bd2 53. Ke2 Ba5
            |54. Bc5 Rd7 55. b4 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(55, 55),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp scanner candidate: exact ply 55 a5 denies the board-derived ...a6-a5 route with no same-destination transform; admit only if the exact neutralize_key_break packet, planner owner, and claim-only SupportedLocal surface materialize."
      ),
      SourceCandidate(
        id = "source-luckis-bielicki-1961-a6-a5-break-prevention",
        gameName = "Luckis-Bielicki, Argentine Championship 1961",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "ARG-ch"]
            |[Site "Buenos Aires"]
            |[Date "1961.??.??"]
            |[Round "14"]
            |[White "Luckis, Marcos"]
            |[Black "Bielicki, Carlos"]
            |[Result "1-0"]
            |[ECO "A75"]
            |
            |1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6
            |6. e4 g6 7. Nf3 Bg7 8. Be2 O-O 9. O-O a6
            |10. a4 Bg4 11. h3 Bxf3 12. Bxf3 Nbd7 13. Bf4 Qc7
            |14. Qc2 Rfe8 15. Be2 c4 16. Rfe1 Rac8 17. a5 b5
            |18. axb6 Qxb6 19. Ra4 Bf8 20. Rxc4 Rxc4 21. Bxc4 Rb8
            |22. b3 Nc5 23. b4 Ncd7 24. Na2 Rc8 25. Qe2 Re8
            |26. Be3 Qd8 27. f3 a5 28. Bb5 axb4 29. Nxb4 Qa5
            |30. Nc6 Qa3 31. Qd3 Qa2 32. Bc4 Qa4 33. Qc3 Ra8
            |34. Bd4 Bg7 35. Rb1 h5 36. Qc1 Qa3 37. Qd2 Ne5
            |38. Nxe5 dxe5 39. Ra1 Qf8 40. Rxa8 Qxa8 41. Be3 Bf8
            |42. Qa2 Qxa2 43. Bxa2 Nd7 44. Kf1 Bc5 45. Bd2 Kg7
            |46. Bb1 Nb6 47. Bd3 f5 48. Bc3 Kf6 49. f4 Bd6
            |50. Ke2 Na4 51. Ba1 Nc5 52. exf5 gxf5 53. Bc2 Nd7
            |54. Bc3 Nf8 55. h4 Ng6 56. g3 Nf8 57. Ba4 Ng6
            |58. Be8 Ne7 59. Bc6 Ng6 60. Bd7 Ne7 61. Be6 Ng6
            |62. Kf3 Bc7 63. Bb2 Bd6 64. Bd7 Ne7 65. Bc6 Ng6
            |66. Be8 Ne7 67. Bc6 Ng6 68. Bd7 Ne7 69. Be6 Ng6
            |70. Ke2 Bc7 71. Bc8 Ne7 72. Bb7 Ng6 73. Bc8 Ne7
            |74. Be6 Ng6 75. Bc3 Bd6 76. Ba1 Bc7 77. Bc3 Bb8
            |78. Kd3 Ba7 79. fxe5+ Nxe5+ 80. Ke2 Bb8 81. Bd7 Kg6
            |82. Be8+ Nf7 83. Kf3 Bd6 84. Bd4 Bc7 85. Be3 Kf6
            |86. Bxf7 Kxf7 87. Bf4 Ba5 88. Ke3 Ke7 89. Kd4 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(33, 33),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp scanner candidate: exact ply 33 a5 denies the board-derived ...a6-a5 route with no same-destination transform; keep screen-only unless exact replay promotes neutralize_key_break with claim-only SupportedLocal parity."
      ),
      SourceCandidate(
        id = "source-pfleger-maalouf-1961-a6-a5-break-prevention",
        gameName = "Pfleger-Maalouf, World Junior Championship preliminary 1961",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "Wch U20 prel-B"]
            |[Site "The Hague"]
            |[Date "1961.??.??"]
            |[Round "4"]
            |[White "Pfleger, Helmut"]
            |[Black "Maalouf, Carlos"]
            |[Result "1-0"]
            |[ECO "A70"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nc3 c5 4. d5 exd5 5. cxd5 d6
            |6. e4 g6 7. Nf3 Bg7 8. Bb5+ Bd7 9. Bxd7+ Nbxd7
            |10. O-O O-O 11. Bg5 h6 12. Bf4 Nb6 13. Qd3 Nh5
            |14. Bd2 Nf6 15. Rfe1 Re8 16. a4 a6 17. a5 Nbd7
            |18. Bf4 Qb8 19. Re2 Ng4 20. h3 Nge5 21. Nxe5 Nxe5
            |22. Bxe5 Bxe5 23. Na4 Qc7 24. Nb6 Rad8 25. Rf1 Qe7
            |26. f4 Bd4+ 27. Kh1 Qf6 28. Rf3 Qg7 29. Nc4 Kf8
            |30. Rf1 Qf6 31. Ree1 Qg7 32. e5 dxe5 33. fxe5 Bxe5
            |34. Nxe5 Rxe5 35. Rxe5 Qxe5 36. Qxg6 Qxd5 37. Qxh6+ Kg8
            |38. Rf3 Qd1+ 39. Kh2 Rd6 40. Rg3+ Rg6 41. Rxg6+ fxg6
            |42. Qxg6+ 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(33, 33),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp scanner candidate: exact ply 33 a5 denies the board-derived ...a6-a5 route with no same-destination transform; promote only under exact neutralize_key_break owner packet proof and claim-only surfaces."
      ),
      SourceCandidate(
        id = "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention",
        gameName = "Polugaevsky-Giorgadze, USSR Championship semifinal 1956",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "URS-ch sf"]
            |[Site "Tbilisi"]
            |[Date "1956.??.??"]
            |[Round "19"]
            |[White "Polugaevsky, Lev"]
            |[Black "Giorgadze, Tamaz"]
            |[Result "1-0"]
            |[ECO "A65"]
            |
            |1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6
            |6. e4 Be7 7. Nf3 O-O 8. Be2 Ne8 9. O-O Bf6
            |10. Nd2 b6 11. a4 a6 12. Nc4 Nd7 13. f4 Rb8
            |14. e5 dxe5 15. Ne4 exf4 16. Nxf6+ Ndxf6 17. Bxf4 Rb7
            |18. Bf3 Rd7 19. Ne5 Rxd5 20. Bxd5 Qxd5 21. Qxd5 Nxd5
            |22. a5 bxa5 23. Rad1 Nef6 24. Bg5 Be6 25. Bxf6 gxf6
            |26. Nd7 Ne3 27. Nxf8 Kxf8 28. Rd6 Nxf1 29. Kxf1 a4
            |30. Kf2 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(23, 23),
        reviewGroup = "A:break_prevention",
        intendedVerdict = "screen_only",
        validationNote =
          "Clean route-clamp scanner candidate: exact ply 23 Nc4 denies the board-derived ...c5-c4 route with no same-destination transform; promote only if the live owner path produces neutralize_key_break with SupportedLocal parity."
      ),
      SourceCandidate(
        id = "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation",
        gameName = "Boleslavsky-Nezhmetdinov, USSR Championship semifinal 1950",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "URS-ch sf"]
            |[Site "Gorky"]
            |[Date "1950.??.??"]
            |[Round "?"]
            |[White "Boleslavsky, Isaak"]
            |[Black "Nezhmetdinov, Rashid"]
            |[Result "1/2-1/2"]
            |[ECO "A77"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nc3 c5 4. d5 exd5 5. cxd5 d6
            |6. e4 g6 7. Be2 Bg7 8. Nf3 O-O 9. O-O Re8 10. Nd2 b6
            |11. a4 Ba6 12. Bb5 Bxb5 13. axb5 Nbd7 14. Qc2 Ne5
            |15. f4 Ned7 16. Nf3 c4 17. Re1 Qc7 18. Ra4 a6
            |19. Rxa6 Rxa6 20. bxa6 b5 21. Nxb5 Qc5+ 22. Nbd4 Nxd5
            |23. exd5 Rxe1+ 24. Nxe1 Bxd4+ 25. Kf1 Qb5 26. Qe2 Qxa6
            |27. Nc2 Bf6 28. Na3 Nb6 29. Qe8+ Kg7 30. Qb5 Qxb5
            |31. Nxb5 Be7 32. Na3 Bf6 33. Nb5 Be7 1/2-1/2
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(19, 19),
        reviewGroup = "B:static_weakness_fixation",
        intendedVerdict = "screen_only",
        validationNote =
          "Natural StaticWeaknessFixation authority candidate from public ModernBenoni6e4 PGN: exact ply 19 Nd2 must materialize exact_target_fixation on d6 on the engine top line before matrix admission."
      ),
      SourceCandidate(
        id = "source-maderna-palermo-1955-static-weakness-fixation",
        gameName = "Maderna-Palermo, Argentine Championship 1955",
        sourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip",
        pgn =
          """[Event "ARG-ch"]
            |[Site "Buenos Aires"]
            |[Date "1955.??.??"]
            |[Round "18"]
            |[White "Maderna, Carlos"]
            |[Black "Palermo, Vicente"]
            |[Result "1-0"]
            |[ECO "A69"]
            |
            |1. d4 Nf6 2. c4 c5 3. d5 e6 4. Nc3 exd5 5. cxd5 d6
            |6. e4 g6 7. Nf3 Bg7 8. Be2 O-O 9. Nd2 Nbd7
            |10. O-O a6 11. a4 Re8 12. f4 Rb8 13. Re1 Nb6
            |14. Bf3 Nfd7 15. a5 Na8 16. Nc4 Qc7 17. e5 dxe5
            |18. d6 Qd8 19. Nd5 e4 20. Ne7+ Kf8 21. Rxe4 Nf6
            |22. Re1 Be6 23. Ne5 Ng8 24. f5 Bxf5 25. Bg5 f6
            |26. Qd5 fxe5 27. Qxg8+ 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(17, 17),
        reviewGroup = "B:static_weakness_fixation",
        intendedVerdict = "screen_only",
        validationNote =
          "Natural static-weakness authority candidate from public ModernBenoni6e4 PGN: exact ply 17 Nd2 must materialize exact_target_fixation on d6 on the engine top line before matrix admission."
      ),
      SourceCandidate(
        id = "source-aronian-andreikin-2014-defender-trade",
        gameName = "Aronian-Andreikin, FIDE Candidates 2014",
        sourceUrl = "https://en.chessbase.com/post/candidates-rd6-spoilt-opportunities",
        pgn =
          """[Event "FIDE Candidates Tournament 2014"]
            |[Site "Khanty-Mansiysk"]
            |[Date "2014.03.19"]
            |[Round "6"]
            |[White "Aronian, Levon"]
            |[Black "Andreikin, Dmitry"]
            |[Result "1/2-1/2"]
            |[ECO "A11"]
            |[WhiteElo "2830"]
            |[BlackElo "2709"]
            |
            |1. c4 c6 2. Nf3 d5 3. g3 dxc4 4. Bg2 Nd7
            |5. O-O Ngf6 6. Qc2 Nb6 7. Na3 Be6 8. Ne5 Qd4
            |9. Nxc6 bxc6 10. Bxc6+ Kd8 11. Nb5 Qc5 12. Bxa8 Qxb5
            |13. Bg2 Bd7 14. b3 e5 15. Rb1 cxb3 16. Rxb3 Qxe2
            |17. Ba3 Bxa3 18. Rxa3 Qc4 19. Qb1 Ke7 20. Rxa7 Qd4
            |21. Rb7 Na4 22. Rc1 Rd8 23. h3 Kf8 24. Qb3 e4
            |25. Rc4 Qd5 26. Qb4+ Kg8 27. Rd4 Qc6 28. Rbxd7 Nxd7
            |29. Qxa4 Qxa4 30. Rxa4 Nf8 31. Rxe4 Rxd2 32. a4 Ra2
            |33. Bf3 g6 34. Kg2 Ne6 35. Rc4 Kg7 36. Bd5 Kf6
            |37. Re4 Ra3 38. Bxe6 fxe6 39. Rf4+ Ke7 40. h4 h5
            |41. Re4 Kf7 42. Kf1 Ra2 43. Ke1 Kf6 44. Kd1 Ke7
            |45. f4 Ra3 46. Kc2 Rxg3 47. Rd4 Re3 48. Kb2 e5 1/2-1/2
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(33, 33),
        reviewGroup = "C:defender_trade",
        intendedVerdict = "screen_only",
        validationNote =
          "ChessBase notes 17.Ba3 as clearing away a defender; exact intake must prove the exchange removes a local defender on the engine top line before matrix admission."
      ),
      SourceCandidate(
        id = "source-bad-piece-liquidation-pilot",
        gameName = "Bad-piece liquidation exact pilot",
        sourceUrl = "https://example.invalid/bad-piece-liquidation-pilot.pgn",
        pgn =
          """[Event "Bad piece liquidation pilot"]
            |[Site "?"]
            |[Date "2026.01.01"]
            |[Round "?"]
            |[White "White"]
            |[Black "Black"]
            |[Result "*"]
            |[SetUp "1"]
            |[FEN "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1"]
            |
            |1. Ba3 Kf7 2. Bxf8 Kxf8 *
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(1, 1),
        reviewGroup = "C:bad_piece_liquidation",
        intendedVerdict = "screen_only",
        validationNote =
          "Exact bad-piece liquidation pilot: admit only if 1.Ba3 Kf7 2.Bxf8 Kxf8 is engine-backed on the same branch and materializes the bounded SupportedLocal bad_piece_liquidation packet."
      ),
      SourceCandidate(
        id = "source-capablanca-golombek-1939",
        gameName = "Capablanca-Golombek, Margate 1939",
        sourceUrl = "https://www.chessgames.com/perl/chessgame?comp=1&gid=1004963",
        pgn =
          """[Event "Margate"]
            |[Site "Margate ENG"]
            |[Date "1939.04.19"]
            |[Round "7"]
            |[White "Jose Raul Capablanca"]
            |[Black "Harry Golombek"]
            |[Result "1-0"]
            |[ECO "E34"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. Qc2 d5 5. cxd5 exd5
            |6. Bg5 c6 7. e3 Nbd7 8. Bd3 h6 9. Bh4 O-O 10. Nf3 Re8
            |11. O-O Be7 12. Bg3 Nf8 13. h3 Be6 14. Rab1 Nh5 15. Bh2 g6
            |16. Ne5 Ng7 17. b4 Bf5 18. Na4 Bxd3 19. Qxd3 Nd7
            |20. Rfc1 Nxe5 21. Bxe5 Bd6 22. Bxd6 Qxd6 23. b5 cxb5
            |24. Qxb5 Ne6 25. Nc3 Red8 26. Qxb7 Qa3 27. Nxd5 Qxa2
            |28. Nb4 Qa4 29. Nc6 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(33, 47),
        reviewGroup = "B:carlsbad_fixed_target",
        intendedVerdict = "screen_only",
        validationNote = "Identify the post-b5 exact FEN; verify the c6/d5 target is not a transient release."
      ),
      SourceCandidate(
        id = "source-botvinnik-vidmar-1936",
        gameName = "Botvinnik-Vidmar, Nottingham 1936",
        sourceUrl = "https://www.chessgames.com/perl/chessgame?gid=1008258",
        pgn =
          """[Event "Nottingham"]
            |[Site "Nottingham ENG"]
            |[Date "1936.08.25"]
            |[Round "13"]
            |[White "Mikhail Botvinnik"]
            |[Black "Milan Vidmar"]
            |[Result "1-0"]
            |[ECO "D60"]
            |
            |1. c4 e6 2. Nf3 d5 3. d4 Nf6 4. Nc3 Be7 5. Bg5 O-O
            |6. e3 Nbd7 7. Bd3 c5 8. O-O cxd4 9. exd4 dxc4
            |10. Bxc4 Nb6 11. Bb3 Bd7 12. Qd3 Nbd5 13. Ne5 Bc6
            |14. Rad1 Nb4 15. Qh3 Bd5 16. Nxd5 Nbxd5 17. f4 Rc8
            |18. f5 exf5 19. Rxf5 Qd6 20. Nxf7 Rxf7 21. Bxf6 Bxf6
            |22. Rxd5 Qc6 23. Rd6 Qe8 24. Rd7 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(20, 34),
        reviewGroup = "C:iqp_simplification",
        intendedVerdict = "screen_only",
        validationNote = "Locate the stabilized IQP phase and reject if the truth is tactical breakthrough rather than simplification."
      ),
      SourceCandidate(
        id = "source-kramnik-anand-2001",
        gameName = "Kramnik-Anand, Dortmund 2001",
        sourceUrl = "https://www.chessgames.com/perl/chessgame?gid=1173387",
        pgn =
          """[Event "Dortmund Sparkassen"]
            |[Site "Dortmund GER"]
            |[Date "2001.07.21"]
            |[Round "9"]
            |[White "Vladimir Kramnik"]
            |[Black "Viswanathan Anand"]
            |[Result "1-0"]
            |[ECO "D27"]
            |
            |1. d4 d5 2. c4 dxc4 3. Nf3 e6 4. e3 Nf6 5. Bxc4 c5
            |6. O-O a6 7. Bb3 cxd4 8. exd4 Nc6 9. Nc3 Be7
            |10. Bg5 O-O 11. Qd2 Na5 12. Bc2 b5 13. Qf4 Ra7
            |14. Rad1 Bb7 15. d5 Bxd5 16. Nxd5 exd5 17. Qh4 h5
            |18. Rfe1 Nc6 19. g4 Qd6 20. gxh5 Qb4 21. h6 Qxh4
            |22. Nxh4 Ne4 23. hxg7 Rc8 24. Bxe7 Nxe7 25. Bxe4 dxe4
            |26. Rxe4 Kxg7 27. Rd6 Rc5 28. Rg4+ Kh7 29. Nf3 Ng6
            |30. Ng5+ Kg7 31. Nxf7 Rxf7 32. Rdxg6+ Kh7
            |33. R6g5 Rxg5 34. Rxg5 Rc7 35. a3 b4 36. axb4 Rc1+
            |37. Kg2 Rb1 38. Ra5 Rxb2 39. Ra4 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(27, 32),
        reviewGroup = "C:tactical_first_blocker",
        intendedVerdict = "reject_tactical_first",
        validationNote = "The d5 break must stay tactical-first unless engine replay proves a bounded simplification owner."
      ),
      SourceCandidate(
        id = "source-tartakower-capablanca-1924",
        gameName = "Tartakower-Capablanca, New York 1924",
        sourceUrl = "https://www.chessgames.com/perl/chessgame?gid=1076242",
        pgn =
          """[Event "New York"]
            |[Site "New York, NY USA"]
            |[Date "1924.04.12"]
            |[Round "19"]
            |[White "Savielly Tartakower"]
            |[Black "Jose Raul Capablanca"]
            |[Result "0-1"]
            |[ECO "C33"]
            |
            |1. e4 e5 2. f4 exf4 3. Be2 d5 4. exd5 Nf6 5. c4 c6
            |6. d4 Bb4+ 7. Kf1 cxd5 8. Bxf4 dxc4 9. Bxb8 Nd5
            |10. Kf2 Rxb8 11. Bxc4 O-O 12. Nf3 Nf6 13. Nc3 b5
            |14. Bd3 Ng4+ 15. Kg1 Bb7 16. Bf5 Bxf3 17. gxf3 Ne3
            |18. Bxh7+ Kh8 19. Qd3 Bxc3 20. bxc3 Nd5 21. Be4 Nf4
            |22. Qd2 Qh4 23. Kf1 f5 24. Bc6 Rf6 25. d5 Rd8
            |26. Rd1 Rxc6 27. dxc6 Rxd2 28. Rxd2 Ne6 29. Rd6 Qc4+
            |30. Kg2 Qe2+ 0-1
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(17, 22),
        reviewGroup = "C:tactical_first_blocker",
        intendedVerdict = "reject_tactical_first",
        validationNote = "Replay after Bxb8 and validate that ...Nd5 owns the surface tactically."
      ),
      SourceCandidate(
        id = "source-alekhine-bogoljubow-1936-iqp-inducement",
        gameName = "Alekhine-Bogoljubow, Nottingham 1936",
        sourceUrl = "https://www.pgnmentor.com/events/Nottingham1936.pgn",
        pgn =
          """[Event "Nottingham"]
            |[Site "Nottingham"]
            |[Date "1936.??.??"]
            |[Round "?"]
            |[White "Alekhine, Alexander"]
            |[Black "Bogoljubow, Efim"]
            |[Result "1-0"]
            |[ECO "D16"]
            |
            |1. Nf3 d5 2. d4 Nf6 3. c4 c6 4. Nc3 dxc4 5. a4 e6
            |6. e4 Bb4 7. e5 Ne4 8. Qc2 Qd5 9. Be2 c5
            |10. O-O Nxc3 11. bxc3 cxd4 12. Nxd4 Bc5 13. Nf3 Nd7
            |14. Rd1 Qc6 15. Bxc4 O-O 16. Ng5 g6 17. Bb5 Qc7
            |18. Ne4 Be7 19. f4 Nc5 20. Nf6+ Bxf6 21. exf6 Bd7
            |22. Be3 Bxb5 23. axb5 Nd7 24. g3 Nxf6 25. Bd4 Nd7
            |26. Qf2 b6 27. Re1 Qc4 28. Rab1 Rac8 29. Qe3 Rfe8
            |30. Qf3 f6 31. Rb4 Qc7 32. Rb2 Re7 33. Rbe2 Kf7
            |34. g4 Rce8 35. g5 fxg5 36. f5 Qf4 37. fxe6+ Rxe6
            |38. Qd5 Nf6 39. Bxf6 Qg4+ 40. Rg2 Qf5 41. Be5 Kg8
            |42. Rf2 Qg4+ 43. Kh1 h5 44. Rg1 Qh4 45. Rf6 Kh7
            |46. Rxe6 Rxe6 47. Qd7+ 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(20, 20),
        reviewGroup = "C:iqp_inducement",
        intendedVerdict = "screen_only",
        validationNote =
          "Natural IQP authority candidate from public Nottingham 1936 PGN: exact ply 20 ...Nxc3 must induce an opponent isolated central pawn on the engine top line before matrix admission."
      ),
      SourceCandidate(
        id = "source-salov-ljubojevic-1992-simplification-window",
        gameName = "Salov-Ljubojevic, Linares 1992",
        sourceUrl = "https://www.pgnmentor.com/events/Linares1992.pgn",
        pgn =
          """[Event "Linares"]
            |[Site "Linares"]
            |[Date "1992.??.??"]
            |[Round "6"]
            |[White "Salov, Valery"]
            |[Black "Ljubojevic, Ljubomir"]
            |[Result "1-0"]
            |[WhiteElo "2655"]
            |[BlackElo "2610"]
            |[ECO "E18"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. g3 Be7 5. Bg2 O-O
            |6. O-O b6 7. Nc3 Bb7 8. cxd5 exd5 9. Bf4 Na6
            |10. Qc2 Re8 11. Rad1 Bd6 12. Be5 Qe7 13. a3 Rad8
            |14. Nb5 Bxe5 15. Nxe5 Ra8 16. e3 c5 17. f4 cxd4
            |18. exd4 Ne4 19. Qb3 Red8 20. Rfe1 Nc7 21. Nxf7 Qxf7
            |22. Nxc7 Rac8 23. Nb5 Ba6 24. Nc3 Bc4 25. Qc2 Nxc3
            |26. bxc3 b5 27. Bh3 Rc6 28. Re5 Re8 29. Rde1 Rxe5
            |30. Rxe5 Rh6 31. Bf5 Ra6 32. Bxh7+ Kh8 33. Bd3 Bxd3
            |34. Qxd3 Rxa3 35. Qxb5 Rxc3 36. Qxd5 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(71, 71),
        reviewGroup = "C:simplification_window",
        intendedVerdict = "screen_only",
        validationNote =
          "Natural simplification-window authority candidate from public Linares 1992 PGN: exact ply 71 Qxd5 must materialize a SimplificationWindow packet on the engine top line before matrix admission."
      ),
      SourceCandidate(
        id = "source-najdorf-sergeant-1939-iqp-inducement",
        gameName = "Najdorf-Sergeant, Margate 1939",
        sourceUrl = "https://www.pgnmentor.com/events/Margate1939.pgn",
        pgn =
          """[Event "Margate"]
            |[Site "Margate"]
            |[Date "1939.??.??"]
            |[Round "9"]
            |[White "Najdorf, Miguel"]
            |[Black "Sergeant, Edward G"]
            |[Result "1-0"]
            |[ECO "D61"]
            |
            |1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. Bg5 Be7 5. e3 Nbd7
            |6. Qc2 c5 7. Nf3 O-O 8. Rd1 cxd4 9. Nxd4 Nc5
            |10. cxd5 Nxd5 11. Bxe7 Qxe7 12. Nxd5 exd5 13. Be2 Be6
            |14. O-O Rfc8 15. Bf3 Nd7 16. Qb3 Qc5 17. Nxe6 fxe6
            |18. Bxd5 Nf8 19. Bxe6+ Nxe6 20. Qxe6+ Kh8 21. Rd7 Rc6
            |22. Qf7 Qg5 23. f4 Qg4 24. Rfd1 h6 25. f5 Kh7
            |26. h3 Qg5 27. Qe7 Rf6 28. h4 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(23, 23),
        reviewGroup = "C:iqp_inducement",
        intendedVerdict = "screen_only",
        validationNote =
          "Natural IQP authority candidate from public Margate 1939 PGN: exact ply 23 Nxd5 must induce an opponent isolated central pawn on the engine top line before matrix admission."
      ),
      SourceCandidate(
        id = "source-carlsen-anand-2014-g6",
        gameName = "Carlsen-Anand, World Championship 2014 game 6",
        sourceUrl = "https://www.chessgames.com/perl/chessgame?gid=1778864",
        pgn =
          """[Event "Carlsen - Anand World Championship Match"]
            |[Site "Sochi RUS"]
            |[Date "2014.11.15"]
            |[Round "6"]
            |[White "Magnus Carlsen"]
            |[Black "Viswanathan Anand"]
            |[Result "1-0"]
            |[ECO "B41"]
            |
            |1. e4 c5 2. Nf3 e6 3. d4 cxd4 4. Nxd4 a6 5. c4 Nf6
            |6. Nc3 Bb4 7. Qd3 Nc6 8. Nxc6 dxc6 9. Qxd8+ Kxd8
            |10. e5 Nd7 11. Bf4 Bxc3+ 12. bxc3 Kc7 13. h4 b6
            |14. h5 h6 15. O-O-O Bb7 16. Rd3 c5 17. Rg3 Rag8
            |18. Bd3 Nf8 19. Be3 g6 20. hxg6 Nxg6 21. Rh5 Bc6
            |22. Bc2 Kb7 23. Rg4 a5 24. Bd1 Rd8 25. Bc2 Rdg8
            |26. Kd2 a4 27. Ke2 a3 28. f3 Rd8 29. Ke1 Rd7
            |30. Bc1 Ra8 31. Ke2 Ba4 32. Be4+ Bc6 33. Bxg6 fxg6
            |34. Rxg6 Ba4 35. Rxe6 Rd1 36. Bxa3 Ra1 37. Ke3 Bc2
            |38. Re7+ 1-0
            |""".stripMargin.trim,
        candidatePlyRange = CandidatePlyRange(15, 18),
        reviewGroup = "C:queen_trade_boundary",
        intendedVerdict = "screen_only",
        validationNote = "Validate the queenless branch separately from the later missed tactic; do not promote from source narrative alone."
      )
    )

  val all: List[SourceCandidate] =
    fixedBatch.flatMap {
      case row if row.id == "source-capablanca-golombek-1939" =>
        List(
          row,
          row.copy(
            id = "source-capablanca-golombek-1939-bad-piece-liquidation",
            candidatePlyRange = CandidatePlyRange(43, 43),
            reviewGroup = "C:bad_piece_liquidation",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural bad-piece liquidation authority candidate: exact ply 43 Bxd6 must clear the bad bishop on the engine top line before matrix admission."
          ),
          row.copy(
            id = "source-capablanca-golombek-1939-iqp-inducement",
            candidatePlyRange = CandidatePlyRange(45, 45),
            reviewGroup = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 45 b4-b5 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-evans-opsahl-1950" =>
        List(
          row,
          row.copy(
            id = "source-evans-opsahl-1950-iqp-inducement",
            candidatePlyRange = CandidatePlyRange(33, 33),
            reviewGroup = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 33 Rc1 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-karpov-andersson-1975-hedgehog-break-screen" =>
        List(
          row,
          row.copy(
            id = "source-karpov-andersson-1975-iqp-inducement",
            candidatePlyRange = CandidatePlyRange(49, 49),
            reviewGroup = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 49 cxd5 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-maderna-palermo-1955-a6-a5-break-prevention" =>
        List(
          row,
          row.copy(
            id = "source-maderna-palermo-1955-central-break-timing",
            candidatePlyRange = CandidatePlyRange(33, 33),
            reviewGroup = "A:central_break_timing",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural central-break authority candidate: exact ply 33 e4-e5 must keep the direct central-break geometry and timing gap on the admitted PV before release."
          ),
          row.copy(
            id = "source-maderna-palermo-1955-central-break-prep-review",
            candidatePlyRange = CandidatePlyRange(31, 31),
            reviewGroup = "A:central_break_timing",
            intendedVerdict = "screen_only",
            validationNote =
              "Plan-only central-break review candidate: exact ply 31 Nc4 leaves the e5 break visible but has no direct board link, so it must never release."
          )
        )
      case row if row.id == "source-camara-bazan-1960-b7-b5-break-prevention" =>
        List(
          row,
          row.copy(
            id = "source-camara-bazan-1960-d5-color-complex-squeeze",
            candidatePlyRange = CandidatePlyRange(27, 27),
            reviewGroup = "A:flank_clamp",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural flank-clamp authority candidate: exact ply 27 Bb5 must materialize a color_complex_squeeze probe on d5 on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-pfleger-maalouf-1961-a6-a5-break-prevention" =>
        List(
          row,
          row.copy(
            id = "source-pfleger-maalouf-1961-d5-color-complex-squeeze",
            candidatePlyRange = CandidatePlyRange(33, 33),
            reviewGroup = "A:flank_clamp",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural flank-clamp authority candidate: exact ply 33 a5 must materialize a color_complex_squeeze probe on d5 on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-botvinnik-vidmar-1936" =>
        List(
          row,
          row.copy(
            id = "source-botvinnik-vidmar-1936-iqp-multipv-screen",
            candidatePlyRange = CandidatePlyRange(31, 31),
            reviewGroup = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Negative IQP screen: exact ply 31 can materialize an IQP packet, but it must stay non-authority unless the played move is engine top PV."
          ),
          row.copy(
            id = "source-botvinnik-vidmar-1936-simplification-window",
            candidatePlyRange = CandidatePlyRange(31, 31),
            reviewGroup = "C:simplification_window",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural simplification-window authority candidate: exact ply 31 Nxd5 must keep the same local edge on d5 on a near-top engine branch before matrix admission."
          ),
          row.copy(
            id = "source-botvinnik-vidmar-1936-iqp-opening-inducement",
            candidatePlyRange = CandidatePlyRange(16, 16),
            reviewGroup = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 16 ...cxd4 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          ),
          row.copy(
            id = "source-botvinnik-vidmar-1936-flank-clamp",
            candidatePlyRange = CandidatePlyRange(25, 25),
            reviewGroup = "A:flank_clamp",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural flank-clamp authority candidate: exact ply 25 Ne5 must materialize a color_complex_squeeze probe on the engine top line before matrix admission."
          ),
          row.copy(
            id = "source-botvinnik-vidmar-1936-e4-color-complex-squeeze",
            candidatePlyRange = CandidatePlyRange(30, 30),
            reviewGroup = "A:flank_clamp",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural flank-clamp authority candidate: exact ply 30 ...Bxd5 must materialize a color_complex_squeeze probe on e4 before matrix admission."
          )
        )
      case row if row.id == "source-kramnik-anand-2001" =>
        List(
          row,
          row.copy(
            id = "source-kramnik-anand-2001-iqp-opening-inducement",
            candidatePlyRange = CandidatePlyRange(14, 14),
            reviewGroup = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 14 ...cxd4 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-carlsen-anand-2014-g6" =>
        List(
          row.copy(candidatePlyRange = CandidatePlyRange(15, 15)),
          row.copy(
            id = "source-carlsen-anand-2014-g6-queen-trade-completion",
            candidatePlyRange = CandidatePlyRange(17, 17),
            reviewGroup = "C:queen_trade_boundary",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural queen-trade completion authority candidate: exact ply 17 Qxd8+ must complete the queenless-branch shield on the engine top line before matrix admission."
          )
        )
      case row => List(row)
    }
