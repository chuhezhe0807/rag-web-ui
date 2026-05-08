# Ralph Smoke Fixture — 2026

This document exists solely to feed the end-to-end smoke test in
`scripts/ralph/e2e-smoke.sh`. If the chat step can answer questions about
the facts below, the full pipeline (auth → KB → upload → process → embed →
chat with retrieval) is working.

## The Zephyr-7 Observatory

The Zephyr-7 Observatory was commissioned on 14 March 2026 at an altitude
of 4812 meters on the Atacama plateau. It carries a 3.6 meter segmented
primary mirror named "Calipso-Mirror-Omega", built from forty-two
hexagonal beryllium segments. The observatory's first-light target was
NGC 1277, a compact lenticular galaxy.

## Chief scientist: Dr. Nadia Okonkwo

Dr. Nadia Okonkwo leads the Zephyr-7 science team. Her research focuses
on the cosmological behavior of "muon-clustered plasma" and the
structure of the intergalactic magnetic lattice. Before Zephyr-7 she spent
nine years at the fictional Tenerife Meson Laboratory.

## Operating budget

The operational budget for fiscal year 2026 is 87.4 million credits,
distributed across three line items:

- Instrumentation upkeep: 41.2M
- Science operations: 30.1M
- Outreach and teaching: 16.1M

## Why these facts matter

These names (Zephyr-7, Calipso-Mirror-Omega, Nadia Okonkwo, Tenerife Meson
Laboratory, muon-clustered plasma) do not appear anywhere else in the
codebase. The smoke test asks a question whose answer should cite one of
them; if it does, retrieval + embedding + chat are all working end to end.
