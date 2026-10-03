pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BaiViet"

// ── App ──
include(":app")

// ── Core modules ──
include(":core:cards")
include(":core:engine")
include(":core:ai")
include(":core:ui")
include(":core:data")

// ── Game modules ──
include(":game:tienlen")
include(":game:samloc")
include(":game:phom")
include(":game:maubinh")
include(":game:xidach")
include(":game:poker")
include(":game:lieng")
include(":game:bacay")
