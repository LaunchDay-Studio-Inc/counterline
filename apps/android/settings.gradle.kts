pluginManagement {
    repositories {
        google()
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

rootProject.name = "CounterLine"

include(":app")

include(":core:model")
include(":core:data")
include(":core:database")
include(":core:domain")
include(":core:designsystem")
include(":core:engine")
include(":core:content")

include(":feature:home")
include(":feature:repertoire")
include(":feature:drill")
include(":feature:modelgames")
include(":feature:plans")
include(":feature:deviations")
include(":feature:exam")
include(":feature:progress")
include(":feature:settings")
include(":feature:learn")
include(":feature:mistakereview")
include(":feature:quick5")
include(":feature:practice")
include(":feature:onboarding")
include(":feature:blindfold")
include(":feature:coach")
include(":feature:notebook")
include(":feature:pgnimport")
include(":feature:preppack")
include(":feature:tacticalmotifs")
include(":feature:transitiontrainer")
