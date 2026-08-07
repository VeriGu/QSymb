# SymbolicOptimizer — Project Guide

A quantum-circuit optimizer for ion / IBM / Nam / Rigetti gate sets that combines **e-graph saturation** (egglog) with **symbolic-rule SA search**. Optimizes for 2q-gate count, total size, or fidelity.

## Build & run

```bash
bash build_qsymb.sh                     # rebuilds the fat jar in /root
# Run on a benchmark:
java --enable-preview -Xss256m -Xmx16g \
  -cp /root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Optimizer \
  -b guoq_benchmarks/ion/qaoa_5.qasm \
  -r rules_ion_q3_3.txt -lr rules_q3_s3_ion.txt -sr anchored_ion_q3.txt \
  -m SA -t 240 -symb true -g ion -ilp true -minsymb 0 -maxsymb 30
```

Build idiosyncrasy: `Optimizer.java` has imports `import dag.CircuitDAG;` etc that **must be removed** for Maven build (default-package classes shadow the `dag.*` subpackage names). `bash build_qsymb.sh` works after the imports are deleted.

## Rule file taxonomy (4 distinct kinds, easy to confuse)

| Flag | File | Format | Purpose |
|---|---|---|---|
| (auto-loaded by `-g <gateset>`) | `rules_ion.txt` | egglog S-expr | **Initial / hand-written egglog rules** (commute, merge, gadget identities). Always loaded. **Concrete only — no SYMB allowed**. |
| `-r` | `rules_ion_q3_3.txt` | QASM-pipe | **Concrete enumerated rules** (parsed by `QASMAstBuilder`, fed to egglog). **Concrete only — no SYMB allowed**. |
| `-lr` | `rules_q3_s3_ion.txt` | QASM-pipe | **Long rules**, applied stochastically each SA iter via `random.nextInt(validLongRules.size())`. The MAIN driver of circuit mutation. |
| `-sr` | `anchored_ion_q3.txt` | S-expr + Matrix(...) basis | **Anchored symbolic rules** with SYMB placeholder + commutant basis matrices. Picked one-at-a-time by `SymbolicThread` per SA iter. |

Source files for canonical (pre-anchor) symbolic rules: `rules_ion_q3_2_symb_nm.txt` (size-2 enumeration; size-3 is empty). Anchored output goes to `anchored_ion_q3.txt`.

**Important: egglog e-graph saturation only handles CONCRETE rules** (no SYMB placeholder). The egglog inputs are `rules_ion.txt` and `rules_ion_q3_*.txt` (both `-r` and the hand-written `.txt`). Symbolic rules (with SYMB) — both the canonical `rules_ion_q3_2_symb_nm.txt` and the anchored `anchored_ion_q3.txt` — are applied ONLY via the SA loop's `SymbolicThread` / `symbolicMatchBeforeAfter` path, never via egglog. Consequence: adding a new symbolic rule does NOT contribute to egglog saturation; it only affects the SA cycle. To benefit from egglog, the rule must be expressible without SYMB.

## Two-thread SA loop (Optimizer.optimize_SA)

Each iteration:
1. **egglog saturation** runs (deterministic) on the current circuit, applying all rules in `rules_ion.txt` + `rules_ion_q3_3.txt`.
2. **Long-rule random pick** (`random.nextInt(validLongRules.size())`) tries 1 long rule per iter; applied if it matches. This is the main stochastic driver.
3. **SymbolicThread** runs concurrently — picks ONE symbolic rule (`rand.nextInt(symbRules.size() + monomialRules.size())`), tries `symbolicMatchBeforeAfter` which grows a SYMB region in `[minSymb, maxSymb]`, basis-checks via Python `semantics.py` subprocess (slow), and applies on success.
4. SA accept/reject with `random.nextDouble() <= acceptP` based on temperature.

**Important**: `random` and `symbRandom` are separate (added 2026-05-24) so changes to the symbolic pool don't perturb the long-rule random sequence.

## Key concepts

### Concrete vs symbolic rules
- **Concrete-with-free-angle** (e.g., QAOA gadget rule in `rules_ion.txt`): LHS and RHS both have specific gates; `γ` moves between positions. **Can encode Clifford-conjugation rewrites**.
- **Symbolic SYMB rules**: LHS/RHS contain a `(SYMB N)` placeholder. The matched middle's matrix must lie in a basis (commutant or intertwiner). Cannot perform Clifford-conjugation transforms.

