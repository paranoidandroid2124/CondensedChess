package lila.ops

import java.time.Instant

import lila.core.perm.Permission
import lila.core.user.{ RoleDbKey, User, UserTier }
import lila.core.userId.UserId
import lila.db.dsl.Bdoc
import lila.security.UserSession

import play.api.libs.json.*
import reactivemongo.api.bson.*

enum OpsMemberAction:
  case StatusChanged
  case EmailChanged
  case PasswordResetSent
  case SessionsRevoked
  case SessionRevoked
  case TwoFactorCleared
  case PlanUpdated
  case RolesUpdated
  case PermissionsUpdated

  def label: String = this match
    case StatusChanged    => "Status changed"
    case EmailChanged     => "Email changed"
    case PasswordResetSent => "Password reset sent"
    case SessionsRevoked  => "All sessions revoked"
    case SessionRevoked   => "Session revoked"
    case TwoFactorCleared => "Two-factor cleared"
    case PlanUpdated      => "Plan updated"
    case RolesUpdated     => "Managed roles updated"
    case PermissionsUpdated => "Raw permissions updated"

object OpsMemberAction:
  given BSONHandler[OpsMemberAction] = BSONHandler.from[OpsMemberAction](
    {
      case BSONString(value) =>
        OpsMemberAction.values.find(_.toString == value).fold(
          scala.util.Failure(new IllegalArgumentException(s"Invalid ops action: $value"))
        )(scala.util.Success(_))
      case other =>
        scala.util.Failure(new IllegalArgumentException(s"Invalid ops action bson: $other"))
    },
    action => scala.util.Success(BSONString(action.toString))
  )

  given Writes[OpsMemberAction] = Writes(action => JsString(action.toString))

enum OpsPlanState:
  case Free
  case Active
  case Expired

  def label: String = this match
    case Free    => "Free"
    case Active  => "Active"
    case Expired => "Expired"

object OpsPlanState:
  def fromString(value: String): Option[OpsPlanState] =
    values.find(_.toString.equalsIgnoreCase(value.trim))

case class OpsMemberSearch(
    q: Option[String] = None,
    email: Option[String] = None,
    enabled: Option[Boolean] = None,
    tier: Option[UserTier] = None,
    planState: Option[OpsPlanState] = None,
    role: Option[RoleDbKey] = None,
    createdFrom: Option[Instant] = None,
    createdTo: Option[Instant] = None,
    seenFrom: Option[Instant] = None,
    seenTo: Option[Instant] = None,
    page: Int = 1
):
  def cleaned: OpsMemberSearch =
    copy(
      q = q.map(_.trim).filter(_.nonEmpty),
      email = email.map(_.trim).filter(_.nonEmpty),
      page = page.max(1)
    )

case class OpsMemberSummary(
    id: UserId,
    username: String,
    email: Option[String],
    enabled: Boolean,
    tier: UserTier,
    expiresAt: Option[Instant],
    roles: List[RoleDbKey],
    createdAt: Instant,
    seenAt: Option[Instant],
    hasTwoFactor: Boolean
):
  def planState(now: Instant = Instant.now()): OpsPlanState =
    if tier == UserTier.Free then OpsPlanState.Free
    else if expiresAt.exists(exp => !exp.isAfter(now)) then OpsPlanState.Expired
    else OpsPlanState.Active

object OpsMemberSummary:
  def fromUser(user: User): OpsMemberSummary =
    OpsMemberSummary(
      id = user.id,
      username = user.username.value,
      email = user.email.map(_.value),
      enabled = user.enabled.yes,
      tier = user.tier,
      expiresAt = user.expiresAt,
      roles = user.roles,
      createdAt = user.createdAt,
      seenAt = user.seenAt,
      hasTwoFactor = user.totpSecret.isDefined
    )

