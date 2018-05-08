package at.ac.tuwien.big.api

import at.ac.tuwien.big.MQTT
import at.ac.tuwien.big.MessageController
import at.ac.tuwien.big.PickAndPlaceController
import at.ac.tuwien.big.TimeSeriesDatabase
import at.ac.tuwien.big.entity.state.ConveyorState
import at.ac.tuwien.big.entity.state.RoboticArmState
import at.ac.tuwien.big.entity.state.SliderState
import at.ac.tuwien.big.entity.state.TestingRigState
import at.ac.tuwien.big.entity.transition.*
import com.google.gson.Gson
import io.javalin.ApiBuilder.get
import io.javalin.ApiBuilder.put
import io.javalin.Javalin
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.eclipse.jetty.websocket.api.Session
import at.ac.tuwien.big.StateMachine.States as s

/**
 * Controller for web-based APIs: Static frontend, HTTP API and WebSocket
 */
class WebController(private val mqtt: MQTT,
                    private val messageController: MessageController,
                    private val timeSeriesDatabase: TimeSeriesDatabase) {

    data class TopicMessage(val topic: String, val message: String)

    private val channel = Channel<TopicMessage>(Channel.UNLIMITED)
    private var app: Javalin = Javalin.create()

    private var session: Session? = null
    private val gson = Gson()

    init {
        messageController.subscribe(this::pushToWebSocket)
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
                    mqtt.send(message)
                }
            }
            ws.onClose { session, statusCode, reason -> println("Closed $session, $statusCode: $reason") }
            ws.onError { session, throwable -> println("Error: $session, $throwable") }
        }
        setupRoutes()
    }

    /**
     * Start forwarding messages to clients that arrive via [pushToWebSocket]
     */
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

    /**
     * Stop all web-based services services
     */
    fun stop() {
        app.stop()
    }

    /**
     * Push message to WebSocket
     */
    private fun pushToWebSocket(topic: String, message: String) {
        runBlocking {
            channel.send(TopicMessage(topic, message))
        }
    }

    /**
     * Definition of HTTP API
     */
    private fun setupRoutes() {
        app.routes {
            get("/messageRate") { ctx -> messageController.messageRate }
            put("/messageRate") { ctx -> messageController.messageRate = ctx.body().toInt() }
            get("/autoPlay") { ctx -> ctx.json(messageController.autoPlay) }
            put("/autoPlay") { ctx -> messageController.autoPlay = ctx.body().toBoolean() }
            get("/recording") { ctx -> ctx.json(messageController.recording) }
            put("/recording") { ctx -> messageController.recording = ctx.body().toBoolean() }
            put("/resetData") { timeSeriesDatabase.resetDatabase() }
            get("/all") { ctx -> ctx.json(s.all) }
            get("/roboticArmState") { ctx -> ctx.json(PickAndPlaceController.roboticArmSnapshot) }
            get("/sliderState") { ctx -> ctx.json(PickAndPlaceController.sliderSnapshot) }
            get("/conveyorState") { ctx -> ctx.json(PickAndPlaceController.conveyorSnapshot) }
            get("/testingRigState") { ctx -> ctx.json(PickAndPlaceController.testingRigSnapshot) }
            put("/roboticArmState") { ctx ->
                run {
                    val match = s.roboticArm[ctx.body()]
                    if (match != null)
                        send(RoboticArmTransition(RoboticArmState(), match))
                }
            }
            put("/sliderState") { ctx ->
                run {
                    val match = s.slider[ctx.body()]
                    if (match != null)
                        send(SliderTransition(SliderState(), match))
                }
            }
            put("/conveyorState") { ctx ->
                run {
                    val match = s.conveyor[ctx.body()]
                    if (match != null)
                        send(ConveyorTransition(ConveyorState(), match))
                }
            }
            put("/testingRigState") { ctx ->
                run {
                    val match = s.testingRig[ctx.body()]
                    if (match != null)
                        send(TestingRigTransition(TestingRigState(), match))
                }
            }
        }
    }

    private fun send(match: Transition?) {
        if (match != null) {
            val commands = PickAndPlaceController.transform(match)
            for (c in commands) {
                mqtt.send(c)
            }
        }
    }
}
