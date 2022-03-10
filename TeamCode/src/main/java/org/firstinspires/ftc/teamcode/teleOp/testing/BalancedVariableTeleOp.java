package org.firstinspires.ftc.teamcode.teleOp.testing;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "Balanced Variable Co-TeleOp")
public class BalancedVariableTeleOp extends LinearOpMode {
    // EFFICIENCY INITIALIZATION
    DcMotor
            rightFront, leftFront, rightBack, leftBack, // All of the main motors
            carouselMotor, frontIntake, backIntake;  // The misc motors
    public static double
            rx, ry, lx, ly, // The controller specific x and y values
            power = 1; // Setting default power to full power
    public static float
            rightTrigger, leftTrigger; // Triggers
    public static boolean
            dPadRight, dPadLeft, dPadUp, dPadDown, // dPad Values
            A, B, X, Y, // Button Presses
            rightBumper, leftBumper, // Bumpers
            slowMode = false; // Conditionals
    public static final double
            DUCK_MULTIPLIER = 0.7;
    @Override
    public void runOpMode() {
        // "Drivers, pick up your controllers!" \\
        waitForStart();

        rightFront = hardwareMap.get(DcMotor.class, "rf");
        leftFront = hardwareMap.get(DcMotor.class, "lf");
        rightBack = hardwareMap.get(DcMotor.class, "rb");
        leftBack = hardwareMap.get(DcMotor.class, "lb");

        carouselMotor = hardwareMap.get(DcMotor.class,"carouselMotor");
        frontIntake = hardwareMap.get(DcMotor.class, "frontIntake");
        backIntake = hardwareMap.get(DcMotor.class, "backIntake");

        // Reverse the left motors so that positive values move the robot forwards, etc.
        leftFront.setDirection(DcMotorSimple.Direction.REVERSE);
        leftBack.setDirection(DcMotorSimple.Direction.REVERSE);

        // Begin the --misery-- code
        while (opModeIsActive()) {
            A = gamepad1.a;
            dPadRight = gamepad1.dpad_right;
            dPadLeft = gamepad1.dpad_left;
            rightBumper = gamepad1.right_bumper;
            leftBumper = gamepad1.left_bumper;
            rightTrigger = gamepad1.right_trigger;
            leftTrigger = gamepad1.left_trigger;

            // If slow mode button pressed and not slowMode,
            if (A && !slowMode) {
                power = 0.5;
                slowMode = true;
                //TODO maybe make modes a
                // separate class with mode.unSet()
                // and mode.set() values?
            }

            else if (!A && slowMode) {
                power = 1;
                slowMode = false;
            }

            rx = (gamepad1.right_stick_x);
            ry = -(gamepad1.right_stick_y);
            lx = 1.1 * (gamepad1.left_stick_x);
            ly = -(gamepad1.left_stick_y);

            leftFront.setPower(power * (ly + lx + rx));
            leftBack.setPower(power * (ly - lx + rx));
            rightFront.setPower(power * (ly - lx - rx));
            rightBack.setPower(power * (ly + lx - rx));
            // Merged into main
            if (dPadLeft || dPadRight) {
                int multiplier = 1;
                if (dPadLeft) multiplier = -1;
                carouselMotor.setPower(multiplier * DUCK_MULTIPLIER);
            }
            else if (carouselMotor.getPower() != 0) {
                carouselMotor.setPower(0);
            }
            // Merged into main
            if (rightTrigger > 0) {
                frontIntake.setPower(1);
            }
            else if (rightBumper) {
                frontIntake.setPower(-1);
            }
            else {
                frontIntake.setPower(0);
                backIntake.setPower(0);
            }
            if (leftTrigger > 0) {
                backIntake.setPower(1);
            }
            else if (leftBumper) {
                backIntake.setPower(-1);
            }
            else {
                frontIntake.setPower(0);
                backIntake.setPower(0);
            }
        }
    }
}

//TODO  what- this logic bends my mind ill figure this out later.
// where da swerve / where da turn?