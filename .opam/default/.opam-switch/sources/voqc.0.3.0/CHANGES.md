## 0.3.0 (2022-07-07)

- Added support for a new mapping validation function (`check_swap_equivalence`).
- Updated the interfaces of mapping-related functions.
- Generally improved organization and documentation.
- Moved benchmarking scripts to [VOQC-benchmarks](https://github.com/inQWIRE/VOQC-benchmarks).

## 0.2.1 (2021-06-21)

- Updated to the most recent version of SQIR & extracted using Coq v8.13.2.
- The only change in SQIR that impacts the behavior of mlvoqc is that Optimize1qGates now merges as many gates as possible instead of just two at a time.

## 0.2.0 (2021-04-07)

- Various updates to documentation and scripts.
- Switched to using [dune-release](https://github.com/ocamllabs/dune-release) for publishing.

## 0.1 (2021-03-30)

- Initial release.