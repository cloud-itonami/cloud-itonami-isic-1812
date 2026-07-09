# Governance

`cloud-itonami-1812` is an OSS open-business blueprint for community
print support services operations, robotics-premised.

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a robot action the governor refuses is never dispatched to hardware.
- the Print Support Governor remains independent of the advisor.
- hard policy violations (a job release without a completed quality
  inspection, a cutting/binding job outside verified equipment-safety
  scope) cannot be overridden by human approval.
- every dispatch, sign-off and release path is auditable.
- sensitive job and customer data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model, storage contract, public business model, operator certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a separate trust mark and should require security, robot-safety, audit and data-flow review.

Certified operators can lose certification for:
- bypassing robot-safety or equipment-safety-scope checks
- mishandling job or customer data
- misrepresenting certification status
- failing to respond to safety incidents
