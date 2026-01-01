package lila.llm

import scala.util.Random

object NarrativeTemplates {

  // Deterministic random selection based on mixed seed
  def select(options: Seq[String], seed: Int): String = {
    val rand = new Random(seed)
    options(rand.nextInt(options.size))
  }
  
  // Issue #3 Fix: Mix multiple factors for better variability
  def mixSeed(base: Int, ply: Int, extras: Int*): Int = {
    val combined = base * 31 + ply * 17 + extras.sum * 7
    combined.abs
  }

  // 4. Narrative Styles
  enum Style:
    case Book, Coach, Dramatic

  // 0. Game-Level Templates
  def gameIntro(white: String, black: String, event: String, date: String, result: String): String =
    s"In this encounter between **$white** and **$black** ($event, $date), the game concluded with **$result**. Let's examine the critical moments."

  def gameConclusion(winner: Option[String], themes: List[String]): String =
    winner match {
      case Some(w) => s"**$w** prevailed, capitalizing on key turning points. The game featured themes such as ${themes.mkString(", ")}."
      case None => s"The game ended in a draw. Both sides missed opportunities to tip the balance, with themes like ${themes.mkString(", ")} emerging."
    }

  // 1. Position Intro Templates (Style-Aware)
  def intro(nature: String, tension: Double, ply: Int, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Coach => Seq(
        // Educational / Direct
        s"Let's pause and assess. The position is $nature, and the tension is ${"%.1f".format(tension)}. What would you do?",
        s"Look closely. We are in a $nature struggle (tension: ${"%.1f".format(tension)}). Can you spot the key idea?",
        s"Training point: This is a classic $nature structure. Note the tension level at ${"%.1f".format(tension)}.",
        s"Don't rush. The board currently favors a $nature approach. Tension is ${"%.1f".format(tension)}.",
        s"If you were sitting here, how would you handle this $nature situation? (Tension: ${"%.1f".format(tension)})",
        s"A critical moment for study. The game has settled into a $nature phase.",
        s"Understand the nature of the position: it is $nature.",
        s"Mastery requires handling these $nature positions with care.",
        s"The tension is ${"%.1f".format(tension)}. In such $nature positions, precision is everything.",
        s"Take a deep breath. It's a $nature grind."
      )
      case Style.Dramatic => Seq(
        // Emotional / High Stakes
        s"The board is ablaze! A $nature chaos with high tension (${"%.1f".format(tension)})!",
        s"Tension spikes to ${"%.1f".format(tension)} in this $nature thriller.",
        s"It's a $nature showdown. One mistake could be fatal.",
        s"A cauldron of pressure! The position screams '$nature'.",
        s"Walking a tightrope. This $nature battle leaves no room for error (Tension: ${"%.1f".format(tension)}).",
        s"Silence before the storm? No, the $nature conflict is deafening.",
        s"Every piece is trembling. A $nature nightmare requires nerves of steel.",
        s"Pure adrenaline. The tension is palpable at ${"%.1f".format(tension)}.",
        s"This isn't just a game; it's a $nature war.",
        s"Chaos reigns in this $nature spectacle!"
      )
      case _ => Seq(
        // Formal / Objective (Book)
        s"The position is fundamentally $nature (tension: ${"%.1f".format(tension)}).",
        s"We find ourselves in a $nature landscape, with a tension index of ${"%.1f".format(tension)}.",
        s"The board reflects a $nature struggles (tension: ${"%.1f".format(tension)}).",
        s"Strategic balance: $nature. Tension: ${"%.1f".format(tension)}.",
        s"Analysis reveals a $nature character to the current state.",
        s"The game continues as a $nature contest.",
        s"Structuring the assessment: we observe a $nature layout.",
        s"From a positional standpoint, this is $nature.",
        s"The tension metric (${"%.1f".format(tension)}) confirms a $nature dynamic.",
        s"We have reached a $nature middlegame phase."
      )
    }
    select(templates, ply)
  }

  // 5. Rhetorical Devices (Questions, Contrasts)
  def rhetoricalQuestion(plan: String, ply: Int, style: Style = Style.Book): String = {
     val questions = style match {
       case Style.Coach => Seq(
         s"Ask yourself: Is $plan really the best path?",
         s"A question for you: Can we afford to ignore $plan?",
         s"Consider this: Why is $plan relevant now?",
         s"What if we tried $plan? Is it premature?",
         s"Does $plan solve our main problem?"
       )
       case Style.Dramatic => Seq(
         s"Dare we attempt $plan?",
         s"Is $plan a stroke of genius or madness?",
         s"The burning question: Will $plan succeed?",
         s"Can the opponent survive $plan?",
         s"Is $plan the dagger we need?"
       )
       case _ => Seq(
         s"Is $plan feasible here?",
         s"The position suggests $plan.",
         s"We must evaluate the potential of $plan.",
         s"Does the geometry support $plan?"
       )
     }
     select(questions, ply)
  }

  // 2. Candidate Intro Templates
  val candidateIntros = Seq(
    "\n**CONSIDERED CANDIDATES (Deep Analysis):**",
    "\n**KEY ALTERNATIVES:**",
    "\n**CANDIDATE MOVES Breakdown:**",
    "\n**OPTIONS ON THE TABLE:**"
  )

  // 3. Fallback Nature Descriptions (when motifs are empty)
  def fallbackNature(nature: String, tension: Double, ply: Int): String = {
    val templates = Seq(
      s"A quiet $nature position. The tension (${"%.1f".format(tension)}) suggests careful maneuvering is required.",
      s"The game enters a $nature phase. With tension at ${"%.1f".format(tension)}, precision is key.",
      s"This $nature structure demands patience. Tension is low (${"%.1f".format(tension)}), favoring long-term plans.",
      s"In this $nature position, no immediate tactics dictate play. Tension: ${"%.1f".format(tension)}.",
      s"The $nature landscape requires positional understanding over quick strikes."
    )
    select(templates, ply)
  }
  
  // Issue #4 Fix: Tactics section rhetorical questions
  def tacticsQuestion(motifName: String, ply: Int, style: Style = Style.Book): String = {
    val questions = style match {
      case Style.Coach => Seq(
        s"Can you spot how $motifName is exploited?",
        s"What would you do to leverage the $motifName?",
        s"How should you respond if $motifName appears on the board?"
      )
      case Style.Dramatic => Seq(
        s"Will the $motifName decide the game?",
        s"Is this the moment $motifName turns everything around?",
        s"The $motifName looms – can the defense hold?"
      )
      case _ => Seq(
        s"The $motifName is a key factor.",
        s"Note the $motifName in this position."
      )
    }
    select(questions, ply)
  }
  // 6. Book-Style: Intent & Refutation
  def intent(move: String, plan: String, ply: Int, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Dramatic => Seq(
        s"$move! A bold choice aiming to $plan.",
        s"With $move, the intention is clear: $plan.",
        s"$move sets the stage for $plan."
      )
      case _ => Seq(
        s"$move aims for $plan.",
        s"$move, with the idea of $plan.",
        s"The goal of $move is $plan.",
        s"$move seeks $plan.",
        s"$move is driven by $plan."
      )
    }
    select(templates, ply)
  }

  def refutation(move: String, reply: String, outcome: String, ply: Int, style: Style = Style.Book): String = {
    val templates = style match {
      case Style.Dramatic => Seq(
        s"$move seems natural, but $reply is a killer blow ($outcome)!",
        s"Do not play $move! It crashes into $reply ($outcome).",
        s"$move is a tragic error due to $reply ($outcome)."
      )
      case _ => Seq(
        s"$move fails to $reply, which leads to $outcome.",
        s"$move runs into $reply ($outcome).",
        s"If $move, then $reply proves decisive ($outcome).",
        s"Alternatives like $move are met by $reply ($outcome)."
      )
    }
    select(templates, ply)
  }

  def drama(scoreDiff: Int, ply: Int): String = {
    val absDiff = scoreDiff.abs
    val adj = if (absDiff > 500) Seq("crushing", "decisive", "hopeless", "devastating")
              else if (absDiff > 200) Seq("significant", "clear", "dangerous", "unpleasant")
              else if (absDiff > 50) Seq("slight", "noticeable", "nagging", "tangible")
              else Seq("minimal", "negligible", "unclear", "balanced")
    select(adj, ply)
  }
}
