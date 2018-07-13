package at.ac.tuwien.big.api

import at.ac.tuwien.big.*
import at.ac.tuwien.big.entity.state.*
import at.ac.tuwien.big.entity.transition.*
import at.ac.tuwien.big.sm.BasicState
import at.ac.tuwien.big.sm.ChoiceState
import at.ac.tuwien.big.sm.Job
import at.ac.tuwien.big.sm.StateBase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.javalin.ApiBuilder.*
import io.javalin.Javalin
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.websocket.api.Session
import java.io.StringWriter

/**
 * Controller for web-based APIs: Static frontend, HTTP API and WebSocket
 */
class WebController(private val mqtt: MQTT,
                    private val messageController: MessageController,
                    private val jobController: JobController,
                    private val timeSeriesDatabase: TimeSeriesDatabase) {

    data class TopicMessage(val topic: String, val message: String)

    private val channel = Channel<TopicMessage>(Channel.UNLIMITED)
    private var app: Javalin = Javalin.create()

    private var session: Session? = null
    private val gson: Gson = GsonBuilder()
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory.of(StateBase::class.java, "type")
                            .registerSubtype(BasicState::class.java, "BasicState")
                            .registerSubtype(ChoiceState::class.java, "ChoiceState")
            )
            .create()

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
                println("Received: $message")
                if (message != null) {
                    println("Message: $message")
                    if (message == "idle") {
                        val transitions = PickAndPlaceControllerSimulation.transform(StateMachine.Transitions.park_idle)
                        for (t in transitions) {
                            mqtt.send(t)
                        }
                    } else {
                        mqtt.send(message)
                    }
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
            put("/messageRate") { ctx -> messageController.messageRate = ctx.body().toInt() }
            get("/messageRate") { messageController.messageRate }
            put("/messageRate") { ctx -> messageController.messageRate = ctx.body().toInt() }
            get("/autoPlay") { ctx -> ctx.json(messageController.autoPlay) }
            put("/autoPlay") { ctx -> messageController.autoPlay = ctx.body().toBoolean() }
            get("/recording") { ctx -> ctx.json(messageController.recording) }
            put("/recording") { ctx -> messageController.recording = ctx.body().toBoolean() }
            put("/resetData") { timeSeriesDatabase.resetDatabase() }
            post("/moveEnvironment") { ctx ->
                run {
                    val env = gson.fromJson(ctx.body(), Environment::class.java)
                    val commands = mutableListOf<String>();
                    if (env.roboticArmState != null) {
                        commands.addAll(StateObserver.transform(RoboticArmTransition(RoboticArmState(), env.roboticArmState)))
                    }
                    if (env.conveyorState != null) {
                        commands.addAll(StateObserver.transform(ConveyorTransition(ConveyorState(), env.conveyorState)))
                    }
                    if (env.sliderState != null) {
                        commands.addAll(StateObserver.transform(SliderTransition(SliderState(), env.sliderState)))
                    }
                    if (env.testingRigState != null) {
                        commands.addAll(StateObserver.transform(TestingRigTransition(TestingRigState(), env.testingRigState)))
                    }

                    for (c in commands) {
                        mqtt.send(c)
                    }
                }
            }
            get("/all") { ctx -> ctx.json(s.all) }
            get("/roboticArmState") { ctx -> ctx.json(PickAndPlaceController.roboticArmSnapshot) }
            get("/sliderState") { ctx -> ctx.json(PickAndPlaceController.sliderSnapshot) }
            get("/conveyorState") { ctx -> ctx.json(PickAndPlaceController.conveyorSnapshot) }
            get("/testingRigState") { ctx -> ctx.json(PickAndPlaceController.testingRigSnapshot) }
            put("/roboticArmState") { ctx ->
                run {
                    val match = StateObserver.stateMachine?.all()?.find { it.name == ctx.body() }
                    if (match != null)
                        send(RoboticArmTransition(RoboticArmState(), match.environment.roboticArmState!!))
                }
            }
            put("/sliderState") { ctx ->
                run {
                    val match = StateObserver.stateMachine?.all()?.find { it.name == ctx.body() }
                    if (match != null)
                        send(SliderTransition(SliderState(), match.environment.sliderState!!))
                }
            }
            put("/conveyorState") { ctx ->
                run {
                    val match = StateObserver.stateMachine?.all()?.find { it.name == ctx.body() }
                    if (match != null)
                        send(ConveyorTransition(ConveyorState(), match.environment.conveyorState!!))
                }
            }
            put("/testingRigState") { ctx ->
                run {
                    val match = StateObserver.stateMachine?.all()?.find { it.name == ctx.body() }
                    if (match != null)
                        send(TestingRigTransition(TestingRigState(), match.environment.testingRigState!!))
                }
            }

            get("/jobs") { ctx -> ctx.json(jobController.getJobs()) }
            get("/jobs/:id") { ctx ->
                run {
                    val id = ctx.param("id") ?: ""
                    ctx.json(jobController.getJob(id) ?: Any())
                }
            }
            put("/jobs/:id") { ctx ->
                run {
                    try {
                        val id = ctx.param("id") ?: ""
                        val job = jobController.getJob(id)
                        if (job != null) {
                            val newJob = gson.fromJson(ctx.body(), Job::class.java)
                            jobController.setJob(newJob)
                            StateObserver.stateMachine = StateMachine(newJob.states)
                            ctx.status(200)
                        } else {
                            ctx.status(404)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            post("/jobs") { ctx ->
                run {
                    val newJob = gson.fromJson(ctx.body(), Job::class.java)
                    val id = jobController.addJob(newJob)
                    StateObserver.stateMachine = StateMachine(newJob.states)
                    ctx.json(id)
                }
            }
            post("/jobFile") { ctx ->
                run {
                    val writer = StringWriter()
                    IOUtils.copy(ctx.uploadedFile("job")?.content, writer)
                    val newJob = gson.fromJson(writer.toString(), Job::class.java)
                    val id = jobController.addJob(newJob)
                    StateObserver.stateMachine = StateMachine(newJob.states)
                    ctx.json(id)
                }
            }
            put("/selectedJob") { ctx ->
                run {
                    jobController.setSelectedJob(ctx.body())
                    messageController.autoPlay = false
                    StateObserver.stateMachine = StateMachine(jobController.selected.states)
                }
            }
            get("/selectedJob") { ctx ->
                ctx.json(jobController.selected)
            }
            put("/reset") { ctx ->
                run {
                    messageController.reset()
                    ctx.status(200)
                }
            }
        }
    }

    private fun send(match: Transition?) {
        if (match != null) {
            val commands = StateObserver.transform(match)
            for (c in commands) {
                mqtt.send(c)
            }
        }
    }
}
