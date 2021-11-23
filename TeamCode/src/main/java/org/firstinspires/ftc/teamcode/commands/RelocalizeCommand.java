package org.firstinspires.ftc.teamcode.commands;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.arcrobotics.ftclib.command.CommandBase;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.subsystems.DistanceSensors;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/*
Command that will keep a running average the position of the robot using the gyro and the distance sensors.

Command can be told to run for a set amount of time for now, and also can be interrupted.

We will use a supplier for the gyro angle in order to ensure that we can adapt to future
ways of obtaining heading (for example three-wheel odometry which doesn't use the IMU).
 */
public class RelocalizeCommand extends CommandBase {

    private static final double FORWARD_SENSOR_OFFSET = 65.0;
    private static final double LEFT_SENSOR_OFFSET = 65.0;
    private static final double RIGHT_SENSOR_OFFSET = 65.0;
    private final DistanceSensors distanceSensors;
    private final DoubleSupplier headingSupplier;
    private final Consumer<Pose2d> poseConsumer;
    private final boolean redSide;
    private final ElapsedTime timer = new ElapsedTime();
    private final int millis;
    private boolean isUsingTimer;
    private boolean firstRun = true;

    private Pose2d averagePosition = new Pose2d();

    public RelocalizeCommand(Consumer<Pose2d> poseConsumer, DistanceSensors distanceSensors, DoubleSupplier headingSupplier, boolean redSide) {
        this(poseConsumer, distanceSensors, headingSupplier, redSide, 0);
        isUsingTimer = false;
    }

    /**
     * @param headingSupplier Supplier of the heading of the robot IN RADIANS.
     */
    public RelocalizeCommand(Consumer<Pose2d> poseConsumer, DistanceSensors distanceSensors, DoubleSupplier headingSupplier, boolean redSide, int millis) {
        super();
        this.distanceSensors = distanceSensors;
        this.headingSupplier = headingSupplier;
        this.poseConsumer = poseConsumer;
        this.redSide = redSide;
        this.millis = millis;
        this.isUsingTimer = true;
        addRequirements(distanceSensors);
    }

    @Override
    public void initialize() {
        //Start taking range measurements from the sensors
        distanceSensors.startReading();
        timer.reset();
    }

    @Override
    public void execute() {
        //Find our current heading once so we dont have to keep reading it
        double heading = headingSupplier.getAsDouble();

        //test for possible invalid values
        if (distanceSensors.getForwardRange(DistanceUnit.INCH) < 8 ||
                distanceSensors.getForwardRange(DistanceUnit.INCH) > 96 ||
                distanceSensors.getLeftRange(DistanceUnit.INCH) < 3 ||
                distanceSensors.getLeftRange(DistanceUnit.INCH) > 30 ||
                distanceSensors.getRightRange(DistanceUnit.INCH) < 3 ||
                distanceSensors.getRightRange(DistanceUnit.INCH) > 30) return;

        //Find our forward distance (x in field coordinates)
        double x = (FORWARD_SENSOR_OFFSET - distanceSensors.getForwardRange(DistanceUnit.INCH)) * Math.cos(heading);

        //Find our side distance (y in field coordinates)
        double y = (redSide) ?
                (distanceSensors.getRightRange(DistanceUnit.INCH) * Math.cos(heading)) - RIGHT_SENSOR_OFFSET :
                LEFT_SENSOR_OFFSET - (distanceSensors.getLeftRange(DistanceUnit.INCH) * Math.cos(heading));


        //Put them together in a position
        Pose2d currentPosition = new Pose2d(x, y, heading);

        //if its the first run we need to make sure we have an initial position for the average to work
        if (firstRun) {
            averagePosition = new Pose2d(currentPosition.getX(), currentPosition.getY(), currentPosition.getHeading());
            firstRun = false;
        } else {
            //Average the two
            averagePosition = new Pose2d(
                    (averagePosition.getX() + currentPosition.getX()) / 2.0,
                    (averagePosition.getY() + currentPosition.getY()) / 2.0,
                    AngleUnit.normalizeRadians((averagePosition.getHeading() + currentPosition.getHeading()) / 2.0)
            );
        }

        //Update the user with the new position
        poseConsumer.accept(averagePosition);
    }

    //If we are using a timer, then see if its expired; otherwise, just return false.
    @Override
    public boolean isFinished() {
        return isUsingTimer && timer.milliseconds() > millis;
    }

    @Override
    public void end(boolean interrupted) {
        distanceSensors.stopReading();
    }


}