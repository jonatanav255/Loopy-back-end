# Rules for Claude

## Build & Verify

- After every change or phase, the application must compile and run successfully.
- If tests exist, all tests must pass before considering any work done.
- Never leave the codebase in a broken state.
- Run `mvn compile -q` to verify compilation after changes.
- Run `mvn test` to verify tests before finishing any task.
- Both backend AND frontend must be in a working state after every change. If a backend change affects the frontend (or vice versa), verify both.

## Dependency Guide (REQUIRED)

Every external dependency API used in source code must be documented in `DEPENDENCY_GUIDE.md` at the project root.

### Source files — one-liner reference
After the `package` statement and before `import`s, add:
```java
// Dependencies: @Annotation, ClassName, methodName — see DEPENDENCY_GUIDE.md
```
Skip the one-liner if the file only uses our own code (e.g. DTOs, enums).

### DEPENDENCY_GUIDE.md — entry format
For each new external API used, add an entry:
```
### `name`
**From:** `full.package.name`
**Used in:** `FileName.java`

Prose explanation of what it does and why we use it.
```

### What goes in the guide vs source files
- **DEPENDENCY_GUIDE.md**: Explanations of what dependency-provided APIs do (annotations, classes, methods from Spring, JPA, JJWT, etc.)
- **Source files**: Normal Javadoc and business logic comments stay in the source — only dependency explanations go in the guide

### When to update
- When creating a new file that uses external APIs → add the one-liner + guide entries
- When adding a new external API to an existing file → update the one-liner + add guide entry
- When removing usage of an external API → clean up the one-liner + remove guide entry if no longer used anywhere

## Project Plan

The full project plan (10 phases) is at `docs/PROJECT_PLAN.md`. Reference it for phase details, task breakdowns, and build order.
