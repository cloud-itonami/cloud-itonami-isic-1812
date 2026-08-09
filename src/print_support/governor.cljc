(ns print-support.governor
  "PrintSupportGovernor — the independent safety/traceability layer for
  the ISIC-1812 independent print-support-services actor. Wired as its
  own `:govern` node in `print-support.actor`'s StateGraph, downstream of
  `:advise` — the Advisor has no notion of client provenance, equipment
  safety scope, or quality-verification risk, so this MUST be a separate
  system able to reject a proposal (itonami actor pattern, per
  ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance  — the request's client must be registered.
    2. no-direct-actuation  — proposal :effect must be :propose
       (no direct equipment control or delivery release).
    3. prepress craft      — when `:op` is `:plan-prepress` /
       `:prepress/plan`, any seihan blocking finding (via
       `print-support.prepress/plan`) is a hard hold. Geometry is not
       invented here; seihan owns plates/imposition.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. :op :flag-quality-defect — quality issues always escalate.
    5. :op :coordinate-delivery — final delivery release always escalates
       (high-risk job release).
    6. low confidence (< `confidence-floor`)."
  (:require [print-support.store :as store]
            [print-support.prepress :as prepress]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:flag-quality-defect :coordinate-delivery})

(defn- hard-violations [{:keys [proposal]} client-record]
  (cond-> []
    (nil? client-record)
    (conj {:rule :no-client :detail "未登録 client"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-direct-actuation
           :detail "effect は :propose のみ許可（直接装置制御・納期release禁止）"})))

(defn- prepress-assessment
  "When the proposal is a prepress plan op, run seihan via
  `print-support.prepress/plan`. Returns
  `{:violations [...] :prepress <plan result or nil>}`.
  Job map is taken from proposal first, then request (advisor may put
  it either place)."
  [request proposal]
  (if-not (prepress/plan-op? (:op proposal))
    {:violations [] :prepress nil}
    (let [job (or (:job proposal) (:job request) (:prepress-job proposal))
          result (prepress/plan job)]
      {:violations (or (:violations result) [])
       :prepress result})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `print-support.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool
    :prepress <prepress plan result or nil>}`.

  `:prepress` is the full craft result when the op is a prepress plan
  (so `:commit` can attach summary without re-deriving geometry)."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        base-hard (hard-violations {:proposal proposal} client-record)
        {:keys [violations prepress]} (prepress-assessment request proposal)
        hard (into (vec base-hard) violations)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))
     :prepress prepress}))