### Anchoring (Anchor.java)
Composes a canonical symbolic rule with a concrete rule via prefix/suffix overlap:
- `matchPrefix(post_R, concrete_lhs)`: extends LHS forward.
- `matchSuffix(pre_R, concrete_lhs)`: extends LHS backward.
- The anchored rule **inherits the canonical's basis** (it's NOT recomputed).
- The matchAngle convention in `Anchor.matchAngle` is asymmetric — the concrete's parameter is matched against the symbolic's. A fix handles the case where the concrete parameter is a concrete `Real` and the symbolic parameter is a `theta` Symbol (added 2026-05-24).

### Verifier (Verifier.verifyv2)
Symbolic equivalence checker — evaluates path sums with random angle samples (default 10 samples × multiple phase candidates). Used by both the enumerator and the test harness when verifying rule identities. Much faster than full qiskit equivalence checking and handles free symbols.

### Canonicalization & dedup (rules go through THREE normalization steps)
1. **`EnumeratorPrune.previousReps`** (`Map<qasmString, ConstrainedCircuit>`) deduplicates enumerated circuits at the circuit level. Only the smallest representative per qasm string survives.
2. **`EnumeratorPrune.canonicalizeCircuit`** renames qubits by first-occurrence order (q5 → q0, q3 → q1) so structurally-identical circuits with different qubit naming collapse to one.
3. **`EggGen.addRewriteRule`** (line 1063) — when actually adding a rule for learning, calls `canonicalizeCircuit(..., qubitToVar, true)` on BOTH LHS and RHS with the SAME `qubitToVar` map. This canonicalizes qubits AND angle symbols (the `true` flag = `replaceSymbol`) consistently across the rule. Then `addOptRules` dedupes by the exact canonical string.

So rules differing only by qubit-name swaps (q0↔q1) or symbol-name swaps (theta1↔theta2) are collapsed to a single canonical entry before being stored. This is why a search for naming-variant duplicates returns 0.

The log "duplicates" of `Adding optimization rule: ...` come from **multiple `EggGen` instances during enumeration** — each instance has its own `optrules` set and re-adds the baseline canonicals on construction. This is setup overhead, not learning redundancy.

## Grammar (symbAngles) and why it matters

Each gateset specifies a `symbAngles` array in `EnumeratorPrune.main`. The enumerator builds candidate LHS patterns by iterating over these angles. **Both the concrete-rule enumeration AND the symbolic-rule enumeration use this array.**

For ion (current `EnumeratorPrune.java:1631`):
- `theta1`, `theta2` — free symbols. That's it.

We trimmed concrete angles (`pi`, `pi/2`, `-pi/2`) out of `symbAngles` because in the **symbolic phase** they introduced √2 via `cos(π/4)`, forcing the algebraic-field (Q(ζ₈)) path in `semantics.py` — 10-100× slower than Q(i). The trade-off: concrete-rule enumeration also loses those angles, dropping concrete rule counts dramatically (81 → 3 at size 2). When running with `-symb true` we only care about symbolic rules, so this is acceptable. If you ever need rich concrete-rule sets (e.g., the Clifford rewrites that power the QAOA gadget), put the concrete angles back temporarily.

Negative angles (e.g. `-theta1`, `-pi/2`) and sums (e.g. `theta1+theta2`) can be re-added if a specific gadget identity requires them; the enumerator does **not** auto-negate or auto-compose.

## Key files / where things live

```
/root/SymbolicOptimizer/src/main/java/
  Optimizer.java         — main SA loop, symbolicMatchBeforeAfter, matchAngle (Java-side)
  SymbolicThread.java    — picks one symbolic rule per spawn
  EnumeratorPrune.java   — rule discovery + symbAngles config
  Anchor.java            — composes canonicals + concrete rules
  Verifier.java          — path-sum based symbolic equivalence
  EggGen.java            — egglog wrapper, const-eval rules, sendCommand with timeout
  dag/CircuitDAG.java    — DAG representation, cost functions (zero-angle aware)

/root/
  rules_ion.txt          — hand-written egglog rules (gadget Identity A + B)
  rules_ion_q3_3.txt     — size-3 concrete enumerated rules
  rules_q3_s3_ion.txt    — long rules
  anchored_ion_q3.txt    — anchored symbolic rules (279 default)
  rules_ion_q3_2_symb_nm.txt — canonical symbolic rules (size 2, 124 rules)
  guoq_benchmarks/ion/   — benchmark QASM files
  optimizer_logs/        — runs from previous sessions
  semantics.py           — Python basis-check subprocess server
  qiskit_equivalence_checker.py — concrete-angle equivalence

  TestQAOAFinal.java, TestGadgetRule*.java, TestSymbolicRules.java,
  TestAnchor.java, TestAnchorReal.java, SaveAnchored.java,
  TestNewSymbRules.java, TestZeroAngleRule.java  — Verifier-based test framework
```

