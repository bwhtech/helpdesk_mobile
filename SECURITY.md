# Security Policy

## Supported versions

Only the most recent release is supported. Fixes land on `develop` and ship in the
next tagged release; older tags do not get patches.

| Version | Supported |
| ------- | --------- |
| 1.0.2 (latest release) | Yes |
| Older tags | No |

## Reporting a vulnerability

Report privately through GitHub:
[Report a vulnerability](https://github.com/bwhtech/helpdesk_mobile/security/advisories/new).

Please do not open a public issue, a pull request, or a discussion for a suspected
vulnerability, and please do not post details anywhere public until a fix has shipped.

A useful report includes:

- the app version and build type (debug, internal, release)
- the Android version and device
- what an attacker gains, and what access they need to get there
- the steps to reproduce it, with a log excerpt or screenshot if you have one

Redact your own credentials, tokens and site URL before sending anything. If a report
would require sharing a live API key or session token, describe the request instead of
including the secret.

## What to expect

- Acknowledgement within three working days.
- An assessment, with a severity and a decision on whether it is in scope, within seven days.
- High severity fixes go into the next release. Lower severity fixes are scheduled openly
  in the issue tracker once a public description is safe.

You will be credited in the release notes and the advisory unless you would rather stay
anonymous. This is an unpaid project; there is no bug bounty.

## Scope

In scope, in this repository:

- storage and handling of site credentials and OAuth tokens; the app keeps API keys,
  secrets and tokens in `EncryptedSharedPreferences` under an AES256-GCM master key
- the API client and anything that could send credentials or ticket data somewhere other
  than the configured site
- the local Room cache of tickets and agents, and what an attacker with device access
  can read from it
- push notification registration and payload handling
- the release pipeline: the signing setup, the workflows under `.github/workflows`, and
  the published APK

Out of scope:

- Frappe Framework and Frappe Helpdesk themselves. Report those to Frappe through
  <https://frappe.io/security>.
- configuration and hosting of the Helpdesk site the app connects to, including its
  permissions, roles and API key policy
- anything that needs a rooted or already compromised device, or physical access to an
  unlocked phone that is signed in
- results from automated scanners with no working attack path against this app
- the placeholder values in `local.properties.sample` and `google-services.json.sample`,
  which are not real credentials

## Notes for reporters

The app talks only to the site URL you configure, and it is distributed as a signed APK
from the GitHub releases page of this repository. An APK from anywhere else is not ours
and is not covered by this policy.
