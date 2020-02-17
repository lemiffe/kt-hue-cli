package com.example

import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpGet
import io.github.zeroone3010.yahueapi.Hue
import io.github.zeroone3010.yahueapi.Light
import io.github.zeroone3010.yahueapi.Room
import io.github.zeroone3010.yahueapi.State
import io.github.zeroone3010.yahueapi.StateBuilderSteps.OnOffStep
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import picocli.CommandLine
import java.io.File
import java.net.ConnectException
import java.util.*
import kotlin.system.exitProcess
import picocli.CommandLine.Command
import picocli.CommandLine.Option

class NotInitializedException(message: String): Exception(message)

const val CONFIG_FILE = "config.json"

enum class HueColor(val rgb: java.awt.Color?) {
    WHITE(java.awt.Color.WHITE),
    RED(java.awt.Color.RED),
    GREEN(java.awt.Color.GREEN),
    BLUE(java.awt.Color.BLUE),
    YELLOW(java.awt.Color.YELLOW),
    ORANGE(java.awt.Color.ORANGE),
    MAGENTA(java.awt.Color.MAGENTA),
    PINK(java.awt.Color.PINK),
    CYAN(java.awt.Color.CYAN)
}

class HueFactory() {
    var ip: String? = null
    var apiKey: String? = null
    var appName: String? = "HueApp"
    var hue: Hue? = null

    constructor(ip: String?, apiKey: String?, appName: String?) : this() {
        this.ip = ip
        this.apiKey = apiKey
        this.appName = appName
    }

    fun initializeHue() {
        if (this.apiKey?.isNotEmpty() == true) {
            this.hue = Hue(this.ip, this.apiKey)
            return
        }

        val apiKey = Hue
            .hueBridgeConnectionBuilder(this.ip)
            .initializeApiConnection(this.appName)

        this.apiKey = apiKey.get()
        this.hue = Hue(this.ip, this.apiKey)
    }

    fun getRooms(): List<Room> {
        checkInitialization()
        return this.hue?.rooms?.toList() ?: listOf()
    }

    fun getLights(room: Room): List<Light> {
        checkInitialization()
        return room.lights.toList()
    }

    fun turnOn(room: Room) {
        checkInitialization()
        room.setState((State.builder() as OnOffStep).on());
        room.setBrightness(254)
    }

    fun turnOn(light: Light) {
        checkInitialization()
        light.turnOn()
        light.setBrightness(254)
    }

    fun turnOff(room: Room) {
        checkInitialization()
        room.setState((State.builder() as OnOffStep).off());
    }

    fun turnOff(light: Light) {
        checkInitialization()
        light.turnOff()
    }

    fun switchColor(room: Room, color: java.awt.Color) {
        checkInitialization()
        room.setState(State.builder().color(color).on());
        room.setBrightness(254)
    }

    fun switchColor(light: Light, color: java.awt.Color) {
        checkInitialization()
        light.state = State.builder().color(color).on()
        light.setBrightness(254)
    }

    private fun checkInitialization() {
        if (this.apiKey == null || this.hue == null) {
            throw NotInitializedException("Hue not initialized")
        }
    }
}

@Serializable
data class Config(
    val ip: String? = null,
    var apiKey: String? = null,
    val appName: String? = null
)

@Serializable
data class HueConfig(
    val id: String? = null,
    val internalipaddress: String? = null
)

@Command(
    name = "kt-lights",
    description = ["Kotlin Hue Lights CLI Command"],
    mixinStandardHelpOptions = true,
    helpCommand = true
)
open class App : Runnable {
    @Option(names = ["-s", "--setup"], description = ["Run the setup process"])
    internal open var runSetup: Boolean = false

    @Option(names = ["-r", "--rooms"], description = ["Get a list of rooms"])
    internal open var getRooms: Boolean = false

    @Option(names = ["-l", "--lights"], description = ["Get a list of lights for a room"])
    internal open var getLights: String? = null

    @Option(names = ["-on", "--turnOn"], description = ["Turn on a room or light (based on the name)"])
    internal open var turnOn: String? = null

    @Option(names = ["-off", "--turnOff"], description = ["Turn off a room or light (based on the name)"])
    internal open var turnOff: String? = null

    @Option(names = ["-c", "--color"], description = ["Switch color of a room or light, used in combination with 'turnOn'"])
    internal open var color: HueColor? = null

