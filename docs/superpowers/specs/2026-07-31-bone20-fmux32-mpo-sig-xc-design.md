# Bone2.0 FMUX32 MPO-SIG XC Design

## Problem

The Bone2.0 `FMUX_32` model creates the `COM1 -> SIGA,SIGB` APS XC but does not
create the fixed passive connection from `MPO1..MPO4` to `SIG`. SiteLink route
data therefore has a gap between the MUXPANEL/FMUX MPO hop and the FMUX
SIG/COM1 hop, and the frontend cannot draw that connection.

## Design

Add one initiated, fixed, bidirectional OCH XC to the FMUX32 card model:

- source ports: `MPO?,1,4,1`
- destination port: `SIG`
- multiplicity: `1`

The physical node will contain one XC with four source TPs and one destination
TP. Existing SiteLink MPO aggregation will serialize it as one virtual
`MPO -> SIG` XC.

The Bone2.0 Flex32 special path must rewrite only APS XCs to
`COM1 -> SIGA,SIGB`. Other initiated FMUX XCs must be created from their model
definitions without endpoint rewriting.

## Compatibility

- Keep existing equipment and TP identifiers.
- Keep the current APS XC and protection route behavior.
- Keep physical MPO1-4 links and existing virtual MPO route aggregation.
- No frontend change is required.
- Existing persisted SiteLinks are not migrated; newly computed/created data
  receives the fixed XC.

## Tests

- Assert the FMUX32 card model declares the fixed MPO-SIG XC and existing APS.
- Assert Flex32 node XC creation preserves MPO1-4 to SIG while rewriting only
  APS to COM1-SIGA/SIGB.
- Assert SiteLink serialization collapses the four MPO source TPs to one
  virtual MPO source without duplicate XC IDs.
