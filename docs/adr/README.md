# Architecture decision records

Each file records one decision: what forced it, what else was on the table, and what it costs. `docs/architecture.md` says how the app is built; these say why it is built that way and not another.

| ADR | Decision | Status |
| --- | --- | --- |
| [0001](0001-rows-as-json-documents.md) | Room stores every row as a JSON document | Accepted |
| [0002](0002-core-owns-storage-interface.md) | Core declares `AppStorage`; storage engines implement it | Accepted |
| [0003](0003-one-app-module.md) | One app module with checked screen boundaries, not a module per feature | Accepted |

A new record takes the next number, and the same sections: Context, Decision, Options considered, Trade-off analysis, Consequences. A decision that replaces an earlier one says so in its status line, and the earlier one's status becomes "Superseded by ADR-N". Accepted records are not rewritten; they are superseded.