## QSymb paper (this codebase implements it) — formal definitions

The codebase implements the paper **"Synthesis of Compact and Expressive Quantum-Circuit Optimizations"** (`/root/qsymb.pdf`). Read it once before touching the rule synthesizer — terminology below maps directly.

### Symbolic rule
A symbolic rewrite rule `R_s` has the form `G₁; S; G₂ ≡ G₁'; S; G₂'` where `G₁, G₂, G₁', G₂'` are concrete circuit sequences and `S` is a **symbolic variable** representing a set of subcircuits. `S` on the LHS *matches* an arbitrary subcircuit `C`; `S` on the RHS is *substituted with the same matched `C`*. The constraint matrix `M` characterizes exactly which `C` make `G₁; C; G₂ ≡ G₁'; C; G₂'`.

### Canonical symbolic rule (Theorem 5.1 in the paper)
Every symbolic rule `G₁; S; G₂ ≡ G₁'; S; G₂'` is equivalent to a **canonical** rule of the form

  **L; S = S; R**

where:
- `L = ⟦G₁⟧ · ⟦G₁'⟧⁻¹` (i.e., apply `G₁`, undo `G₁'`)
- `R = ⟦G₂⟧⁻¹ · ⟦G₂'⟧`

In the canonical form `S` sits at the **two ends** of the rule, on **opposite sides** — LHS has `S` at the right edge, RHS has `S` at the left edge (or vice versa). Non-canonical forms are derivable from the canonical by left/right-appending implementations of `⟦G₁⟧⁻¹` and `⟦G₂⟧⁻¹` to both sides. Canonical form is what `infer_symb` enumerates — never `gate; S; gate` interior placements — and is why the file `rules_ion_q3_2_symb_nm.txt` only contains `(SYMB at end)|(SYMB at start)` shaped pairs.

### Constraint / basis of S (§5.4)
For canonical form `L; S = S; R`, find unitary `S` with `S · ⟦L⟧ = ⟦R⟧ · S`. Equivalent to solving the linear nullspace of `K = (I ⊗ ⟦L⟧ᵀ) − (⟦R⟧ ⊗ I)` over a polynomial ring in `W_θ = exp(iθ/2)` substitutions. The solution space is a list of basis matrices `{B₁, ..., B_k}`; any unitary in the span is a valid `S`. This is the `Matrix(...)::Matrix(...)` content stored on each symbolic-rule line.

### Property grouping (Lemmas 5.3–5.4)
A unitary intertwiner `S` with `SL = RS` exists **iff `L` and `R` have the same eigenvalue multiset** (same eigenvalues with same multiplicities). QSymb groups symbolic-term candidates by their concrete-trace fingerprint first (sampling angles), then verifies eigenvalue equality. The `-skipDistinct` flag in our `EnumeratorPrune` rejects candidates where `L` has all-distinct eigenvalues — those produce a "thin" 3-torus intertwiner (`dim = n`) and aren't worth solving.

### Rule anchoring (Step ③, §3)
Canonical symbolic rules have `S` at the two ends, but most circuit gadgets need `S` to sit inside other gates. **Anchoring** selectively appends the *same prefix or suffix* to both sides of a canonical rule so the resulting circuit fragment matches the LHS of a concrete cancellation rule. Example: canonical `cx q0 q1; S → S; cx q0 q1` becomes anchored `cx q0 q1; S; cx q0 q1 → S; cx q0 q1; cx q0 q1` — and now the orange fragment `cx q0 q1; cx q0 q1` on the RHS matches the concrete rule `cx q0 q1; cx q0 q1 → ε`, so the anchored rule effectively shrinks the circuit. `Anchor.java`'s `matchPrefix`/`matchSuffix` does this composition.