    private fun configure(): Config {
        val json = Json(JsonConfiguration.Stable)
        val config: Config

        if (!File(CONFIG_FILE).exists() || runSetup) {
            val input = Scanner(System.`in`)
            println("App is not configured, let's do this now:")
            print("Enter a name for your Hue app: ")
            val hueAppName = input.nextLine()
            print("Enter the IP of your bridge (or enter \"search\" to automatically find it): ")
            var hueBridgeIp = input.nextLine()
            if (hueBridgeIp?.toLowerCase()?.trim() == "search") {
                try {
                    val (_, response, result) = "https://www.meethue.com/api/nupnp"
                        .httpGet().responseString()
                    val (payload, _) = result
                    if (response.isSuccessful && payload?.isNotEmpty() == true) {
                        val hueConfigs: List<HueConfig> = json.parse(HueConfig.serializer().list, payload)
                        if (hueConfigs.count() == 0) {
                            throw Exception("Hue bridge was not found in the response")
                        }
                        hueBridgeIp = hueConfigs.first().internalipaddress
                    } else {
                        throw Exception("Hue bridge locator proxy not found")
                    }
                } catch (exception: Exception) {
                    println("Couldn't find a Hue bridge (are you on the right network?)...")
                    print("Enter the IP of your bridge (you can find it at https://www.meethue.com/api/nupnp): ")
                    hueBridgeIp = input.nextLine()
                }
            }

            if (hueBridgeIp.isNullOrEmpty() || hueBridgeIp.trim().isEmpty() || hueBridgeIp == "search") {
                println("Hue Bridge IP wasn't set and is required to store the configuration (you can use $CONFIG_FILE.dist as an example if you want to manually configure it).")
                exitProcess(2)
            }

            // Save the configuration
            config = Config(hueBridgeIp, null, hueAppName)
            val jsonConfig = json.stringify(Config.serializer(), config)
            File(CONFIG_FILE).writeText(jsonConfig)

        } else {
            try {
                val jsonConfig = File(CONFIG_FILE).readText()
                config = json.parse(Config.serializer(), jsonConfig)
            } catch (exception: JsonDecodingException) {
                println("Invalid configuration (use $CONFIG_FILE.dist as an example if you want to manually configure it).")
                exitProcess(2)
            }
        }

        val ipRegex = Regex("(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])")
        if (config.ip.isNullOrEmpty() || !ipRegex.matches(config.ip)) {
            println("Invalid IP in $CONFIG_FILE (verify you are using an IPv4 address in the 'ip' field).")
            exitProcess(2)
        }

        return config
    }

    private fun updateApiKey(config: Config, apiKey: String) {
        val json = Json(JsonConfiguration.Stable)
        config.apiKey = apiKey
        val jsonConfig = json.stringify(Config.serializer(), config)
        File(CONFIG_FILE).writeText(jsonConfig)
    }

    override fun run() {
        val config = configure()

        val factory: HueFactory
        try {
            factory = HueFactory(config.ip, config.apiKey, config.appName)
            factory.initializeHue()
        } catch (exception: ConnectException) {
            println("Couldn't connect to Hue bridge (wrong IP or network)")
            exitProcess(2)
        }

        if (config.apiKey.isNullOrEmpty() && !factory.apiKey.isNullOrEmpty()) {
            updateApiKey(config, factory.apiKey!!)
        }

        if (getRooms) {
            val rooms = factory.getRooms()
            rooms.forEach { room -> println(room.name) }

        } else if (!getLights.isNullOrEmpty()) {
            val rooms = factory.getRooms().filter { it.name == getLights }
            if (rooms.isEmpty()) {
                println("Couldn't find any lights in room $getLights")
                exitProcess(2)
            }
            for (light in factory.getLights(rooms.first())) {
                println(light.name)
            }

        } else if (!turnOn.isNullOrEmpty()) {
            val rooms = factory.getRooms()
            val room: Room? = rooms.firstOrNull { it.name == turnOn }
            val light: Light? = sequence {
                rooms
                    .forEach { room ->
                        yieldAll(
                            factory.getLights(room).filter { it.name == turnOn }
                        )
                    }
            }.toList().firstOrNull()

            if (room == null && light == null) {
                println("Couldn't find any lights or rooms matching $turnOn")
                exitProcess(2)
            } else {
                if (color != null) {
                    println("Switching color of $turnOn to $color")
                    when {
                        room != null -> factory.switchColor(room, color!!.rgb!!)
                        light != null -> factory.switchColor(light, color!!.rgb!!)
                    }
                } else {
                    println("Turning on $turnOn")
                    when {
                        room != null -> factory.turnOn(room)
                        light != null -> factory.turnOn(light)
                    }
                }
            }

        } else if (!turnOff.isNullOrEmpty()) {
            val rooms = factory.getRooms()
            val room: Room? = rooms.firstOrNull { it.name == turnOff }
            val light: Light? = sequence {
                rooms
                    .forEach { room ->
                        yieldAll(
                            factory.getLights(room).filter { it.name == turnOff }
                        )
                    }
            }.toList().firstOrNull()

            if (room == null && light == null) {
                println("Couldn't find any lights or rooms matching $turnOff")
                exitProcess(2)
            } else {
                println("Turning off $turnOff")
                when {
                    room != null -> factory.turnOff(room)
                    light != null -> factory.turnOff(light)
                }
            }

        } else {
            // Show help
            CommandLine.usage(App(), System.err)
        }
    }
}

fun main(args: Array<String>) {
    CommandLine(App()).execute(*args)
}
