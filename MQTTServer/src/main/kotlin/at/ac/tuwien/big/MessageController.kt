package at.ac.tuwien.big

import at.ac.tuwien.big.entity.state.*
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.*
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import javax.annotation.PreDestroy

@Controller
final class MessageController(val webSocket: SimpMessagingTemplate) : MqttCallback {

    final val qos = 0
    final val sensorTopic = "Sensor"
    final val actuatorTopic = "Actuator"
    final val detectionCameraTopic = "DetectionCamera"
    final val pickupCameraTopic = "PickupCamera"
    final val client: MqttClient
    final val gson: Gson
    var roboticArmState: RoboticArmState? = null
    var sliderState: SliderState? = null
    var conveyorState: ConveyorState? = null
    var testingRigState: TestingRigState? = null
    var autoPlay: Boolean = false
    var lock = Any()

    init {
        println("Connecting to MQTT endpoint.")
        client = MqttClient("tcp://localhost:1883", "Controller")
        val connOpts = MqttConnectOptions()
        connOpts.isCleanSession = true
        client.connect(connOpts)
        println("Established connection.")
        println("Subscribing to topic: $sensorTopic")
        client.setCallback(this)
        client.subscribe(sensorTopic)
        client.subscribe(detectionCameraTopic)
        client.subscribe(pickupCameraTopic)
        gson = Gson()
    }

    @PreDestroy
    fun cleanup() {
        client.disconnect()
        client.close()
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        when (topic) {
            sensorTopic -> {
                sendWebSocketMessageSensor(String(message!!.payload))
                handle(parse(String(message.payload)))
            }
            detectionCameraTopic -> {
                sendWebSocketMessageDetectionCamera(String(message!!.payload))
            }
            pickupCameraTopic -> {
                sendWebSocketMessagePickupCamera(String(message!!.payload))
            }
        }
    }

    fun parse(payload: String): State {
        try {
            val basicState = gson.fromJson(payload, BasicState::class.java)
            return when (basicState.entity) {
                "RoboticArm" -> gson.fromJson(payload, RoboticArmState::class.java)
                "Slider" -> gson.fromJson(payload, SliderState::class.java)
                "Conveyor" -> gson.fromJson(payload, ConveyorState::class.java)
                else -> BasicState()
            }
        } catch(e: Exception) {
            println(e.message)
        }
        return BasicState()
    }

    fun handle(state: State) {
        when (state) {
            is RoboticArmState -> {
                val match = States.matchState(state)
                if (match != null && state != match) {
                    roboticArmState = match
                }

            }
            is SliderState -> {
                val match = States.matchState(state)
                if (match != null && state != match) {
                    sliderState = match
                }
            }
            is ConveyorState -> {
                val match = States.matchState(state)
                if (match != null && state != match) {
                    conveyorState = match
                }
            }

        }
    }

    @Scheduled(fixedRate = 1000)
    fun issueCommands() {
        val context = Context(roboticArmState?.copy(), sliderState?.copy(), conveyorState?.copy(), States.red)
        val next = RobotController.next(context)
        println("Next: ${context.roboticArmState?.name}, ${context.sliderState?.name}, ${context.conveyorState?.name} -> ${next?.targetState?.name}")
        sendMQTTTransitionCommand(next)
        sendWebSocketMessageContext(gson.toJson(context))
    }

    override fun connectionLost(cause: Throwable?) {
        throw cause ?: Exception("Connection lost.")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        println("Delivery complete.")
    }

    fun sendMQTTTransitionCommand(transition: Transition?) {
        if (transition != null) {
            val commands = States.transform(transition)
            for (c in commands) {
                sendMQTTDirectCommand(c)
            }
        }
    }

    private fun sendMQTTDirectCommand(message: String) {
        val tmp = MqttMessage(message.toByteArray())
        tmp.qos = qos
        println("Sending via MQTT: $message")
        synchronized(lock) {
            client.publish(actuatorTopic, tmp)
        }
    }

    @MessageMapping("/actuator")
    fun receiveActuatorWebSocketMessage(command: String) {
        println("Actuator command: " + command)
        if (command.startsWith("goto:")) {
            val stateName = command.split(": ")[1]
            val state = States.all.filter { it.name == stateName }.first()
            val transition: Transition = when (state) {
                is RoboticArmState -> RoboticArmTransition(state, state)
                is SliderState -> SliderTransition(state, state)
                is ConveyorState -> ConveyorTransition(state, state)
                else -> BasicTransition(state, state)
            }
            sendMQTTTransitionCommand(transition)
        } else {
            sendMQTTDirectCommand(command)
        }
    }

    private fun sendWebSocketMessageSensor(message: String) {
        sendWebSocketMessage("/topic/sensor", message)
    }

    private fun sendWebSocketMessageContext(message: String) {
        sendWebSocketMessage("/topic/context", message)
    }

    private fun sendWebSocketMessagePickupCamera(message: String) {
        sendWebSocketMessage("/topic/pickupCamera", message)
    }

    private fun sendWebSocketMessageDetectionCamera(message: String) {
        sendWebSocketMessage("/topic/detectionCamera", message)
    }

    private fun sendWebSocketMessage(topic: String, message: String) {
        webSocket.send(topic, MessageBuilder.withPayload(message.toByteArray()).build())
    }
}
