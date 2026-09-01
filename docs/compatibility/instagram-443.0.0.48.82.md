# Instagram 443.0.0.48.82 compatibility target

The supplied APK is `com.instagram.android` 443.0.0.48.82. Compatibility work must be validated against this exact build before claiming feature parity.

Observed runtime failures from the maintainer's logs remain the acceptance criteria: replay update resolution, story raw mention resolution, caption getter resolution, reel quality discovery, video-version discovery, and reel download gate discovery.

The implementation must prefer semantic/structural discovery and runtime capability checks over hard-coded obfuscated names. Unsupported capabilities must degrade to disabled feature state without affecting Instagram startup or unrelated hooks.
