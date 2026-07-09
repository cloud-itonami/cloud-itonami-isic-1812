# cloud-itonami-1812

Open Business Blueprint for **ISIC Rev.5 1812**: service activities
related to printing (independent pre-press services such as plate-
making and color proofing, and independent post-press/bindery
services such as cutting, folding, binding and laminating).

This repository designs a forkable OSS business for community print-
support services: job-specification and equipment-safety-scope
management, robotics-assisted plate-making/proofing and cutting/
folding/binding, and job/quality records — run by a qualified
operator so a pre-press or bindery shop keeps its own job and quality
history instead of renting a closed print-support platform.

## Scope note: independent print-support, not press production

`cloud-itonami-isic-1811` ("Community Printing Operations") covers
the integrated in-house press-production business itself -- operating
printing presses and their own finishing/binding line. This
repository is deliberately scoped to the SEPARATE business of
providing pre-press or post-press/bindery SERVICES independently,
frequently to MULTIPLE print shops that lack their own in-house
plate-making, proofing or bindery capacity (a value-chain relationship
mirroring `cloud-itonami-isic-5224`'s own cargo-handling-serves-
multiple-carriers pattern). Bindery/cutting equipment (guillotines,
folders) carries its own well-documented machine-guarding hazard
category under OSHA and equivalent international workplace-safety
frameworks, distinct from press-line safety concerns; pre-press color
proofing follows the same ISO 12647 process-control standards as
press production but applied at the proofing stage rather than the
press itself.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (plate-making/
proofing assist, cutting/folding/binding line operation) operate
under an actor that proposes actions and an independent **Print
Support Governor** that gates them. The governor never releases a
finished job for delivery itself; `:high`/`:safety-critical` actions
(a cutting/binding job outside verified equipment-safety scope, a
delivery release without a completed quality-inspection pass, a
quality record without verified evidence) require human sign-off.

## Core Contract

```text
intake + identity + job-specification/equipment-safety scope + work order
        |
        v
Print Support Advisor -> Print Support Governor -> production record, inspection record, release, or human approval
        |
        v
robot actions (gated) + production record + quality record + audit ledger
```

No automated advice can release a finished job for delivery the
governor refuses, advance a cutting/binding step outside its verified
equipment-safety scope, or publish a quality record without governor
approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `1812`). Implemented by:

- [`kotoba-lang/robotics`](https://github.com/kotoba-lang/robotics) — missions, actions, safety-stops, telemetry proofs
- [`kotoba-lang/cae`](https://github.com/kotoba-lang/cae) — proofing/color-calibration simulation evidence

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
