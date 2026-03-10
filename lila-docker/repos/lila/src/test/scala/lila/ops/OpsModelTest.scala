package lila.ops

import java.time.Instant

import lila.core.perm.Permission
import lila.core.user.BSONFields
import lila.core.user.UserTier
import lila.core.userId.UserId
import reactivemongo.api.bson.*

class OpsModelTest extends munit.FunSuite:

  test("managed role split and merge keep unmanaged roles intact"):
    val existing = List(
      Permission.Verified.dbKey,
      Permission.Tech.dbKey,
      Permission.Coach.dbKey,
      Permission.OpsViewer.dbKey
    )

    val split = OpsRoles.split(existing)
    assertEquals(split.bundle, Some(Permission.OpsViewer.dbKey))
    assertEquals(split.toggles, List(Permission.Coach.dbKey, Permission.Verified.dbKey))
    assertEquals(split.unmanaged, List(Permission.Tech.dbKey))

    val merged = OpsRoles.mergeManaged(
      existing = existing,
      bundle = Some(Permission.OpsAdmin.dbKey),
      toggles = List(Permission.Teacher.dbKey)
    )

    assertEquals(
      merged,
      List(
        Permission.Tech.dbKey,
        Permission.OpsAdmin.dbKey,
        Permission.Teacher.dbKey
      )
    )

  test("snapshot diff reports only changed fields"):
    val before = OpsMemberState(
      id = UserId("member-a"),
      username = "MemberA",
      email = Some("before@example.com"),
      enabled = true,
      tier = UserTier.Free,
      expiresAt = None,
      roles = List(Permission.OpsViewer.dbKey),
      createdAt = Instant.parse("2026-01-01T00:00:00Z"),
      seenAt = Some(Instant.parse("2026-01-02T00:00:00Z")),
      hasTwoFactor = false,
      hasPassword = false
    )
    val after = before.copy(
      email = Some("after@example.com"),
      enabled = false,
      tier = UserTier.Premium,
      expiresAt = Some(Instant.parse("2026-12-31T00:00:00Z")),
      roles = List(Permission.OpsManager.dbKey, Permission.Verified.dbKey),
      hasTwoFactor = true,
      hasPassword = true
    )

    val diff = OpsMemberSnapshot.diff(
      OpsMemberSnapshot.fromState(before),
      OpsMemberSnapshot.fromState(after)
    )

    assertEquals(
      diff.map(_.field),
      List("email", "enabled", "tier", "expiresAt", "roles", "hasTwoFactor", "hasPassword")
    )

  test("rollback update restores present fields and unsets absent fields"):
    val before = BSONDocument(
      BSONFields.enabled -> true,
      BSONFields.roles -> BSONArray(
        BSONString(Permission.OpsViewer.dbKey.value),
        BSONString(Permission.Verified.dbKey.value)
      ),
      BSONFields.totpSecret -> BSONBinary(Array[Byte](1, 2, 3), Subtype.GenericBinarySubtype)
    )

    val rollback = OpsRollback.userFields(
      before,
      List(BSONFields.enabled, BSONFields.email, BSONFields.roles, BSONFields.totpSecret)
    )

    val setDoc = rollback.getAsOpt[BSONDocument]("$set").get
    val unsetDoc = rollback.getAsOpt[BSONDocument]("$unset").get

    assertEquals(setDoc.getAsOpt[Boolean](BSONFields.enabled), Some(true))
    assertEquals(
      setDoc.getAsOpt[BSONArray](BSONFields.roles).map(_.values.collect { case BSONString(value) => value }.toList),
      Some(List(Permission.OpsViewer.dbKey.value, Permission.Verified.dbKey.value))
    )
    assert(setDoc.getAsOpt[BSONBinary](BSONFields.totpSecret).nonEmpty)
    assertEquals(unsetDoc.getAsOpt[String](BSONFields.email), Some(""))

  test("plan state distinguishes free active and expired members"):
    val now = Instant.parse("2026-03-10T00:00:00Z")

    val free = OpsMemberSummary(
      id = UserId("free-user"),
      username = "FreeUser",
      email = None,
      enabled = true,
      tier = UserTier.Free,
      expiresAt = None,
      roles = Nil,
      createdAt = now,
      seenAt = None,
      hasTwoFactor = false
    )
    val active = free.copy(
      id = UserId("active-user"),
      tier = UserTier.Premium,
      expiresAt = Some(Instant.parse("2026-04-01T00:00:00Z"))
    )
    val expired = free.copy(
      id = UserId("expired-user"),
      tier = UserTier.Pro,
      expiresAt = Some(Instant.parse("2026-03-01T00:00:00Z"))
    )

    assertEquals(free.planState(now), OpsPlanState.Free)
    assertEquals(active.planState(now), OpsPlanState.Active)
    assertEquals(expired.planState(now), OpsPlanState.Expired)

  test("ops bundles grant expected capabilities and remain visible in permission categories"):
    assert(Permission.OpsViewer.grants(Permission.OpsMemberRead))
    assert(Permission.OpsManager.grants(Permission.OpsMemberWrite))
    assert(Permission.OpsAdmin.grants(Permission.OpsMemberRoleGrant))
    assert(Permission.SuperAdmin.grants(Permission.OpsMemberAdvanced))

    val opsCategory = lila.security.Permission.categorized.toMap.getOrElse("Ops", Nil)
    assertEquals(
      opsCategory,
      List(
        Permission.OpsMemberRead,
        Permission.OpsMemberWrite,
        Permission.OpsMemberRoleGrant,
        Permission.OpsMemberAdvanced,
        Permission.OpsViewer,
        Permission.OpsManager,
        Permission.OpsAdmin
      )
    )
