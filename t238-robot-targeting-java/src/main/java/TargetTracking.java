import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.*;
import org.opencv.imgproc.*;


class TargetTracking
{
    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190
    public static final int CAMERA_STREAM_PORT = 1185;

    // This is the network port streaming the processed image that has
    // undergone filtering and other operations
    public static final int TARGET_STREAM_PORT = 1186;

    public static int CAMERA_RESOLUTION_WIDTH = 640;
    public static int CAMERA_RESOLUTION_HEIGHT = 480;

    public static final double CAMERA_WIDTH_DEGREES = 51.87;
    public static final double CAMERA_HEIGHT_DEGREES = 40.35;
    public static final double CAMERA_FOCAL_LENGTH = 658.0;

    // red side 
    // public static final double CAMERA_SHOOTER_ANGLE = 25.0; //35.0;

    // blue side    
    public static final double CAMERA_SHOOTER_ANGLE = 30.0;
    public static final double CAMERA_SHOOTER_HEIGHT = 18.5; // TBD in inches
    public static final double SHOOTER_TARGET_HEIGHT_CENTER = 83.0; // inches

    private CvSink mImageSink = null;
    private CvSource mImageSource = null;
    private GripPipeline mPipeline = null;
    private NetworkTable mNetworkTable = null;
    private Mat mInputImage = null;
    private Mat mHSVImage = null;
    private TargetDetection mTargetDetection = null;

    private double mResolutionWidth = 0;
    private double mResolutionHeight = 0;
    private double mWidthDegrees = 0;
    private double mHeightDegrees = 0;

    public TargetTracking()
    {
        // do nothing
    }

    public void Initialize(UsbCamera camera,
            double resolutionWidth, double resolutionHeight,
            double widthDegrees, double heightDegrees)
    {
        mNetworkTable = NetworkTable.getTable("SmartDashboard");

        // This creates a CvSink for us to use.
        // This grabs images from our selected camera, 
        // and will allow us to use those images in opencv
        mImageSink = new CvSink("Target Camera");
        mImageSink.setSource(camera);

        // This creates a CvSource to use.
        // This will take in a Mat image that has had OpenCV operations
        mImageSource = new CvSource("Target Image",
                VideoMode.PixelFormat.kMJPEG,
                (int)resolutionWidth, (int)resolutionHeight, 30); //TBD 30 ?? FPS?

        MjpegServer cvStream = new MjpegServer("Target Stream",
                TARGET_STREAM_PORT);
        cvStream.setSource(mImageSource);

        mPipeline = new GripPipeline();

        // All Mats and Lists should be stored outside the
        // loop to avoid allocations as they are expensive to create
        mInputImage = new Mat();
        mHSVImage = new Mat();

        mTargetDetection = new TargetDetection2017_Gear();
        //mTargetDetection = new TargetDetection2016();

        mResolutionWidth = resolutionWidth;
        mResolutionHeight = resolutionHeight;
        mWidthDegrees = widthDegrees;
        mHeightDegrees = heightDegrees;
    }

    public void Process()
    {
        // Grab a frame. If it has a frame time of 0, there was an error.
        // Just skip and continue
        long frameTime = mImageSink.grabFrame(mInputImage);
        if (frameTime != 0)
        {
            Mat outputImage = mInputImage;

            mPipeline.process(mInputImage);

            Target target = mTargetDetection.Process(
                    mPipeline.convexHullsOutput());

            // calculate the center point of the target

            if (target.HasData())
            {
                // find the centerpoint of the target
                /*
                Size targetSize = target.GetBounds().size();

                Point center = target.GetBounds().tl();
                center.x += (targetSize.width / 2); // - (mResolutionWidth / 2);
                center.y += (targetSize.height / 2); // - (mResolutionHeight / 2);
                */
                Point center = target.Center();

                // calculate the angles relative to the center of the screen
                double horizontalAngle = CalculateAngleOnScreen(
                        center.x, mResolutionWidth, mWidthDegrees);
                double verticalAngle = CalculateAngleOnScreen(
                        center.y, mResolutionHeight, mHeightDegrees);

                mNetworkTable.putNumber("Camera Horizontal", horizontalAngle);
                mNetworkTable.putNumber("Camera Vertical", verticalAngle);
            }
            else
            {
                Point center = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
                //System.out.println(center);
            }


            // put the image to the output camera stream for the dashboard
            outputImage = CalculateOutputImage(mInputImage,
                    mTargetDetection.GetDiscardedHulls(), 
                    new Scalar(128, 128, 128));

            outputImage = CalculateOutputImage(outputImage,
                    mTargetDetection.GetFilteredHulls(), 
                    new Scalar(255, 0, 0));

            outputImage = CalculateTargetImage(outputImage, target, 
                    new Scalar(0, 0,255,0));

            mImageSource.putFrame(outputImage);
        }
    }

    private Mat CalculateOutputImage(Mat inputImage,
            ArrayList<MatOfPoint> hulls, Scalar color)
    {
        Mat image = inputImage;
        if (hulls != null)
        {
            for (int index = 0; index < hulls.size(); index++)
            {
                MatOfPoint2f hull = new MatOfPoint2f(hulls.get(index).toArray());

                MatOfPoint h2 = new MatOfPoint(hull.toArray());

                Rect rect = Imgproc.boundingRect(h2);
                Imgproc.rectangle(image, 
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    color, 2);

                Rect center = new Rect(
                    rect.x + (rect.width / 2) - 2,
                    rect.y + (rect.height / 2) - 2,
                    4, 4);

                Imgproc.rectangle(image, 
                    new Point(center.x, center.y),
                    new Point(center.x + center.width, center.y + center.height),
                    color, 0);
            }
        }

        return image;

    }

    private Mat CalculateTargetImage(Mat inputImage, Target target, Scalar color)
    {
        Mat image = inputImage;

        if (target.HasData())
        {
            Point centerPoint = target.Center();

            Rect center = new Rect(
                    (int)centerPoint.x - 3,
                    (int)centerPoint.y - 3,
                    6,
                    6);

            Imgproc.rectangle(image, 
                new Point(center.x, center.y),
                new Point(center.x + center.width, center.y + center.height),
                color, -100);
        }

        return image;
    }

    private double CalculateAngleOnScreen(double pixelPosition,
            double pixelRange, double angleRange)
    {
        return
            ((pixelPosition * angleRange ) / pixelRange) - (angleRange / 2.0);
    }

}

