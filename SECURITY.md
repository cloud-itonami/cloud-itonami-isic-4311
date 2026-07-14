# Security Policy

This project handles demolition-site coordination data (site records,
hazmat-survey status, safety-concern flags, supply-order proposals).
Treat vulnerabilities as potentially high impact even when the demo data
is synthetic -- a bypass of the Demolition Governor's hard checks could
allow a demolition-operation schedule to be proposed against an
unverified site, an incomplete hazmat survey, or an unresolved safety
concern.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real site, hazmat-survey or personal data exposure
- authorization bypass
- Demolition Governor bypass (including any path that lets a proposal
  commit with an `:effect` other than `:propose`, or that lets an
  equipment-control/plan-finalization marker through)
- audit-ledger tampering
- over-disclosure in safety-concern notices or exports
- tenant isolation failures

## Reporting

Use GitHub private vulnerability reporting when available for the
repository. If that is unavailable, contact the repository maintainers
through the cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on site-safety data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets (RESEND_API_KEY, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
  outside Git.
- Keep real site/hazmat/personal data outside this repository.
- Run the full test suite (`clojure -M:dev:test`) before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
- Never deploy a fork that has relaxed the Demolition Governor's closed
  op-allowlist or forbidden-action-class check.
