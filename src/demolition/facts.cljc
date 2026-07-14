(ns demolition.facts
  "Per-jurisdiction demolition-safety regulatory catalog -- the spec-basis
  table the Demolition Governor checks every `:schedule-demolition-
  operation` proposal against ('did the advisor cite an OFFICIAL public
  source for this jurisdiction's hazmat-survey/demolition-notification
  requirements, or did it invent one?'). Same honest-coverage discipline
  `construction.facts` (`cloud-itonami-isic-4211`) established for this
  fleet: a jurisdiction not in this table has NO spec-basis, full stop --
  the advisor must not fabricate one, and the governor holds if it tries.

  Coverage is reported HONESTLY (see `coverage`); this is a STARTING
  catalog (JPN/USA/DEU), not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to `catalog`,
  cite a real source, done -- never invent a jurisdiction's requirements
  to make coverage look bigger.

  `:threshold-model` mirrors the SAME honest quantitative/qualitative
  split `construction.facts/threshold-model` established, applied here to
  a DIFFERENT real-world numeric trigger -- the minimum number of days a
  demolition-notification filing must precede the planned start of work:
    :quantitative -- the law itself states a fixed lead-time (Japan's 7
                     days under the Construction Recycling Act; the USA's
                     10 working days under the NESHAP asbestos rule).
                     `notification-lead-insufficient?` can independently
                     recompute a HARD hold from this.
    :qualitative  -- the law imposes a documented 'plan of work' duty
                     before demolition/asbestos-removal work starts, with
                     NO fixed EU-wide numeric lead-time (Germany/EU).
                     This actor does NOT invent a day-count to make this
                     jurisdiction look automatable -- `notification-lead-
                     insufficient?` returns `:qualitative` and the
                     Demolition Governor's permanent high-stakes gate on
                     `:schedule-demolition-operation` (see `demolition.
                     governor` ns docstring) routes the decision to a
                     human every time regardless.

  DEU is used as the EU-jurisdiction proxy, the SAME convention
  `construction.facts`/`aerospace.facts` established -- there is no
  ISO-3166 alpha-3 code for the EU itself, and Directive 2009/148/EC is
  transposed into national law per member state (here: Germany's
  Gefahrstoffverordnung / TRGS 519), so the citation lists BOTH the EU
  directive and its German transposition rather than inventing an EU
  country code."
  )

(def catalog
  "iso3 -> requirement map. `:hazmat-survey-basis` / `:demolition-
  notification-basis` / their `-provenance` pairs, plus `:owner-authority`,
  are the G2-style citation the governor requires before a `:schedule-
  demolition-operation` proposal can ever commit. `:permit-basis` /
  `:permit-provenance` are carried for citation completeness in the
  rendered schedule/safety-concern documents (`demolition.registry`) but
  are NOT independently governor-hard-gated in this R0 slice (unlike
  `construction.facts`'s building-permit check) -- reported honestly as
  reference data, not fabricated as an enforced gate this actor doesn't
  actually check."
  {"JPN" {:name "Japan"
          :owner-authority "厚生労働省（労働基準監督署）／都道府県知事（建設リサイクル法届出先）／国土交通省"
          :hazmat-survey-basis "石綿障害予防規則（平成17年厚生労働省令第21号）第3条（建築物・工作物の解体・改修等の作業を行う前に、石綿使用の有無について事前調査を行う義務）"
          :hazmat-survey-provenance "https://laws.e-gov.go.jp/law/417M60000100021"
          :demolition-notification-basis "建設工事に係る資材の再資源化等に関する法律（建設リサイクル法）第10条（床面積の合計が80㎡以上の建築物の解体工事について、工事着手の7日前までに都道府県知事等へ分別解体等の計画を届け出る義務）"
          :demolition-notification-provenance "https://laws.e-gov.go.jp/law/412AC0000000104"
          :threshold-model :quantitative
          :notification-lead-days 7
          :threshold-note "工事着手の7日前までの届出義務（建設リサイクル法第10条、床面積80㎡以上の解体工事が対象）"
          :permit-basis "建築基準法施行規則 第8条の2（建築物除却届 -- 床面積10㎡を超える建築物を除却する場合、施工者は都道府県知事等に届け出る）"
          :permit-provenance "https://laws.e-gov.go.jp/law/325M50004000040"}
   "USA" {:name "United States"
          :owner-authority "U.S. Environmental Protection Agency (EPA), NESHAP Asbestos Program / Occupational Safety and Health Administration (OSHA), U.S. Department of Labor / State or local building department (Authority Having Jurisdiction)"
          :hazmat-survey-basis "OSHA 29 CFR 1926.1101 (Asbestos, construction industry standard -- demolition IS 'removal' under this standard, triggering Class I/II work classification, regulated-area and exposure-assessment duties before work begins)"
          :hazmat-survey-provenance "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.1101"
          :demolition-notification-basis "40 CFR 61.145 (NESHAP Subpart M -- written notice of intention to demolish/renovate to EPA or its delegated state agency, required at least 10 working days before demolition begins)"
          :demolition-notification-provenance "https://www.ecfr.gov/current/title-40/chapter-I/subchapter-C/part-61/subpart-M/section-61.145"
          :threshold-model :quantitative
          :notification-lead-days 10
          :threshold-note "解体開始の10営業日（working days）前までのEPA（または権限移譲先の州機関）への事前通知義務（40 CFR 61.145）。日本の暦日7日とは単位（営業日 vs 暦日）が異なる点を混同しない。"
          :permit-basis "International Building Code (IBC) §105.1 (Permits -- Required: demolition of a building or structure requires a permit before work begins) -- ICC model code adopted, with amendments, as state/local law by the Authority Having Jurisdiction"
          :permit-provenance "https://codes.iccsafe.org/s/IBC2021P1/chapter-1-scope-and-administration/IBC2021P1-Ch01-Sec105.1"
          :permit-note "The IBC is a model code published by the International Code Council (ICC), not federal statute; it becomes law only via state/local AHJ adoption -- the same honest labeling cloud-itonami-isic-4211 uses for its building-permit citation."}
   "DEU" {:name "Germany (EU jurisdiction proxy, see ns docstring)"
          :owner-authority "Bundesministerium für Arbeit und Soziales (BMAS) / Bundesanstalt für Arbeitsschutz und Arbeitsmedizin (BAuA); EU level: European Agency for Safety and Health at Work (EU-OSHA)"
          :hazmat-survey-basis "Directive 2009/148/EC (protection of workers from the risks related to exposure to asbestos at work, codified version) Art.11 -- a documented plan of work must be drawn up before demolition or asbestos-removal work is started; nationally transposed via the Gefahrstoffverordnung (GefStoffV, Hazardous Substances Ordinance) and TRGS 519 (Technical Rule for Hazardous Substances -- Asbestos: demolition, reconstruction or maintenance work)"
          :hazmat-survey-provenance "https://eur-lex.europa.eu/eli/dir/2009/148/oj"
          :framework-provenance "https://www.baua.de/EN/Service/Technical-rules/TRGS/TRGS-519"
          :threshold-model :qualitative
          :notification-lead-days nil
          :threshold-note "EU/ドイツの石綿関連法令は解体・石綿除去作業開始前の作業計画書（plan of work）作成義務を課すのみで（Directive 2009/148/EC 第11条、GefStoffV/TRGS 519）、日本の7暦日・米国の10営業日のような固定日数の届出リードタイムはEU全域では法定されていない -- ここで数値を創作しない。"
          :demolition-notification-basis "Landesbauordnung（州建築法、標準法として Musterbauordnung MBO 第61条以下参照）-- Abbruchanzeige/Abbruchgenehmigung（解体届出／許可）が解体作業開始前に必要。固定日数のリードタイムは州法ごとに異なり、EU全域の統一値は法定されていない。"
          :demolition-notification-provenance "https://www.dibt.de/en"
          :permit-basis "Landesbauordnung（州建築法、標準法として Musterbauordnung MBO 参照）-- Abbruchgenehmigung/Abbruchanzeige（解体許可／届出）が解体開始前に必要。建築法制は連邦ではなく州（Länder）の権限。"
          :permit-provenance "https://www.dibt.de/en"
          :permit-note "German demolition permits/notifications are governed by the state Landesbauordnungen, not federal law -- the same honest state-vs-federal layering cloud-itonami-isic-4211 uses for its own DEU building-permit citation."}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any `:schedule-demolition-operation`
  proposal that tries to cite one."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-4311 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `demolition.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn notification-lead-insufficient?
  "Independently recompute whether `site`'s own recorded
  `:notification-lead-days-actual` (the site's own permanent recorded
  field -- days between the demolition-notification filing and the
  planned start of work) falls SHORT of `iso3`'s regulatory minimum lead
  time.

  Three-valued, deliberately (the same shape `construction.facts/weather-
  threshold-exceeded?` established):
    true         -- a :quantitative jurisdiction (Japan, USA) whose own
                    numeric minimum lead time is independently confirmed
                    NOT met by the site's own recorded actual -- a
                    bright-line legal violation. The Demolition Governor
                    turns this into a HARD, un-overridable hold on
                    `:schedule-demolition-operation`.
    false        -- a :quantitative jurisdiction confirmed sufficient.
    :qualitative -- a jurisdiction with NO fixed numeric lead-time (DEU/
                    EU). This actor cannot independently confirm
                    'sufficient' or 'insufficient' by arithmetic alone --
                    the law itself requires a documented plan-of-work
                    judgment call. Never fabricate a lead-time here. The
                    Demolition Governor relies on its permanent
                    high-stakes gate for `:schedule-demolition-operation`
                    (ALWAYS escalates to a human, at every phase) rather
                    than a HARD numeric rule in this case.
    nil          -- no spec-basis at all for `iso3` (a jurisdiction not in
                    `catalog`)."
  [iso3 {:keys [notification-lead-days-actual]}]
  (when-let [{:keys [threshold-model notification-lead-days]} (spec-basis iso3)]
    (case threshold-model
      :quantitative
      (boolean (and (number? notification-lead-days-actual)
                    (< notification-lead-days-actual notification-lead-days)))
      :qualitative
      :qualitative
      nil)))
