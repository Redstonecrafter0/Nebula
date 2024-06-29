plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "Nebula"
include("core")
include("Minecraft")
include("GitHub")
include("Cloudflared")
include("K3d")
include("YtDlp")
