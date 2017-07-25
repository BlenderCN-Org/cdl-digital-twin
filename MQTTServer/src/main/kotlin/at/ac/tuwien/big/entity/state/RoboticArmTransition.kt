package at.ac.tuwien.big.entity.state

data class RoboticArmTransition(
        override val startState: RoboticArmState,
        override val targetState: RoboticArmState,
        val baseSpeed: Double = 1.0,
        val mainArmSpeed: Double = 1.0,
        val secondArmSpeed: Double = 1.0
) : Transition
