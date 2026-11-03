plugins {
    // Minecraft バージョンに応じて fabric-loom / fabric-loom-remap を適用する
    id("dev.kikugie.loom-back-compat")
    `maven-publish`
}

fun prop(key: String): String = sc.properties[key]

version = "${prop("mod.version")}+mc${sc.current.version}"
group = prop("mod.group")
base.archivesName = prop("mod.archives_name")

val javaVersion: Int = if (sc.current.parsed >= "26.1") 25 else 17

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.terraformersmc.com/releases/", "TerraformersMC", "com.terraformersmc")
    strictMaven("https://maven.shedaniel.me/", "shedaniel", "me.shedaniel.cloth", "me.shedaniel")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // 難読化ありのバージョンでは Mojang マッピングを適用する（26.1+ は何もしない）
    loomx.applyMojangMappings()

    // 26.1+ では loom-back-compat が mod* 設定を implementation 等に読み替える
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
    modImplementation("com.terraformersmc:modmenu:${prop("deps.modmenu")}")

    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${prop("deps.cloth_config")}")
    include("me.shedaniel.cloth:cloth-config-fabric:${prop("deps.cloth_config")}")

    // Litematica（任意、スキマティックのドラッグ＆ドロップ等の互換）
    modCompileOnly("maven.modrinth:litematica:${prop("deps.litematica")}")
    modCompileOnly("maven.modrinth:malilib:${prop("deps.malilib")}")

    // Sodium / Nvidium（任意、1.19.4 のみ）
    prop("deps.sodium").takeIf { it.isNotEmpty() }?.let { modCompileOnly("maven.modrinth:sodium:$it") }
    prop("deps.nvidium").takeIf { it.isNotEmpty() }?.let { modCompileOnly("maven.modrinth:nvidium:$it") }
}

loom {
    runConfigs.all {
        // バージョンごとにワールドや設定を分ける
        runDirectory = rootProject.file("run/${sc.current.version}")
        // ./gradlew :<version>:runClient -PmixinAudit で起動直後に全 Mixin を強制適用して検証する
        if (project.hasProperty("mixinAudit")) {
            property("asttweaks.mixinAudit", "true")
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft" to prop("mod.mc_compat"),
        "java" to javaVersion.toString(),
        "loader" to prop("deps.loader_min"),
        "modmenu" to prop("deps.modmenu_min"),
        "cloth" to prop("deps.cloth_min"),
    )
    props.forEach { (key, value) -> inputs.property(key, value) }

    filesMatching("fabric.mod.json") { expand(props) }

    // Mixin はバージョンごとに有無が変わるので、設定ファイルのクラス一覧をここで組み立てる
    fun jsonList(vararg names: String) = names.joinToString(",\n    ") { "\"$it\"" }
    val legacy = sc.current.parsed < "26.1"
    val mixinProps = mapOf(
        "java" to "JAVA_$javaVersion",
        "server_mixins" to (if (legacy) jsonList("DebugInfoSenderMixin") else jsonList("ServerDebugSubscribersMixin")),
        "client_mixins" to (if (legacy)
            jsonList("VillageDebugRendererAccessor", "VillageDebugRendererBrainAccessor", "VoidTradeInputMixin",
                "MerchantScreenAccessor", "MerchantScreenHandlerAccessor")
        else jsonList("ClientInputAccessor", "ClientDebugSubscriberMixin")),
        // Sodium / Nvidium 互換 Mixin は 1.19.4 だけ。他のバージョンでは空の設定にする
        "sodium_plugin" to (if (sc.current.version == "1.19.4") "\"plugin\": \"com.astral.asttweaks.mixin.sodium.SodiumMixinPlugin\"," else ""),
        "sodium_mixins" to (if (sc.current.version == "1.19.4") jsonList("SodiumFluidRendererMixin") else ""),
        "nvidium_plugin" to (if (sc.current.version == "1.19.4") "\"plugin\": \"com.astral.asttweaks.mixin.nvidium.NvidiumMixinPlugin\"," else ""),
        "nvidium_mixins" to (if (sc.current.version == "1.19.4")
            jsonList("RegionManagerMixin", "RegionMixin", "RenderPipelineMixin", "ShaderLoaderMixin", "SodiumRenderSectionManagerMixin")
        else ""),
    )
    mixinProps.forEach { (key, value) -> inputs.property("mixin_$key", value) }
    filesMatching("*.mixins.json") { expand(mixinProps) }
}

tasks.withType<Jar>().configureEach {
    val archivesName = base.archivesName.get()
    inputs.property("archivesName", archivesName)
    from(rootProject.file("LICENSE")) { rename { "${it}_$archivesName" } }
}

// 各バージョンの jar をルートの build/libs/ に集める（chiseledBuild から呼ばれる）
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds the mod jar and copies it to the root build/libs/"
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
