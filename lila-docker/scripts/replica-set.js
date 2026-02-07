const desired = {
    _id: 'rs0',
    members: [
        // Single voting member so dev works even if the secondary isn't running.
        { _id: 0, host: 'mongodb:27017', priority: 1, votes: 1 },
        // Optional secondary (non-voting) to avoid "No primary" when it's down.
        { _id: 1, host: 'mongodb_secondary:27017', priority: 0, votes: 0, hidden: true },
    ],
}

function normalizeConfig(cfg) {
    let changed = false
    cfg._id = desired._id

    const byHost = Object.fromEntries((cfg.members || []).map(m => [m.host, m]))

    // Ensure primary member exists and is voting.
    if (!byHost['mongodb:27017']) {
        cfg.members = [desired.members[0]]
        changed = true
    } else {
        const m0 = byHost['mongodb:27017']
        ;['votes', 'priority'].forEach(k => {
            if (m0[k] !== desired.members[0][k]) {
                m0[k] = desired.members[0][k]
                changed = true
            }
        })
    }

    // Ensure optional secondary exists but is non-voting.
    if (!byHost['mongodb_secondary:27017']) {
        cfg.members.push(desired.members[1])
        changed = true
    } else {
        const m1 = byHost['mongodb_secondary:27017']
        ;['votes', 'priority', 'hidden'].forEach(k => {
            if (m1[k] !== desired.members[1][k]) {
                m1[k] = desired.members[1][k]
                changed = true
            }
        })
    }

    if (changed) cfg.version = (cfg.version || 1) + 1
    return changed
}

function ensureReplicaSet() {
    try {
        rs.status()
    } catch (err) {
        rs.initiate(desired)
        return
    }

    const cfg = rs.conf()
    if (normalizeConfig(cfg)) {
        rs.reconfig(cfg, { force: true })
    }
}

function waitPrimary(maxAttempts, waitMs) {
    for (let i = 0; i < maxAttempts; i++) {
        try {
            const state = rs.status().myState
            // 1 = PRIMARY
            if (state === 1) return
        } catch (e) {
            // ignore and retry
        }
        sleep(waitMs)
    }
    throw new Error('No primary node is available')
}

ensureReplicaSet()
waitPrimary(20, 500)
