(ns print-support.store
  "SSoT for the ISIC-08 1812 independent print-support-services actor
  (pre-press / bindery services). Store is a protocol injected into the
  `print-support.actor` StateGraph — `MemStore` is the default,
  deterministic, zero-dep backend; a Datomic/kotoba-server-backed
  implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    client/shop  — a registered print-support client (pre-press shop,
                   bindery operator, or print service provider)
                   (:client-id, :name, :equipment-scope)
    job-record   — a committed service job record (logged output,
                   quality inspection, delivery coordination)
                   — written ONLY via commit-record!, never mutated in place
    ledger       — an append-only audit trail of every proposal/verdict/
                   disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (client [s client-id])
  (jobs-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (jobs-of [_ client-id] (filter #(= client-id (:client-id %)) (:jobs @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (commit-record! [s record]
    (swap! a update :jobs (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :jobs [] :ledger []} seed)))))