### How a symbolic rule is applied at SA runtime (§5.5)
1. Syntactically pattern-match the LHS (`G₁; S; G₂` shape) against a window in the current circuit. This binds `S` to a concrete subcircuit `C` (10..30 gates in our config).
2. Compute the unitary matrix `M = ⟦C⟧` of the matched window.
3. Check whether `M` lies in the linear span of the basis matrices `{B₁,..,B_k}` of the rule — this is the symbolic-matrix constraint `S ⊨ M`. Implemented by `semantics.py is_subspace_linear_combination` (called once per match attempt — the expensive step).
4. If yes, replace the LHS occurrence with the RHS (substituting `C` for `S` on the RHS).

### Three-step QSymb pipeline (Fig. 3 of the paper)
1. **SynthesizeConcrete (Alg. 1, §4)** — enumerate all circuit terms up to size `n`, group into ECCs by PIF-style probabilistic hashing, infer non-derivable rewrite rules from each class. Output: `rules_ion_q3_k.txt`.
2. **SynthesizeSymb (Alg. 2, §5)** — take one representative per ECC from step 1, group by concrete-trace, then for each pair `(L, R)` solve the intertwiner equation `S⟦L⟧ = ⟦R⟧S`. Validate eigenvalues match, filter out single-solution cases, add `(L, R, B)` to symbolic-rule set. Output: `rules_ion_q3_k_symb_nm.txt`.
3. **Anchor (§3 step ③)** — for each canonical symbolic rule, find prefix/suffix combinations with concrete rules from step 1, append, store as anchored rule. Output: `anchored_ion_q3.txt`.

The final rule set fed to `Optimizer` is: hand-written `rules_ion.txt` (egglog) + concrete `rules_ion_q3_k.txt` + long rules `rules_q3_s3_ion.txt` + anchored symbolic `anchored_ion_q3.txt`.

### Non-derivability / completeness (§5, Lemma 5.6, Thm 5.11)
- Every symbolic rule whose `L` and `R` fit within `(n, q)` is derivable from canonical symbolic rules + the `(n,q)`-complete concrete rules.
- Each canonical symbolic rule generated by Alg. 2 is **non-derivable** from other canonical rules.

So the canonical set is a generative basis: small but complete.

## Mathematical structure (relevant for new rule development)

- A symbolic rule's basis is computed from `L = pre_R⁻¹·pre_L` and `R = post_R·post_L⁻¹` as the **intertwiner space** `{S : L·S = S·R}`. When `L = R`, this reduces to the commutant of L.
- The X⊗X commutant (8-dim) contains `{I, XI, IX, XX, YY, YZ, ZY, ZZ}` — operators that commute with `RXX(any)`.
- For QAOA-style middles containing `RZ` on a shared qubit, the matrix has `IY, IZ` components which are NOT in the X⊗X commutant — symbolic rules with RXX-based L can't accept these.
- The QAOA gadget reduction is a Clifford-conjugation identity that moves `γ` from a single-qubit `RZ` slot into a 2-qubit `RXX` slot. This is a **structural transformation**, not a permutation — best expressed as a concrete-with-free-angle rule in `rules_ion.txt`.

## Verifier-based rule discovery workflow

When writing a new rule:
1. Construct LHS and RHS in `Circuit` form via `Symbolic.rx(circ, q, angle)` etc.
2. Call `verifier.verifyv2(lhs, rhs, symbolMap)` for multiple random `symbolMap` values.
3. If 20/20 trials pass, the identity is robust. Add to `rules_ion.txt` (egglog S-expr) or as a concrete entry to enumerator's pool.

See `TestQAOAFinal.java` for a working example.

## Known fragility / gotchas

- The optimizer **must be run from `/root`** so that `rules_ion.txt` and benchmark paths resolve. Don't `cd` elsewhere.
- The Python `semantics.py` process is sometimes left running after a killed Optimizer — `pkill -9 -f semantics.py` to clean up.
- Egglog (`egglog-experimental` at `/root/.cargo/bin/`) can hang on certain rule patterns. EggGen has a 120s timeout + replay-log restart mechanism.
- Stray `.class` files in `/root/SymbolicOptimizer/src/main/java/` will cause Maven "duplicate class" errors — `find ... -name '*.class' -delete` if seen.
- `ILP_MAX_GATES = 300` — circuits larger than this skip the ILP compaction step.
- Random seed is `Random(30)` in Optimizer; `Random(31)` for symbRandom. Changing the symbolic rule pool used to perturb the main loop's random walk; the split fixes that.
