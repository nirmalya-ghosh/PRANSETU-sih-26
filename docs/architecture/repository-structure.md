# Repository Structure

The repository is intentionally divided into independently deployable domains.

```text
android/   Citizen Android application
backend/   FastAPI API and integration services
web/       EOC web application
ai/        PRANSETU Intelligence
scripts/   Development/testing/deployment helpers
.github/   CI and contribution automation
docs/      System contracts and architecture
```

Keep domain boundaries explicit. Do not introduce cross-module coupling merely for convenience.
