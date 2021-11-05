package org.firstinspires.ftc.teamcode.subsystems

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.arcrobotics.ftclib.command.SubsystemBase
import com.qualcomm.robotcore.hardware.*
import com.qualcomm.robotcore.util.Range
import kotlin.math.abs
import kotlin.math.cos

@Config
class Arm(private val hardwareMap: HardwareMap) : SubsystemBase() {

    //Calling the constructor of the superclass already registers this subsystem

    private val armMotor by lazy {hardwareMap.get(DcMotorEx::class.java, "arm")}


    var armState = ArmState.STOPPED

    private var power = 0.0
    private var firstRun = true

    //The annotations mean it can be seen by ftc dashboard (in kotlin)
    //Equivalent of doing public static in java.
    @JvmField
    var coefficients = PIDCoefficients(0.02, 0.0, 0.0)
    
    @JvmField
    var armGravityFeedforward: Double = 1.0 //TODO: Find this

    //TODO: Test this
    /*
    See the book Controls Engineering in FRC for an explanation of this and the equation
    for a theoretical gravity feedforward constant.

    Fg = m*g*(L/2)*cos(angle of arm)
     */


    private val armGravityController = PIDFController(coefficients,
            kF = { position, _ ->
                findGravityFF(position)
            }
    )
    
    companion object {

        //The tolerance of our controller in ticks
        private const val positionTolerance = 7

        //Encoder ticks per revolution of our motor
        private const val TICKS_PER_REV = 700.0

        //Distance in the encoder ticks from the bottom limit of the arms rotation to horizontal
        private const val ARM_TO_HORIZONTAL_TICKS_OFFSET = 50.0
    }


    //Current movement state of arm
    enum class ArmState {
        MOVING_MANUAL,
        MOVING_AUTO,
        HOLDING,
        STOPPED
    }

    //TODO: Find these
    //Encoder positions of the arm motor for different levels
    enum class ArmPosition(val targetPosition: Int) {
        DOWN(0),
        BOTTOM_LEVEL(12),
        MIDDLE_LEVEL(51),
        TOP_LEVEL(76)
        //CAP_LEVEL(4000)
    }

    init{
        //We can raise this but to be safe we are leaving it here for now
        armGravityController.setOutputBounds(-0.8, 0.8)
        register()
    }


    override fun periodic() {
        //On the first run set the zero power behavior and run mode
        if(firstRun){
            armMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            armMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        }

        when (armState) {
            ArmState.STOPPED -> { //if we are stopped, set it to 0.
                armMotor.power = 0.0
            }
            ArmState.HOLDING -> { //If we are holding, keep the arm at the position it was stopped at
                val currentPosition = armMotor.currentPosition
                if(abs(armGravityController.targetPosition - currentPosition) <= 8){
                    armMotor.power = findGravityFF(currentPosition.toDouble())
                } else {
                    armMotor.power = armGravityController.update(armMotor.currentPosition.toDouble()) //Hold the
                }
            }
            //If we are moving to a position, update the motor powers with the controller
            ArmState.MOVING_AUTO -> {
                val currentPosition = armMotor.currentPosition

                //Check if the current position is within our tolerance range
                if (abs(armGravityController.targetPosition - currentPosition) <= positionTolerance) {
                    //If it is, then make the new target the currentPosition
                    armGravityController.targetPosition = currentPosition.toDouble()
                    armState = ArmState.HOLDING //If we are at the target position, hold the arm
                } else {
                    armMotor.power = armGravityController.update(currentPosition.toDouble()) //else, move towards the position
                }
            }
            ArmState.MOVING_MANUAL -> { //If we are in manual movement, set the arm to that power
                armMotor.power = power
            }
        }

    }

    private fun findGravityFF(position: Double): Double {
        //Find the angle of the arm in degrees

        /*
        The arm starts at lower than 0 degrees so we do have to subtract the difference
        between that and the actual 0 position of the arm in ticks. Otherwise the controller
        would think that the arm started perfectly horizontal.
         */

        val angle = (position - ARM_TO_HORIZONTAL_TICKS_OFFSET) / TICKS_PER_REV * 360.0
        return cos(angle) * armGravityFeedforward
    }


    //Sets the arm to a manual power
    fun armPower(power: Double) {
        this.power = power
        armState = ArmState.MOVING_MANUAL
    }

    //Moves the arm to the specified position
    fun setArm(position: ArmPosition) {
        armGravityController.targetPosition = position.targetPosition.toDouble()
        armState = ArmState.MOVING_AUTO

    }

    //Holds the arm at the current angle
    fun hold() {
        armGravityController.targetPosition = armMotor.currentPosition.toDouble()
        armState = ArmState.HOLDING
    }


    //Sets a 0 power to the arm
    fun stop() {
        armState = ArmState.STOPPED
    }


}