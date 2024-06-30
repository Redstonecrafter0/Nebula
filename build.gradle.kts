plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
    java
}

subprojects {
    afterEvaluate {
        val nightlyCommitHash = System.getenv("NIGHTLY_COMMIT")
        val preRelease = System.getenv("CI_PRERELEASE")
        version = "0.1.0${if (preRelease != null) "-${preRelease}" else ""}${if (nightlyCommitHash != null) "+${nightlyCommitHash}" else ""}"

        kotlin {
            jvmToolchain(11)
        }

        java {
            withSourcesJar()
        }

        val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
        dependsOn(tasks.dokkaJavadoc)
            from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
            archiveClassifier.set("javadoc")
        }

        signing {
            useGpgCmd()
        }

        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/Redstonecrafter0/Nebula")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
                maven {
                    name = "local"
                    url = uri(rootProject.layout.buildDirectory.dir("repos/local"))
                }
            }

            publications {
                publications.create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifact(dokkaJavadocJar)
                    groupId = "dev.redstones.nebula"
                    artifactId = "nebula-${project.group.toString().split(".").last()}"
                    pom {
                        name.set(rootProject.name)
                        description.set("An easy-to-use download manager")
                        url.set("https://github.com/Redstonecrafter0/Nebula")
                        licenses {
                            license {
                                name.set("GNU Affero General Public License version 3.0")
                                url.set("http://www.gnu.org/licenses/agpl-3.0.html")
                            }
                        }
                        developers {
                            developer {
                                name.set("Redstonecrafter0")
                                email.set("54239558+Redstonecrafter0@users.noreply.github.com")
                                organization.set("Redstonecrafter0")
                                organizationUrl.set("https://redstones.dev")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/Redstonecrafter0/Nebula")
                            developerConnection.set("scm:git:https://github.com/Redstonecrafter0/Nebula")
                            url.set("https://github.com/Redstonecrafter0/Nebula")
                        }
                    }
                    the<SigningExtension>().sign(this)
                }
            }
        }
    }
}
