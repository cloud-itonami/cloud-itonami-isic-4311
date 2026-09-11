(ns demolition.governor
  "Demolition Governor -- the independent compliance layer named in
  `blueprint.edn` (`:itonami.blueprint/governor :demolition-governor`)
  that earns the Demolition Advisor the right to commit. The LLM has no
  notion of demolition-safety law, whether a site's own recorded hazmat
  survey / demolition-notification filing actually satisfies its
  jurisdiction's requirements, or when a proposal has quietly drifted
  outside this actor's charter into equipment control or engineering-plan
  finalization, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD -- the demolition-safety analog of
  `cloud-itonami-isic-4211`'s Construction Governor.

  ## Scope -- this is a COORDINATION-ONLY actor

  This actor NEVER controls heavy equipment and NEVER finalizes a
  structural demolition sequence or engineering plan -- that authority is
  the licensed engineer / site supervisor's EXCLUSIVELY. Unlike
  `construction.governor`'s sibling checks (which gate SIX real-world
  actuation effects such as `:site/mark-alert-dispatched` or `:build/
  dispatch-placement`), every proposal this actor's Demolition Advisor
  can produce carries `:effect :propose` and NOTHING else -- committing a
  proposal here means 'this coordination artifact is now logged/
  scheduled/flagged/ordered', never 'a robot moved' or 'a demolition
  sequence is authorized to begin'. Checks 1-4 below encode this scope as
  STRUCTURAL, permanent HARD holds -- not policy that could be relaxed by
  a future phase, unlike checks 5-8, which are ordinary per-jurisdiction/
  ground-truth safety gates.

  Eight checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them.

    1. Unknown op                -- the proposal's `:op` is outside the
                                     CLOSED four-op allowlist
                                     (`:log-site-record` / `:schedule-
                                     demolition-operation` / `:flag-
                                     safety-concern` / `:order-supplies`).
                                     Permanent, structural.
    2. Effect is not :propose    -- ANY proposal whose `:effect` is not
                                     literally `:propose` is rejected,
                                     unconditionally. Defense-in-depth: a
                                     compromised/malfunctioning advisor
                                     can never slip a real-world actuation
                                     effect past this governor. Permanent,
                                     structural.
    3. Forbidden action class    -- a proposal whose `:value` carries an
                                     `:equipment-control?` / `:direct-
                                     actuation?` / `:finalizes-
                                     engineering-plan?` marker true is
                                     rejected, unconditionally -- even
                                     though this actor's own mock advisor
                                     never sets these, the governor checks
                                     independently so a compromised
                                     advisor gains nothing by trying.
                                     Permanent, structural, un-overridable
                                     by ANY human approval -- see README
                                     `Actuation`.
    4. Site not independently
       verified/registered        -- for `:schedule-demolition-operation`
                                     / `:flag-safety-concern` / `:order-
                                     supplies`, the site's own recorded
                                     `:site-verified?` ground-truth field
                                     (set only via a separately-committed
                                     `:log-site-record`, never trusted
                                     from the CURRENT proposal's own
                                     confidence) must be true.
    5. Legal-basis missing       -- for `:schedule-demolition-operation`,
                                     did the proposal cite an OFFICIAL
                                     source (`demolition.facts`), or
                                     invent one for an uncovered
                                     jurisdiction?
    6. Hazmat survey incomplete  -- for `:schedule-demolition-operation`,
                                     has the site's own recorded `:hazmat-
                                     survey-completed?` ground-truth field
                                     actually been set true?
    7. Notification lead time
       insufficient               -- for `:schedule-demolition-operation`
                                     in a `:quantitative`-threshold
                                     jurisdiction (JPN/USA), INDEPENDENTLY
                                     recompute whether the site's own
                                     recorded `:notification-lead-days-
                                     actual` meets its jurisdiction's
                                     regulatory minimum
                                     (`demolition.facts/notification-lead-
                                     insufficient?`) -- needs no proposal
                                     inspection at all. DELIBERATELY does
                                     not fire for `:qualitative`
                                     jurisdictions (DEU/EU) -- there is no
                                     numeric bright line to independently
                                     re-check there; the Demolition
                                     Governor's permanent high-stakes gate
                                     (check 7 below, the `high-stakes`
                                     set) already routes
                                     `:schedule-demolition-operation` to a
                                     human EVERY time regardless of
                                     jurisdiction, so this HARD check adds
                                     a bright-line floor only where the
                                     law itself gives one.
    8. Unresolved safety concern -- for `:schedule-demolition-operation`,
                                     the site's own recorded `:safety-
                                     concern-unresolved?` ground-truth
                                     field must be false. Evaluated off
                                     the STORE's ground truth, never the
                                     current proposal's own confidence --
                                     the same 'ground truth, not
                                     self-report' discipline `construction
                                     .governor/weather-still-exceeds-
                                     threshold-violations` established.
                                     Does NOT fire for `:flag-safety-
                                     concern` itself (that op ALWAYS
                                     escalates to a human regardless of
                                     its own content -- see check on
                                     `high-stakes` below and
                                     `demolition.phase` ns docstring).

  The confidence/high-stakes gate is SOFT: it asks a human to look, and
  the human may approve.

    - `:flag-safety-concern` is UNCONDITIONALLY a member of `high-stakes`
      -- it ALWAYS escalates to a human, at every phase, regardless of
      confidence or governor cleanliness. Surfacing a structural-
      instability/hazmat concern is exactly the kind of judgment this
      actor must never let auto-commit.
    - `:schedule-demolition-operation` is ALSO UNCONDITIONALLY a member
      of `high-stakes` -- proposing a demolition-phase schedule window
      coordinates potential heavy-equipment dispatch; it always needs a
      human's sign-off, never auto-commits at any phase (belt-and-
      suspenders with `demolition.phase`, which also never puts it in
      any phase's `:auto` set).
    - `:order-supplies` escalates when its own proposed
      `:value :cost-usd` exceeds `supply-order-cost-threshold-usd`, OR
      when confidence is below `confidence-floor` -- a soft, cost-scoped
      gate, NOT a permanent per-op membership in `high-stakes`."
  (:require [demolition.facts :as facts]
            [demolition.store :as store]))

(def confidence-floor 0.6)

(def supply-order-cost-threshold-usd
  "Above this, a `:order-supplies` proposal always escalates to a human,
  regardless of confidence -- see ns docstring."
  5000)

(def closed-op-allowlist
  #{:log-site-record :schedule-demolition-operation :flag-safety-concern :order-supplies})

(def high-stakes
  "Ops that ALWAYS escalate to a human when the governor is otherwise
  clean, at every phase, unconditionally. `:log-site-record` is
  deliberately NOT a member -- low-risk data logging/normalization, the
  same posture `construction.governor/high-stakes` gives `:site/intake`.
  `:order-supplies` is deliberately NOT a permanent member either -- its
  escalation is a SOFT, cost-scoped rule computed in `check` below, not a
  blanket 'always a human' rule."
  #{:flag-safety-concern :schedule-demolition-operation})

;; ----------------------------- checks -----------------------------

(defn- unknown-op-violations
  "The proposal's `:op` must be a member of the CLOSED four-op allowlist.
  Permanent, structural -- see ns docstring check 1."
  [{:keys [op]}]
  (when-not (contains? closed-op-allowlist op)
    [{:rule :unknown-op
      :detail (str op " はこのアクターの許可された4オペレーション（:log-site-record/"
                  ":schedule-demolition-operation/:flag-safety-concern/:order-supplies）"
                  "のいずれにも該当しない")}]))

(defn- effect-not-propose-violations
  "Every proposal from this actor's advisor must carry `:effect
  :propose` -- and nothing else. Permanent, structural -- see ns
  docstring check 2."
  [proposal]
  (when-not (= :propose (:effect proposal))
    [{:rule :effect-not-propose
      :detail (str "提案の:effectが:propose以外（" (pr-str (:effect proposal))
                  "）-- このアクターは提案のみで、実際の作動/確定を一切行わない")}]))

(defn- forbidden-action-class-violations
  "A proposal's `:value` must never carry an equipment-control /
  direct-actuation / engineering-plan-finalization marker. Permanent,
  structural, un-overridable by any human approval -- see ns docstring
  check 3."
  [proposal]
  (let [v (:value proposal)]
    (when (and (map? v)
               (or (true? (:equipment-control? v))
                   (true? (:direct-actuation? v))
                   (true? (:finalizes-engineering-plan? v))))
      [{:rule :forbidden-action-class
        :detail "重機の直接操作コマンド、または構造解体シーケンス/工学的計画の確定を伴う提案は恒久的に禁止（免許を持つ技術者/現場監督の専権事項）"}])))

(defn- site-not-verified-violations
  "For `:schedule-demolition-operation` / `:flag-safety-concern` /
  `:order-supplies`, the site's own recorded `:site-verified?`
  ground-truth field (set only via a separately-committed
  `:log-site-record`) must be true. See ns docstring check 4."
  [{:keys [op subject]} st]
  (when (contains? #{:schedule-demolition-operation :flag-safety-concern :order-supplies} op)
    (let [a (store/site st subject)]
      (when-not (true? (:site-verified? a))
        [{:rule :site-not-verified
          :detail (str subject " は現場記録が独立して検証・登録済み（:site-verified?）でない状態での提案")}]))))

(defn- legal-basis-missing-violations
  "For `:schedule-demolition-operation`, the proposal must cite an
  OFFICIAL source -- never invent a jurisdiction's hazmat-survey/
  demolition-notification requirements. See ns docstring check 5."
  [{:keys [op]} proposal]
  (when (= op :schedule-demolition-operation)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-legal-basis
          :detail "公式legal-basisの引用が無い提案は解体作業スケジュール提案として扱えない"}]))))

(defn- hazmat-survey-incomplete-violations
  "For `:schedule-demolition-operation`, the site's own recorded
  `:hazmat-survey-completed?` ground-truth field must be true. See ns
  docstring check 6."
  [{:keys [op subject]} st]
  (when (= op :schedule-demolition-operation)
    (let [a (store/site st subject)]
      (when-not (true? (:hazmat-survey-completed? a))
        [{:rule :hazmat-survey-incomplete
          :detail (str subject " は石綿等の事前調査（hazmat survey）が完了記録されていない状態での解体作業スケジュール提案")}]))))

(defn- notification-lead-time-insufficient-violations
  "For `:schedule-demolition-operation`, INDEPENDENTLY recompute whether
  the site's own recorded notification lead time meets its jurisdiction's
  regulatory minimum via `demolition.facts/notification-lead-
  insufficient?`. Fires ONLY when that returns `true` (a :quantitative
  jurisdiction confirmed insufficient) -- `:qualitative`/`nil` never trip
  this HARD check. See ns docstring check 7."
  [{:keys [op subject]} st]
  (when (= op :schedule-demolition-operation)
    (let [a (store/site st subject)]
      (when (true? (facts/notification-lead-insufficient? (:jurisdiction a) a))
        [{:rule :notification-lead-time-insufficient
          :detail (str subject " の解体届出リードタイム実測値（" (:notification-lead-days-actual a)
                      "日）が法定最低日数に不足")}]))))

(defn- unresolved-safety-concern-violations
  "For `:schedule-demolition-operation`, the site's own recorded
  `:safety-concern-unresolved?` ground-truth field must be false. See ns
  docstring check 8."
  [{:keys [op subject]} st]
  (when (= op :schedule-demolition-operation)
    (let [a (store/site st subject)]
      (when (true? (:safety-concern-unresolved? a))
        [{:rule :unresolved-safety-concern
          :detail (str subject " は未解決の安全性懸念（構造不安定/危険物）がある状態での解体作業スケジュール提案")}]))))

(defn check
  "Censors a Demolition Advisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (unknown-op-violations request)
                           (effect-not-propose-violations proposal)
                           (forbidden-action-class-violations proposal)
                           (site-not-verified-violations request st)
                           (legal-basis-missing-violations request proposal)
                           (hazmat-survey-incomplete-violations request st)
                           (notification-lead-time-insufficient-violations request st)
                           (unresolved-safety-concern-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        cost (get-in proposal [:value :cost-usd])
        cost-stakes? (and (= (:op request) :order-supplies)
                          (number? cost)
                          (> cost supply-order-cost-threshold-usd))
        stakes? (boolean (or (high-stakes (:stake proposal)) cost-stakes?))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
