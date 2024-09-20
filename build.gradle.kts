val taskGroup = "Minecraft"

tasks.named<TaskReportTask>("tasks") {
    displayGroup = taskGroup
}

tasks.register("runNeoForgeClient") {
    group = taskGroup
    dependsOn(":neoforge:runClient")
}

tasks.register("runNeoForgeServer") {
    group = taskGroup
    dependsOn(":neoforge:runServer")
}

tasks.register("runFabricClient") {
    group = taskGroup
    dependsOn(":fabric:runClient")
}

tasks.register("runFabricServer") {
    group = taskGroup
    dependsOn(":fabric:runServer")
}