case class OpsMemberState(
    id: UserId,
    username: String,
    email: Option[String],
    enabled: Boolean,
    tier: UserTier,
    expiresAt: Option[Instant],
    roles: List[RoleDbKey],
    createdAt: Instant,
    seenAt: Option[Instant],
    hasTwoFactor: Boolean,
    hasPassword: Boolean
):
  def planState(now: Instant = Instant.now()): OpsPlanState =
    if tier == UserTier.Free then OpsPlanState.Free
    else if expiresAt.exists(exp => !exp.isAfter(now)) then OpsPlanState.Expired
    else OpsPlanState.Active

  def summary: OpsMemberSummary = OpsMemberSummary(
    id = id,
    username = username,
    email = email,
    enabled = enabled,
    tier = tier,
    expiresAt = expiresAt,
    roles = roles,
    createdAt = createdAt,
    seenAt = seenAt,
    hasTwoFactor = hasTwoFactor
  )

object OpsMemberState:
  def fromUser(user: User, hasPassword: Boolean): OpsMemberState =
    OpsMemberState(
      id = user.id,
      username = user.username.value,
      email = user.email.map(_.value),
      enabled = user.enabled.yes,
      tier = user.tier,
      expiresAt = user.expiresAt,
      roles = user.roles,
      createdAt = user.createdAt,
      seenAt = user.seenAt,
      hasTwoFactor = user.totpSecret.isDefined,
      hasPassword = hasPassword
    )

case class OpsMemberSearchPage(
    search: OpsMemberSearch,
    items: List[OpsMemberSummary],
    total: Int,
    pageSize: Int
):
  val page: Int = search.page.max(1)
  def totalPages: Int = math.max(1, math.ceil(total.toDouble / pageSize.toDouble).toInt)
  def hasPrev: Boolean = page > 1
  def hasNext: Boolean = page < totalPages
  def prevPage: Int = (page - 1).max(1)
  def nextPage: Int = (page + 1).min(totalPages)

case class OpsMemberSnapshot(
    username: String,
    email: Option[String],
    enabled: Boolean,
    tier: String,
    expiresAt: Option[Instant],
    roles: List[String],
    createdAt: Instant,
    seenAt: Option[Instant],
    hasTwoFactor: Boolean,
    hasPassword: Boolean
):
  private def renderInstant(value: Instant): String = value.toString

  def values: List[(String, Option[String])] = List(
    "username" -> Some(username),
    "email" -> email,
    "enabled" -> Some(enabled.toString),
    "tier" -> Some(tier),
    "expiresAt" -> expiresAt.map(renderInstant),
    "roles" -> Some(roles.sorted.mkString(", ")),
    "createdAt" -> Some(renderInstant(createdAt)),
    "seenAt" -> seenAt.map(renderInstant),
    "hasTwoFactor" -> Some(hasTwoFactor.toString),
    "hasPassword" -> Some(hasPassword.toString)
  )

object OpsMemberSnapshot:
  def fromState(state: OpsMemberState): OpsMemberSnapshot =
    OpsMemberSnapshot(
      username = state.username,
      email = state.email,
      enabled = state.enabled,
      tier = state.tier.name,
      expiresAt = state.expiresAt,
      roles = state.roles.map(_.value).sorted,
      createdAt = state.createdAt,
      seenAt = state.seenAt,
      hasTwoFactor = state.hasTwoFactor,
      hasPassword = state.hasPassword
    )

  def diff(before: OpsMemberSnapshot, after: OpsMemberSnapshot): List[OpsFieldDiff] =
    before.values.zip(after.values).collect:
      case ((field, beforeValue), (_, afterValue)) if beforeValue != afterValue =>
        OpsFieldDiff(field, beforeValue, afterValue)

case class OpsFieldDiff(field: String, before: Option[String], after: Option[String])

case class OpsDetailItem(key: String, value: String)

case class OpsMemberAuditEntry(
    _id: String,
    target: UserId,
    actor: UserId,
    action: OpsMemberAction,
    reason: String,
    actorIp: Option[String],
    actorUa: Option[String],
    before: OpsMemberSnapshot,
    after: OpsMemberSnapshot,
    diff: List[OpsFieldDiff],
    details: List[OpsDetailItem],
    at: Instant
)

