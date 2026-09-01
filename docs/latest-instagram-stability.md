# Latest Instagram runtime stability

## Compatibility contract

InstaEclipse targets obfuscated Instagram builds whose internal class/method names can change without notice. Feature hooks must therefore be capability-driven rather than assuming a resolver succeeds.

### Rules

- Never perform expensive DexKit discovery synchronously from Instagram's main thread.
- Treat unresolved signatures as `UNAVAILABLE`, not as fatal initialization errors.
- Do not install a hook until its resolved method passes the expected structural/type checks.
- Cache discoveries with the Instagram build/version identity; invalidate incompatible cache entries before use.
- A failed feature resolver must not prevent unrelated features from installing.
- Runtime exceptions thrown by a hook must be contained at the feature boundary where practical and logged with feature/build context.
- Discovery should have bounded work and avoid repeated scans on every Activity recreation.

## Current compatibility gaps

Observed on the latest tested Instagram build:

- Ghost replay update method unresolved.
- Story mention raw mention getter unresolved.
- Caption getter unresolved.
- Force Reel Quality discovery unresolved.
- Feed video `VideoVersionIntf` implementors unresolved.
- Reel download gate signatures unresolved.

These are feature compatibility failures and must degrade gracefully until a valid resolver is available; they must never become startup-critical.
