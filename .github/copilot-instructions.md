## Test reliability and reporting

- Run the relevant tests after code changes; never infer or invent test output.
- Report the exact command and distinguish completed, failed, skipped, and
  unexecuted tests. A targeted test run is not a full-suite result.
- Preserve meaningful assertions. Do not disable, weaken, or retry failing
  tests just to obtain a passing result.
- Prefer deterministic inputs and independent expected values. Avoid
  wall-clock, external-network, and shared-state dependencies unless the
  behavior under test specifically requires them.
- Treat skipped tests, flaky retries, and surviving mutation tests as
  evidence to investigate, not as passing validation.