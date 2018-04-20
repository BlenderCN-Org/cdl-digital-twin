package at.ac.tuwien.big

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class MqttServerApplication

fun main(args: Array<String>) {

    SpringApplication.run(MqttServerApplication::class.java, *args)
}