object OpsMemberAuditEntry:
  import lila.user.BSONHandlers.given
  given BSONDocumentHandler[OpsFieldDiff] = Macros.handler
  given BSONDocumentHandler[OpsDetailItem] = Macros.handler
  given BSONDocumentHandler[OpsMemberSnapshot] = Macros.handler
  val bsonHandler: BSONDocumentHandler[OpsMemberAuditEntry] = Macros.handler
  given BSONDocumentHandler[OpsMemberAuditEntry] = bsonHandler

case class OpsAuditPage(
    target: UserId,
    entries: List[OpsMemberAuditEntry],
    page: Int,
    pageSize: Int,
    total: Int
):
  def totalPages: Int = math.max(1, math.ceil(total.toDouble / pageSize.toDouble).toInt)

case class UserPlanLedgerEntry(
    _id: String,
    user: UserId,
    actor: UserId,
    oldTier: UserTier,
    newTier: UserTier,
    oldExpiresAt: Option[Instant],
    newExpiresAt: Option[Instant],
    reason: String,
    source: String,
    at: Instant
)

object UserPlanLedgerEntry:
  import lila.user.BSONHandlers.given
  val bsonHandler: BSONDocumentHandler[UserPlanLedgerEntry] = Macros.handler
  given BSONDocumentHandler[UserPlanLedgerEntry] = bsonHandler

case class OpsPermissionPreview(
    target: OpsMemberState,
    beforeRoles: List[RoleDbKey],
    afterRoles: List[RoleDbKey],
    diff: List[OpsFieldDiff]
)

case class OpsManagedRoles(
    bundle: Option[RoleDbKey],
    toggles: List[RoleDbKey],
    unmanaged: List[RoleDbKey]
)

case class OpsMemberDetail(
    member: OpsMemberState,
    sessions: List[UserSession],
    recentAudit: List[OpsMemberAuditEntry],
    recentPlanLedger: List[UserPlanLedgerEntry],
    managedRoles: OpsManagedRoles
)

object OpsRoles:
  val opsBundles: List[RoleDbKey] = List(
    Permission.OpsViewer.dbKey,
    Permission.OpsManager.dbKey,
    Permission.OpsAdmin.dbKey
  )

  val managedToggles: List[RoleDbKey] = List(
    Permission.Coach.dbKey,
    Permission.Teacher.dbKey,
    Permission.Verified.dbKey,
    Permission.Beta.dbKey
  )

  val managedDailyRoles: List[RoleDbKey] = opsBundles ++ managedToggles

  def split(roles: List[RoleDbKey]): OpsManagedRoles =
    val bundle = roles.find(opsBundles.contains)
    val toggles = roles.filter(managedToggles.contains)
    val unmanaged = roles.filterNot(managedDailyRoles.contains)
    OpsManagedRoles(
      bundle = bundle,
      toggles = toggles.sortBy(_.value),
      unmanaged = unmanaged.sortBy(_.value)
    )

  def normalizedManagedRoles(bundle: Option[RoleDbKey], toggles: Iterable[RoleDbKey]): List[RoleDbKey] =
    bundle.filter(opsBundles.contains).toList ++ toggles.filter(managedToggles.contains).toList.distinct.sortBy(_.value)

  def mergeManaged(existing: List[RoleDbKey], bundle: Option[RoleDbKey], toggles: Iterable[RoleDbKey]): List[RoleDbKey] =
    split(existing).unmanaged ++ normalizedManagedRoles(bundle, toggles)

  def displayName(dbKey: RoleDbKey): String =
    Permission.ofDbKey(dbKey).fold(dbKey.value)(_.name)

  def rawRoleChoices: List[RoleDbKey] =
    Permission.all.toList.sortBy(_.name).map(_.dbKey)

object OpsRollback:

  def userFields(before: Bdoc, fields: Iterable[String]): Bdoc =
    val normalized = fields.toList.distinct
    val setDoc = BSONDocument.strict:
      normalized.flatMap: field =>
        before.get(field).map(field -> _)
    val unsetDoc = BSONDocument.strict:
      normalized.collect:
        case field if before.get(field).isEmpty => field -> BSONString("")

    BSONDocument.strict(
      List(
        Option.when(setDoc.elements.nonEmpty)("$set" -> setDoc),
        Option.when(unsetDoc.elements.nonEmpty)("$unset" -> unsetDoc)
      ).flatten
    )
