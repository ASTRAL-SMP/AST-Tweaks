plugins {
    id("dev.kikugie.stonecutter")
}

// src/ に展開されているバージョン。"Set active project to X" タスクで切り替える。
// コミット前は settings.gradle.kts の vcsVersion に戻すこと（"Reset active project"）。
stonecutter active "26.1.2"

// バージョン間で名前だけが違うシンボルは文字列置換で吸収する（条件が偽のときは逆方向に置換される）
stonecutter parameters {
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "26.1") {
            replace("ClickType", "ContainerInput")
            replace("handleInventoryMouseClick", "handleContainerInput")
            replace("net.minecraft.world.entity.npc.Villager", "net.minecraft.world.entity.npc.villager.Villager")
        }
        string(current.parsed >= "1.21.2") {
            replace("BuiltInRegistries.ITEM.get(", "BuiltInRegistries.ITEM.getValue(")
            replace("BuiltInRegistries.BLOCK.get(", "BuiltInRegistries.BLOCK.getValue(")
            replace("BuiltInRegistries.ENTITY_TYPE.get(", "BuiltInRegistries.ENTITY_TYPE.getValue(")
        }
        string(current.parsed >= "1.20") {
            replace("getLevel()", "level()")
            replace("isOnGround()", "onGround()")
        }
    }
}

// 全バージョンをビルドして jar を build/libs/ に集める
tasks.register("chiseledBuild") {
    group = "project"
    description = "Builds every Minecraft version and collects the jars into build/libs/"
    dependsOn(subprojects.map { "${it.path}:buildAndCollect" })
}
