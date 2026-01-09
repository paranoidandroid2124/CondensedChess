package lila.i18n

object trans:
  object site:
    object siteDescription:
      def txt(): String = "Chesstory - AI Chess Analysis"
    def importedByX(user: Any) = "Imported"
    object deleteThisImportedGame:
      def txt(): String = "Delete"
    object delete:
      def txt(): String = "Delete"
    def whiteIsVictorious: String = "White is victorious"
    def blackIsVictorious: String = "Black is victorious"
    def chess960StartPosition(p: Any) = "Chess960"
    trait Plural:
      def pluralSame(n: Int): String
    def nbStudies: Plural = new Plural:
      def pluralSame(n: Int) = s"$n studies"
    trait Txt:
      def txt: String
    def online: Txt = new Txt:
      def txt = "Online"
    def offline: Txt = new Txt:
      def txt = "Offline"
    def memberSince: String = "Member since"
    def lastSeenActive(s: Any): String = s"Last seen $s"
  def pageNotFound: String = "Page not found"
