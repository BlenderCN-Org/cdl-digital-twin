package at.ac.tuwien.big

import at.ac.tuwien.big.entity.state.*

object States {
    val idle = RoboticArmState("Idle")
    val approach = RoboticArmState(
            name = "Approach",
            basePosition = 0.0,
            mainArmPosition = 1.50,
            secondArmPosition = -0.12,
            wristPosition = 0.0,
            gripperPosition = 1.5)
    val pickup = RoboticArmState(
            name = "Pickup",
            basePosition = 0.0,
            mainArmPosition = 1.50,
            secondArmPosition = -0.12,
            wristPosition = 0.0,
            gripperPosition = -0.40
    )
    val lift = RoboticArmState(
            name = "Lift",
            basePosition = 0.0,
            mainArmPosition = 1.315,
            secondArmPosition = -0.12,
            wristPosition = 0.0,
            gripperPosition = -0.40
    )
    val park = RoboticArmState(
            name = "Park",
            basePosition = 3.142,
            mainArmPosition = 1.40,
            secondArmPosition = -1.55,
            wristPosition = -1.5,
            gripperPosition = -0.40
    )
    val halfRelease = RoboticArmState(
            name = "Half Release",
            basePosition = 3.142,
            mainArmPosition = 1.36,
            secondArmPosition = -1.34,
            wristPosition = -1.5,
            gripperPosition = -0.2
    )
    val fullRelease = RoboticArmState(
            name = "Full Release",
            basePosition = 3.142,
            mainArmPosition = 1.36,
            secondArmPosition = -1.334,
            wristPosition = -1.5,
            gripperPosition = 1.0
    )
    val wait = RoboticArmState(
            name = "Wait",
            basePosition = 3.142,
            mainArmPosition = 0.0,
            secondArmPosition = 0.0,
            wristPosition = -1.5,
            gripperPosition = 1.0
    )
    val retrieve = RoboticArmState(
            name = "Retrieve",
            basePosition = 3.142,
            mainArmPosition = 1.22,
            secondArmPosition = -1.23,
            wristPosition = -1.5,
            gripperPosition = 1.0
    )
    val retrieveGrip = RoboticArmState(
            name = "Retrieve Grip",
            basePosition = 3.142,
            mainArmPosition = 1.22,
            secondArmPosition = -1.23,
            wristPosition = -1.5,
            gripperPosition = -0.4
    )
    val depositGreen = RoboticArmState(
            name = "Deposit Green",
            basePosition = -1.745,
            mainArmPosition = 0.942,
            secondArmPosition = -0.89,
            wristPosition = 1.5,
            gripperPosition = -0.4
    )
    val releaseGreen = RoboticArmState(
            name = "Release Green",
            basePosition = -1.745,
            mainArmPosition = 0.942,
            secondArmPosition = -0.89,
            wristPosition = 1.5,
            gripperPosition = 0.5
    )
    val depositRed = RoboticArmState(
            name = "Deposit Red",
            basePosition = -1.449,
            mainArmPosition = 0.942,
            secondArmPosition = -0.89,
            wristPosition = 1.5,
            gripperPosition = -0.4
    )
    val releaseRed = RoboticArmState(
            name = "Release Red",
            basePosition = -1.449,
            mainArmPosition = 0.942,
            secondArmPosition = -0.89,
            wristPosition = 1.5,
            gripperPosition = 0.5
    )
    val sliderHomePosition = SliderState(
            "Slider Home", sliderPosition = 0.08
    )
    val sliderPushedPosition = SliderState(
            "Slider Push", sliderPosition = 0.42
    )
    val adjusterHomePosition = ConveyorState(
            "Adjuster Home", adjusterPosition = 1.669
    )
    val adjusterPushedPosition = ConveyorState(
            "Adjuster Push", adjusterPosition = 1.909
    )

    val none = TestingRigState("None", objectCategory = ObjectCategory.NONE)
    val green = TestingRigState("Green", objectCategory = ObjectCategory.GREEN)
    val red = TestingRigState("Red", objectCategory = ObjectCategory.RED)

    val slider_pushed = SliderTransition(sliderHomePosition, sliderPushedPosition)
    val slider_home = SliderTransition(sliderPushedPosition, sliderHomePosition)
    val adjuster_pushed = ConveyorTransition(adjusterHomePosition, adjusterPushedPosition)
    val adjuster_home = ConveyorTransition(adjusterPushedPosition, adjusterHomePosition)

    val none_green = TestingRigTransition(none, green)
    val none_red = TestingRigTransition(none, red)
    val to_none = TestingRigTransition(none, none)

