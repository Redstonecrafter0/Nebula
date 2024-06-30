# Nebula
An easy-to-use download manager.

> EARLY WIP!

Goals:
- Notify on:
  - Start(step: Int, maxStep: Int, max: Long?)
  - Progress(pos: Long?)
  - Retry
  - Finished(success: Boolean, reason: String)
- Use APIs to filter for fixed/latest version
- Verify integrity
- Save to specified location without partial downloads
