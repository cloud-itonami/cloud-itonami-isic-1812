(ns print-support.prepress
  "Paper prepress craft wire for ISIC-1812 — a thin pure wrapper over
  `cloud-itonami/seihan`.

  Plate order, trap, bleed, and imposition come ONLY from
  `seihan.core/plan`. This namespace never invents geometry numbers: it
  validates/forwards a job map, calls seihan, and maps blocking
  findings into the governor's hard-violation shape so the Print
  Support Governor can hold before any job record is committed.

  Seihan is paper commercial print prepress (CMYK/spot/bleed/trap/
  imposition). Garment underbase (白版) is `shirohan` and is not
  wired here."
  (:require [seihan.core :as seihan]))

(def plan-ops
  "Ops that mean 'run the paper prepress decision core'."
  #{:plan-prepress :prepress/plan})

(defn plan-op?
  "True when the proposal/request op is a prepress plan op."
  [op]
  (contains? plan-ops op))

(defn- finding->violation
  "Map one seihan finding into the governor hard-violation shape.
  `:rule` is stable for ledger/tests; `:kind` preserves seihan's
  finding identity (e.g. `:zero-pages`, `:missing-paper-size`)."
  [{:keys [kind note blocking?]}]
  {:rule :prepress-blocking-finding
   :kind kind
   :blocking? (boolean blocking?)
   :detail (or note (str "seihan blocking finding: " (pr-str kind)))})

(defn plan
  "Call `seihan.core/plan` on a job map. Returns:

    {:ok?        bool
     :plan       <full seihan result — plates/findings/imposition/spec>
     :summary    <seihan/summary — for ledger/SSoT>
     :violations [{:rule :prepress-blocking-finding :kind … :detail …} …]}

  Geometry fields (`:plates`, `:imposition`) are copied from seihan
  only. When the job is nil/empty, seihan still returns findings
  (missing pages/paper/colors) — we do not invent a default layout."
  [job]
  (let [result (seihan/plan (or job {}))
        blocking (filterv seihan/blocking? (:findings result))
        viols (mapv finding->violation blocking)]
    {:ok? (:ok? result)
     :plan result
     :summary (seihan/summary result)
     :violations viols}))

(defn blocking-kinds
  "Set of finding kinds seihan treats as blocking. Surfaced for tests
  so the wire does not re-declare seihan's hold table."
  []
  ;; Derive from seihan.blocking? on known kinds rather than copying
  ;; seihan's private set — if seihan grows a new blocking kind, a
  ;; dedicated test still pins behaviour via plan calls.
  (->> [:missing-pages :zero-pages :missing-paper-size
        :invalid-paper-size :negative-bleed :negative-trap
        :no-colors :too-many-colors :too-many-spot-colors]
       (filter #(seihan/blocking? {:kind %}))
       (set)))
