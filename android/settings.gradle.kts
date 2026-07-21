// Configuração raiz do projeto Gradle. Diz quais "módulos" existem (aqui só o
// :app) e de onde baixar as bibliotecas.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PrivateMessenger"
include(":app")
