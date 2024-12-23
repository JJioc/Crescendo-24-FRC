package frc.robot.subsystems;

import frc.robot.SwerveModule;
import frc.robot.Constants;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;

// import com.ctre.phoenix.*;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.util.PathPlannerLogging;
import com.ctre.phoenix6.configs.Pigeon2Configuration;

// import com.pathplanner.lib.path.PathPlannerTrajectory;

// import com.pathplanner.lib.path.PathPlannerTrajectory.State;
// import com.pathplanner.*;
//import com.pathplanner.lib.path.PathPlannerTrajectory;

// import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
// import edu.wpi.first.math.MathUtil;
// import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
// import edu.wpi.first.wpilibj.DriverStation;
// import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
// import edu.wpi.first.wpilibj2.command.Command;
// import edu.wpi.first.wpilibj2.command.InstantCommand;
// import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Swerve extends SubsystemBase {
    public SwerveDriveOdometry swerveOdometry;
    public SwerveModule[] mSwerveMods;
    public Pigeon2 gyro;
   // private SwerveDriveKinematics kinematics = Constants.Swerve.swerveKinematics;

    private Field2d field = new Field2d();

    public Swerve() {
        gyro = new Pigeon2(Constants.Swerve.pigeonID);
        gyro.getConfigurator().apply(new Pigeon2Configuration());
        gyro.setYaw(0); // changed this from 0 to 180 so BE CAREFUL OMGGGOMGOGMG
        // zeroGyro();

        mSwerveMods = new SwerveModule[] {
                new SwerveModule(0, Constants.Swerve.FrontLeft.constants),
                new SwerveModule(1, Constants.Swerve.FrontRight.constants),
                new SwerveModule(2, Constants.Swerve.RearLeft.constants),
                new SwerveModule(3, Constants.Swerve.RearRight.constants)
        };

        /*
         * By pausing init for a second before setting module offsets, we avoid a bug
         * with inverting motors.
         * See https://github.com/Team364/BaseFalconSwerve/issues/8 for more info.
         */
        // Timer.delay(1.0);
        // resetModulesToAbsolute();

        // int startingAprilTag = (int) SmartDashboard.getNumber("Starting April Tag ID", 1);
      // used to be "\\\\" here

        swerveOdometry = new SwerveDriveOdometry(Constants.Swerve.swerveKinematics, getGyroYaw(), getModulePositions());

        //  PathPlannerLogging.setLogActivePathCallback((poses) -> field.getObject("path").setPoses(poses));

        AutoBuilder.configureHolonomic( //TODO seems a bit weird, might wanna slash out the stuff if this dont work :(
      this::getPose, 
      this::resetPose, 
      this::getSpeeds, 
      this::driveRobotRelative, 
      Constants.Swerve.pathFollowerConfig,
      () -> {
          // Boolean supplier that controls when the path will be mirrored for the red alliance
          // This will flip the path being followed to the red side of the field.
          // THE ORIGIN WILL REMAIN ON THE BLUE SIDE

          var alliance = DriverStation.getAlliance();
          if (alliance.isPresent()) {
              return alliance.get() == DriverStation.Alliance.Red;
          }
          return false;
      },
      this
    );

    // Set up custom logging to add the current path to a field 2d widget
    PathPlannerLogging.setLogActivePathCallback((poses) -> field.getObject("path").setPoses(poses));


         SmartDashboard.putData("Field", field);



    }

    public void drive(Translation2d translation, double rotation, boolean fieldRelative, boolean isOpenLoop) {
        SwerveModuleState[] swerveModuleStates = 
            Constants.Swerve.swerveKinematics.toSwerveModuleStates(
                fieldRelative ? ChassisSpeeds.fromFieldRelativeSpeeds(
                        translation.getX(),
                        translation.getY(),
                        rotation,
                        getHeading()
                        )
                        : new ChassisSpeeds(
                                translation.getX(),
                                translation.getY(),
                                rotation)
                                );
        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, Constants.Swerve.maxSpeed);

        for (SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(swerveModuleStates[mod.moduleNumber], isOpenLoop);
        }
    }

    /* Used by SwerveControllerCommand in Auto */
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, Constants.Swerve.maxSpeed);

        for (SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(desiredStates[mod.moduleNumber], false);
        }
    }



    // public void resetOdometry(Pose2d pose) {
    //     swerveOdometry.resetPosition(getYaw(), getModulePositions(), pose);
    // }

    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[4];
        for (SwerveModule mod : mSwerveMods) {
            states[mod.moduleNumber] = mod.getState();
        }
        return states;
    }

    public SwerveModulePosition[] getModulePositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for (SwerveModule mod : mSwerveMods) {
            positions[mod.moduleNumber] = mod.getPosition();
        }
        return positions;
    }


    public Pose2d getPose() {
        return swerveOdometry.getPoseMeters();
    }

    public void setPose(Pose2d pose) {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), pose);
    }

    public void resetPose(Pose2d pose) {
        swerveOdometry.resetPosition(gyro.getRotation2d(), getModulePositions(), pose);
    }

    public Rotation2d getHeading() {
        return getPose().getRotation();
    }

    public void setHeading(Rotation2d heading) {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), heading));
    }

    public void zeroHeading() {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), new Rotation2d()));
    }

    public Rotation2d getGyroYaw() {
        return Rotation2d.fromDegrees(gyro.getYaw().getValue());
    }

    public ChassisSpeeds getSpeeds() {
        return Constants.Swerve.swerveKinematics.toChassisSpeeds(getModuleStates());
      }

      public void driveFieldRelative(ChassisSpeeds fieldRelativeSpeeds) {
        driveRobotRelative(ChassisSpeeds.fromFieldRelativeSpeeds(fieldRelativeSpeeds, getPose().getRotation()));
      }

      public void driveRobotRelative(ChassisSpeeds robotRelativeSpeeds) {
        ChassisSpeeds targetSpeeds = ChassisSpeeds.discretize(robotRelativeSpeeds, 0.02);

        SwerveModuleState[] targetStates = Constants.Swerve.swerveKinematics.toSwerveModuleStates(targetSpeeds);
        setModuleStates(targetStates);
      }
    
    // public void zeroGyro() {
    //     gyro.setYaw(0);
    // }

    // public void autozeroGyro() {
    //     gyro.setYaw(0); //for autozero, robot is flipped but still field-oriented
    // }

    // public Rotation2d getYaw() {
    //     return (Constants.Swerve.invertGyro) ? Rotation2d.fromDegrees(360 - gyro.getYaw())
    //             : Rotation2d.fromDegrees(gyro.getYaw());
    // }

    // public double getRoll(){
    //     return gyro.getRoll();
    // }


    public void resetModulesToAbsolute() {
        for (SwerveModule mod : mSwerveMods) {
            mod.resetToAbsolute();
        }
    }

    // public void stopDrive(){
    //     drive(new Translation2d(0, 0), 0, true, true);
    // }

    @Override
    public void periodic() {
        swerveOdometry.update(getGyroYaw(), getModulePositions());

        for (SwerveModule mod : mSwerveMods) {
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " CANcoder", mod.getCANcoder().getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Angle", mod.getPosition().angle.getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Velocity", mod.getState().speedMetersPerSecond);
        }
        // SmartDashboard.putNumber("Current Angle", getYaw().getDegrees());
        // SmartDashboard.putNumber("Current Roll", getRoll());

    
    }

    // public Command followTrajectoryCommand(PathPlannerTrajectory path7, boolean b) {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'followTrajectoryCommand'");
    // }



}




