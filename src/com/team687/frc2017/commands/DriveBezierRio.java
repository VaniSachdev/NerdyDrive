package com.team687.frc2017.commands;

import java.util.ArrayList;

import com.team687.frc2017.Constants;
import com.team687.frc2017.Robot;
import com.team687.frc2017.utilities.BezierCurve;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Drive a path generated from Bezier curve based on 1241
 *
 * @author tedlin
 *
 */

public class DriveBezierRio extends Command {

    private BezierCurve m_path;
    private double m_basePower = 1; // always equal to 1
    private double m_straightPower;
    private boolean m_straightPowerIsDynamic;
    private ArrayList<Double> m_heading;
    private ArrayList<Double> m_arcLength;
    private int m_counter;

    private boolean m_pathIsFinished;

    public DriveBezierRio(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3,
	    double straightPower, boolean straightPowerIsDynamic) {
	m_path = new BezierCurve(x0, y0, x1, y1, x2, y2, x3, y3);
	m_straightPower = straightPower;
	m_straightPowerIsDynamic = straightPowerIsDynamic;
    }

    /**
     * 
     * @param path
     * @param straightPower
     *            (postive if going forward (forward is side with climber), negative
     *            if going backwards)
     * @param straightPowerIsDynamic
     *            (true for paths with sharp turns)
     */
    public DriveBezierRio(double[] path, double straightPower, boolean straightPowerIsDynamic) {
	m_path = new BezierCurve(path[0], path[1], path[2], path[3], path[4], path[5], path[6], path[7]);
	m_straightPower = straightPower;
	m_straightPowerIsDynamic = straightPowerIsDynamic;
    }

    @Override
    protected void initialize() {
	SmartDashboard.putString("Current Command", "DriveBezierRio");
	Robot.drive.stopDrive();
	Robot.drive.resetEncoders();
	Robot.drive.shiftUp();

	m_path.calculateBezier();
	m_heading = m_path.getHeading();
	m_arcLength = m_path.getArcLength();

	m_counter = 0;
	m_pathIsFinished = false;
	m_basePower = 1 * Math.signum(m_straightPower);
    }

    @Override
    protected void execute() {
	if (m_counter < m_arcLength.size()) {
	    if (Robot.drive.getDrivetrainTicks() < m_arcLength.get(m_counter)) {
		double robotAngle = (360 - Robot.drive.getCurrentYaw()) % 360;
		double error = -m_heading.get(m_counter) - robotAngle;
		// double expectedDeltaHeading = 0;
		// if (m_counter >= 1) {
		// expectedDeltaHeading = Math.abs(m_heading.get(m_counter) -
		// m_heading.get(m_counter - 1));
		// }

		error = (error > 180) ? error - 360 : error;
		error = (error < -180) ? error + 360 : error;

		double rotPower = Constants.kRotPBezier * error;
		double straightPower = m_straightPower;
		double direction = Math.signum(m_straightPower);

		// dynamic straight power
		if (m_straightPowerIsDynamic) {
		    straightPower = direction * m_basePower / (Math.abs(error) * Constants.kStraightPowerAdjuster);
		}

		// limit straight power to maintain rotPower to straightPower ratio
		if (Math.abs(straightPower) > Constants.kMaxStraightPower) {
		    straightPower = Constants.kMaxStraightPower * direction;
		}

		double leftPow = rotPower + straightPower;
		double rightPow = rotPower - straightPower;
		Robot.drive.setPower(leftPow, rightPow);
	    } else {
		m_counter++;
	    }
	} else {
	    m_pathIsFinished = true;
	}
    }

    @Override
    protected boolean isFinished() {
	return m_pathIsFinished || Robot.drive.getDrivetrainTicks() >= m_arcLength.get(m_arcLength.size() - 1);
    }

    @Override
    protected void end() {
	Robot.drive.stopDrive();
    }

    @Override
    protected void interrupted() {
	end();
    }

}
