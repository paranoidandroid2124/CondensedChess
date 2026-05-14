package lila.commentary.tools.claim

import lila.commentary.analysis.*
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
      family: String,
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
        family = "B:carlsbad_fixed_target",
        intendedVerdict = "screen_only",
        validationNote = "Replay around b4-b5xc6; promote only if engine PV proves target persistence and owner path."
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
        family = "B:static_weakness_fixation",
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
        family = "B:static_weakness_fixation",
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
        family = "C:defender_trade",
        intendedVerdict = "screen_only",
        validationNote =
          "ChessBase notes 17.Ba3 as clearing away a defender; exact intake must prove the exchange removes a local defender on the engine top line before matrix admission."
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
        family = "B:carlsbad_fixed_target",
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
        family = "C:iqp_simplification",
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
        family = "C:tactical_first_blocker",
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
        family = "C:tactical_first_blocker",
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
        family = "C:iqp_inducement",
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
        family = "C:simplification_window",
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
        family = "C:iqp_inducement",
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
        family = "C:queen_trade_boundary",
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
            id = "source-capablanca-golombek-1939-iqp-inducement",
            candidatePlyRange = CandidatePlyRange(45, 45),
            family = "C:iqp_inducement",
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
            family = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 33 Rc1 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-botvinnik-vidmar-1936" =>
        List(
          row,
          row.copy(
            id = "source-botvinnik-vidmar-1936-iqp-multipv-screen",
            candidatePlyRange = CandidatePlyRange(31, 31),
            family = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Negative IQP screen: exact ply 31 can materialize an IQP packet, but it must stay non-authority unless the played move is engine top PV."
          ),
          row.copy(
            id = "source-botvinnik-vidmar-1936-iqp-opening-inducement",
            candidatePlyRange = CandidatePlyRange(16, 16),
            family = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 16 ...cxd4 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row if row.id == "source-kramnik-anand-2001" =>
        List(
          row,
          row.copy(
            id = "source-kramnik-anand-2001-iqp-opening-inducement",
            candidatePlyRange = CandidatePlyRange(14, 14),
            family = "C:iqp_inducement",
            intendedVerdict = "screen_only",
            validationNote =
              "Natural IQP authority candidate: exact ply 14 ...cxd4 must induce an opponent isolated central pawn on the engine top line before matrix admission."
          )
        )
      case row => List(row)
    }
