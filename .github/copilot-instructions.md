<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan

-- NOTE: Repository workflow tips to avoid prerequisite/script detection issues --
- Always run feature work on a feature branch (named like `001-name`); if a precheck
	script fails because you're on `master`, switch to the intended feature branch first.
- If the `.specify` prerequisite script does not detect `spec.md` or `plan.md`, verify
	that those files live under `specs/<feature>/` and that the branch you're on contains them.
- When updating spec artifacts, commit and push your branch before running the prereq checker
	so the script can inspect the repository state matching CI expectations.
- If you need to force-check while on `master`, run the prereq script with `-Verbose` to
	surface detection logic and paths; consider running it from the feature branch instead.

These notes are for contributors to avoid missing-file false negatives from the prereq checker.
<!-- SPECKIT END -->
