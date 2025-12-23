package chess.analysis

import chess.analysis.AnalysisTypes.{RichTag, TagCategory}

object SalienceSelector:

  case class Policy(
    maxTags: Int = 5,
    forcedCategories: Set[TagCategory] = Set(TagCategory.Tactic, TagCategory.Endgame), // Critical categories
    minScore: Double = 0.1
  )

  def select(allTags: List[RichTag], policy: Policy = Policy()): List[RichTag] =
    // Filter by score
    val candidates = allTags.filter(_.score.abs >= policy.minScore)
    
    // Prioritize forced categories
    val (critical, others) = candidates.partition(t => policy.forcedCategories.contains(t.category))
    
    // Sort critical by score descending
    val sortedCritical = critical.sortBy(-_.score.abs)
    
    // Sort others by score descending
    val sortedOthers = others.sortBy(-_.score.abs)
    
    // Combine
    val selected = (sortedCritical ++ sortedOthers).take(policy.maxTags)
    
    selected

  // Helper to re-rank based on narrative context (e.g. if we are in endgame, prioritize endgame tags)
  def contextAwareSelect(allTags: List[RichTag], phase: String): List[RichTag] =
    val basePolicy = Policy()
    val adjustedPolicy = phase match
      case "endgame" => basePolicy.copy(forcedCategories = basePolicy.forcedCategories + TagCategory.Endgame)
      case _ => basePolicy
      
    select(allTags, adjustedPolicy)
