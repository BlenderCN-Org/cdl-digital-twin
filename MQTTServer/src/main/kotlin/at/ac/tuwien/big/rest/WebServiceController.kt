package at.ac.tuwien.big.rest

import at.ac.tuwien.big.*
import at.ac.tuwien.big.MQTT.sendMQTTTransitionCommand
import at.ac.tuwien.big.entity.state.ConveyorState
import at.ac.tuwien.big.entity.state.RoboticArmState
import at.ac.tuwien.big.entity.state.SliderState
import at.ac.tuwien.big.entity.state.TestingRigState
import at.ac.tuwien.big.entity.transition.ConveyorTransition
import at.ac.tuwien.big.entity.transition.RoboticArmTransition
import at.ac.tuwien.big.entity.transition.SliderTransition
import at.ac.tuwien.big.entity.transition.TestingRigTransition
import com.google.gson.Gson
import io.javalin.ApiBuilder.get
import io.javalin.ApiBuilder.put
import io.javalin.Javalin
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.eclipse.jetty.websocket.api.Session

/**
 * API Controller for WebSockets
 */
object WebServiceController {

    data class TopicMessage(val topic: String, val message: String)

    private val channel = Channel<TopicMessage>(Channel.UNLIMITED)
    private var app: Javalin = Javalin.create()

    private var session: Session? = null
    private val gson = Gson()

    init {
        app.enableCorsForOrigin("http://127.0.0.1:8081", "http://localhost:8081")
                .port(8080)
                .enableStaticFiles("/static")
        app.ws("/websocket") { ws ->
            ws.onConnect { session ->
                this.session = session
                println("Opened $session")
            }
            ws.onMessage { _, message ->
                println("Received: " + message)
                if (message != null) {
                    println("Message: $message")
                    MQTT.send(MQTT.actuatorTopic, message)
                }
            }
            ws.onClose { session, statusCode, reason -> println("Closed $session, $statusCode: $reason") }
            ws.onError { session, throwable -> println("Error: $session, $throwable") }
        }

        app.routes {

            get("/all") { ctx -> ctx.json(StateMachine.all) }
            get("/roboticArmState") { ctx -> ctx.json(PickAndPlaceController.roboticArmSnapshot) }
            get("/sliderState") { ctx -> ctx.json(PickAndPlaceController.sliderSnapshot) }
            get("/conveyorState") { ctx -> ctx.json(PickAndPlaceController.conveyorSnapshot) }
            get("/testingRigState") { ctx -> ctx.json(PickAndPlaceController.testingRigSnapshot) }

            put("/roboticArmState") { ctx ->
                run {
                    val match = StateMachine.roboticArm[ctx.body()]
                    if (match != null)
                        sendMQTTTransitionCommand(RoboticArmTransition(RoboticArmState(), match))
                }
            }
            put("/sliderState") { ctx ->
                run {
                    val match = StateMachine.slider[ctx.body()]
                    if (match != null)
                        sendMQTTTransitionCommand(SliderTransition(SliderState(), match))
                }
            }
            put("/conveyorState") { ctx ->
                run {
                    val match = StateMachine.conveyor[ctx.body()]
                    if (match != null)
                        sendMQTTTransitionCommand(ConveyorTransition(ConveyorState(), match))
                }
            }
            put("/testingRigState") { ctx ->
                run {
                    val match = StateMachine.testingRig[ctx.body()]
                    if (match != null)
                        sendMQTTTransitionCommand(TestingRigTransition(TestingRigState(), match))
                }
            }

            get("/messageRate") { ctx -> MessageController.messageRate }
            put("/messageRate") { ctx -> MessageController.messageRate = ctx.body().toInt() }

            get("/autoPlay") { ctx -> ctx.json(MessageController.autoPlay) }
            put("/autoPlay") { ctx -> MessageController.autoPlay = ctx.body().toBoolean() }

            get("/recording") { ctx -> ctx.json(MessageController.recording) }
            put("/recording") { ctx -> MessageController.recording = ctx.body().toBoolean() }

            put("/resetData") { TimeSeriesCollectionService.resetDatabase() }
        }
    }

    fun start() {
        app.start()
        while (true) {
            runBlocking {
                val data = channel.receive()
                if (session?.isOpen == true) {
                    session?.remote?.sendString(gson.toJson(data))
                }
            }
        }
    }

    fun push(topic: String, message: String) {
        runBlocking {
            channel.send(TopicMessage(topic, message))
        }
    }

    fun stop() {
        app.stop()
    }
}