    val idle_approach = RoboticArmTransition(idle, approach)
    val approach_pickup = RoboticArmTransition(approach, pickup)
    val pickup_lift = RoboticArmTransition(pickup, lift, mainArmSpeed = 0.2, secondArmSpeed = 0.2)
    val lift_park = RoboticArmTransition(lift, park)
    val park_halfrelease = RoboticArmTransition(park, halfRelease, mainArmSpeed = 0.1, secondArmSpeed = 0.1)
    val halfrelease_fullrelease = RoboticArmTransition(halfRelease, fullRelease, mainArmSpeed = 0.1, secondArmSpeed = 0.1)
    val fullrelease_wait = RoboticArmTransition(fullRelease, wait)
    val wait_retrieve = RoboticArmTransition(wait, retrieve)
    val retrieve_retrievegrip = RoboticArmTransition(retrieve, retrieveGrip)
    val retrievegrip_depositgreen = RoboticArmTransition(retrieveGrip, depositGreen)
    val retrievegrip_depositred = RoboticArmTransition(retrieveGrip, depositRed)
    val depositgreen_releasegreen = RoboticArmTransition(depositGreen, releaseGreen)
    val depositred_releasered = RoboticArmTransition(depositRed, releaseRed)
    val releasered_idle = RoboticArmTransition(releaseRed, idle)
    val releasegreen_idle = RoboticArmTransition(releaseGreen, idle)

    val roboticArm = mapOf(
            Pair(idle.name, idle),
            Pair(approach.name, approach),
            Pair(pickup.name, pickup),
            Pair(lift.name, lift),
            Pair(park.name, park),
            Pair(halfRelease.name, halfRelease),
            Pair(fullRelease.name, fullRelease),
            Pair(wait.name, wait),
            Pair(retrieve.name, retrieve),
            Pair(retrieveGrip.name, retrieveGrip),
            Pair(depositGreen.name, depositGreen),
            Pair(releaseGreen.name, releaseGreen),
            Pair(depositRed.name, depositRed),
            Pair(releaseRed.name, releaseRed)
    )

    val slider = mapOf(
            Pair(sliderHomePosition.name, sliderHomePosition),
            Pair(sliderPushedPosition.name, sliderPushedPosition)
    )
    val conveyor = mapOf(
            Pair(adjusterHomePosition.name, adjusterHomePosition),
            Pair(adjusterPushedPosition.name, adjusterPushedPosition)
    )

    val testingRig = mapOf(
            Pair(none.name, none),
            Pair(green.name, green),
            Pair(red.name, red)
    )

    val all = roboticArm.values union slider.values union conveyor.values union testingRig.values

    fun matchState(roboticArmState: RoboticArmState): RoboticArmState? {
        return roboticArm.values.filter { match(it, roboticArmState) }.firstOrNull()
    }

    fun matchState(sliderState: SliderState): SliderState? {
        return slider.values.filter { match(it, sliderState) }.firstOrNull()
    }

    fun matchState(conveyorState: ConveyorState): ConveyorState? {
        return conveyor.values.filter { match(it, conveyorState) }.firstOrNull()
    }

    private fun match(a: State, b: State): Boolean {
        return if (a is RoboticArmState && b is RoboticArmState) {
            similar(a.basePosition, b.basePosition)
                    && similar(a.mainArmPosition, b.mainArmPosition)
                    && similar(a.secondArmPosition, b.secondArmPosition)
                    && similar(a.wristPosition, b.wristPosition)
                    && similar(a.gripperPosition, b.gripperPosition)
        } else if (a is SliderState && b is SliderState) {
            similar(a.sliderPosition, b.sliderPosition)
        } else if (a is ConveyorState && b is ConveyorState) {
            similar(a.adjusterPosition, b.adjusterPosition)
        } else {
            false
        }
    }

    private fun similar(a: Double, b: Double): Boolean {
        return Math.abs(a - b) <= 0.02
    }

    fun transform(transition: Transition): List<String> {
        return when (transition) {
            is RoboticArmTransition -> {
                val target = transition.targetState
                listOf(
                        "base-goto ${target.basePosition} ${transition.baseSpeed}",
                        "main-arm-goto ${target.mainArmPosition} ${transition.mainArmSpeed}",
                        "second-arm-goto ${target.secondArmPosition} ${transition.secondArmSpeed}",
                        "wrist-goto ${target.wristPosition}",
                        "gripper-goto ${target.gripperPosition}"
                )
            }
            is SliderTransition -> {
                listOf("slider-goto ${transition.targetState.sliderPosition}")
            }
            is ConveyorTransition -> {
                listOf("adjuster-goto ${transition.targetState.adjusterPosition}")
            }
            else -> emptyList()
        }
    }
}
