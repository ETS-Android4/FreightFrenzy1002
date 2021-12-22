package org.firstinspires.ftc.teamcode.commands

import com.arcrobotics.ftclib.command.ProfiledPIDCommand
import com.arcrobotics.ftclib.controller.wpilibcontroller.ProfiledPIDController
import com.arcrobotics.ftclib.trajectory.TrapezoidProfile
import org.firstinspires.ftc.teamcode.subsystems.Lift

class MoveLiftPositionCommand(private val lift: Lift, private val position: Lift.Positions ) : ProfiledPIDCommand(
        controller,
        lift::getLiftRawPosition,
        position.targetPosition.toDouble(),
        {output, _ -> lift.setLiftPower(output)},
        lift
) {

    companion object {

        private const val targetPosition = 1050.0

        private val controller = ProfiledPIDController(
                0.05, 0.0, 0.0,
                TrapezoidProfile.Constraints(
                        //TODO: Find this empirically
                        2000.0, //theoretical max velo of 5:1 ultraplanetary in ticks/sec
                        2000.0
                )
        )
    }

    init {
        controller.setTolerance(10.0)
    }

    override fun isFinished(): Boolean {
        return controller.atGoal()
    }
}