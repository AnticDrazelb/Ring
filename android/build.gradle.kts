// Top-level build file. Nothing is configured here; the plugins are declared
// with `apply false` so the versions live in one place (gradle/libs.versions.toml)
// and :app applies them.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
