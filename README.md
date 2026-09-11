# cloud-itonami-4311

Open Business Blueprint for **ISIC Rev.5 4311**: demolition of buildings and other structures.

This repository designs a forkable OSS business for demolition-project
operations coordination: run by a qualified operator so a community keeps
its own operating records instead of renting a closed SaaS.

## Scope -- this is a COORDINATION-ONLY actor, not equipment control

This is a safety-critical domain: structural collapse risk, hazardous
materials (asbestos, lead). **This actor does NOT hold heavy-equipment-
control authority, and it does NOT hold structural-engineering-decision
or demolition-plan-finalization authority.** Both are the licensed
engineer / site supervisor's exclusive authority, always. The Demolition
Advisor (LLM) never issues an equipment-control command and never
finalizes a demolition sequence or engineering plan; the independent
**Demolition Governor** HARD-blocks any proposal that even tries
(un-overridable by any human approval -- see `demolition.governor` ns
docstring). This actor coordinates *potential* equipment dispatch (a
proposed schedule window, a flagged concern, a supply-order proposal) --
it never directly actuates.

Structurally, EVERY proposal this actor's advisor can produce carries
`:effect :propose`, and the Demolition Governor HARD-holds any proposal
that doesn't -- this is a permanent invariant distinguishing this actor
from `cloud-itonami-isic-4211` (the robotics-premise reference this
actor follows structurally), whose sibling actuation ops DO commit
real-world effects (mail dispatch, robot placement, structure handover).
`cloud-itonami-isic-4211`'s README robotics-premise framing therefore
does NOT apply verbatim here: this actor is deliberately narrower.

## Core Contract

```text
site record + independent verification
        |
        v
Advisor -> Demolition Governor -> proceed (log/schedule/flag/order proposal), hold, or human approval
        |
        v
coordination artifacts (schedule proposal, safety-concern flag,
supply-order proposal) + audit ledger -- NEVER equipment dispatch,
NEVER a finalized demolition plan
```

No automated advice can propose a schedule the governor refuses, suppress
a safety-concern flag, or slip an equipment-control/plan-finalization
marker past the governor -- and even a clean, governor-approved proposal
still always needs a human sign-off for scheduling and safety concerns
(see `Actuation` below).

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `4311`). Required capabilities:

- `:identity`
- `:forms`
- `:audit-ledger`
- `:notifications`

## Implemented slice (`src/demolition`)

`blueprint.edn` names the governor `:demolition-governor` and is now
`:implemented`. This repo implements it end-to-end -- **Demolition
Advisor ⊣ Demolition Governor** -- following the SAME `.cljc` actor
pattern (langgraph-clj StateGraph, mock-by-default advisor, dual
MemStore/Datomic backend, 0→3 phase rollout) every prior
`cloud-itonami-isic-*` actor in this fleet uses, structured after
[`cloud-itonami-isic-4211`](https://github.com/cloud-itonami/cloud-itonami-isic-4211)
(the robotics-premise construction-domain reference), narrowed to
coordination-only authority as described above.

### Closed op-allowlist (4 ops, all `:effect :propose`)

| Op | Ask | Implementation |
|---|---|---|
| `:log-site-record` | site survey / hazmat-assessment / progress data logging | Normalizes and commits a patch onto the site's ground-truth fields (`:site-verified?`, `:hazmat-survey-completed?`, `:notification-lead-days-actual`, concern resolution, etc.) and appends an immutable site-record-log entry. No direct capital/safety risk -- MAY auto-commit at phase 3. |
| `:schedule-demolition-operation` | demolition-phase scheduling proposal | Drafts a proposed schedule WINDOW (never a finalized engineering plan). ALWAYS escalates to a human at every phase -- coordinates potential heavy-equipment dispatch. |
| `:flag-safety-concern` | surface a structural-instability / hazmat (asbestos/lead) concern | Drafts a safety-concern flag; ALWAYS escalates to a human, unconditionally. Once approved, `demolition.notify` sends the notice (mail + phone) to the site's licensed-engineer/site-supervisor/hazmat-authority contact roster. |
| `:order-supplies` | equipment/disposal-service procurement proposal | Drafts a supply-order proposal. Escalates above a cost threshold or below the confidence floor; may auto-commit at phase 3 otherwise. |

**Legal basis is data, not code** -- `src/demolition/facts.cljk`'s
`catalog` is the per-jurisdiction EDN source-of-truth the governor checks
every `:schedule-demolition-operation` proposal against (JPN/USA/DEU
seeded; DEU stands in for the EU, the same convention
`construction.facts`/`aerospace.facts` use for EASA):

| Jurisdiction | Hazmat-survey legal basis | Demolition-notification legal basis |
|---|---|---|
| 🇯🇵 Japan | 石綿障害予防規則（平成17年厚生労働省令第21号）第3条 -- [e-Gov](https://laws.e-gov.go.jp/law/417M60000100021) | 建設リサイクル法 第10条（床面積80㎡以上、着手7暦日前までの届出）-- [e-Gov](https://laws.e-gov.go.jp/law/412AC0000000104) |
| 🇺🇸 USA | OSHA 29 CFR 1926.1101 -- [osha.gov](https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.1101) | 40 CFR 61.145 (NESHAP Subpart M, 10営業日前までの通知）-- [eCFR](https://www.ecfr.gov/current/title-40/chapter-I/subchapter-C/part-61/subpart-M/section-61.145) |
| 🇪🇺 EU (DEU proxy) | Directive 2009/148/EC Art.11 + GefStoffV/TRGS 519 -- [EUR-Lex](https://eur-lex.europa.eu/eli/dir/2009/148/oj) | Landesbauordnung Abbruchanzeige/-genehmigung (qualitative -- state-level, no fixed EU-wide day count) |

Japan (7 calendar days) and the USA (10 working days) have real numeric
notification lead-time triggers; the EU deliberately does NOT --
`demolition.facts/notification-lead-insufficient?` reports `:qualitative`
there rather than fabricating a number, and `:schedule-demolition-
operation` always routes to a human regardless of jurisdiction anyway
(see `Actuation` below). See `demolition.facts` ns docstring for the
full honesty discipline.

**Governor -- eight HARD checks, ALL un-overridable by human approval:**
unknown op (outside the closed 4-op allowlist), `:effect` not `:propose`,
forbidden action class (equipment-control / direct-actuation /
engineering-plan-finalization markers), site not independently
verified/registered, legal-basis missing, hazmat survey incomplete,
notification lead time insufficient (quantitative jurisdictions only),
unresolved safety concern on file. See `demolition.governor` ns
docstring for the full enumeration, rationale and real-law citations
behind each.

## Actuation

This actor performs **no real-world actuation** -- every committed
record carries `:effect :propose` (see `demolition.governor` ns
docstring). `:schedule-demolition-operation` and `:flag-safety-concern`
NEVER auto-commit at any phase -- both always need a human sign-off, even
when the governor is completely clean (`demolition.phase` ns docstring
'Actuation' section, `demolition.governor`'s `high-stakes` set).
`:log-site-record` (pure data logging) and `:order-supplies` BELOW the
cost threshold (`demolition.governor/supply-order-cost-threshold-usd`)
MAY auto-commit at phase 3 when the governor is clean.

```bash
clojure -M:dev:run    # demo: full coordination episode + every HARD hold
clojure -M:dev:test   # test suite
clojure -M:lint       # clj-kondo, errors fail
```

## License

AGPL-3.0-or-later.
