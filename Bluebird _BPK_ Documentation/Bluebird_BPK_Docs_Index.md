# Bluebird BPK Documentation

The BPK documentation is intentionally split into three documents.

## 1. Formal specification

**`BPS_v1_Formal_Specification.md`**

Defines the package contract: structure, manifest, runtime, icons, installer, installation layout, `.exe` descriptor, registry, validation/security, optimization, lifecycle, reserved APIs, compatibility, and conformance.

## 2. Developer guide

**`BPK_Developer_Guide.md`**

Practical tutorial for creating an application from scratch, writing the manifest, building the web UI, packaging, creating a custom installer, testing, installing, and preparing a release.

## 3. Store specification

**`Bluebird_Store_Submission_and_Repository_Spec.md`**

Defines community submission, catalog metadata, GitHub automation, validation, release assets, hashes, review, categories, screenshots, updates, and repository organization.

## Current versus future

The formal specification explicitly distinguishes functionality that exists in the current BPS v1 design from functionality reserved for future Bluebird releases. This prevents developers from accidentally depending on APIs that are not yet guaranteed.
