import java.util.ArrayList;
import java.util.List;

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

class TargetDetection2017v3_Shooter
{
    // When determining if a target is approximately half the height
    // of another the detected height of the smaller target has to
    // be within a bounded percentage of the expected height.
    private final double TARGET_MIN_BOUNDING = 0.65;
    private final double TARGET_HEIGHT_RATIO = 2.0 / 10.0;

    private List<Target2017v2> mKeep;
    private List<Target2017v2> mDiscard;

    public Target2017v2 Process(ArrayList<MatOfPoint> hulls)
    {
        Target2017v2 returnTarget = null;

        //TODO maybe make these linked lists - 
        //   linked list would be better insertion, but would also want
        //   to make the system use iterators instead of indexing
        //mKeep = new ArrayList<Target2017v2>();
        mKeep = new ArrayList<Target2017v2>();
        mDiscard = new ArrayList<Target2017v2>();

        // convert all of the MatOfPoint arrays to Targets
        // add them to the list so that they are largest to smallest
        for (int index = 0; index < hulls.size(); index++)
        {
            Target2017v2 target = new Target2017v2(hulls.get(index));
            //mDiscard.add(target);
            InsertTallestToShortest(target, mDiscard);
        }

        if (mDiscard.size() > 1)
        {
            // now pick the largest and see if there is one approximately
            // half the height

            // if this succeeds we expect a target that represents the area
            // of both targets together
            returnTarget = FindShooterTarget(mDiscard);
        }
        // else - no matching pair so return a null target

        return returnTarget;
    }

    /* Find two targets that are a matched set. A matched set is a target
       that appears relatively above one another, and the lower one is
       half the height of the upper.

       This method expects targets to be sorted tallest to shortest.

       This function only picks the first matched set.

     */
    private Target2017v2 FindShooterTarget(List<Target2017v2> targets)
    {
        Target2017v2 retval_target = null;

        for (int index = 0;
                (index < targets.size()) && (retval_target == null);
                index++)
        {
            Target2017v2 target_parent = targets.get(index);
            Rect target_parent_bounds =
                    target_parent.GetBoundingRectangle();
            //System.out.println(String.format("==%d %d", target_parent_bounds.width, target_parent_bounds.height));

            for (int index_child = 1;
                    index_child < targets.size(); index_child++)
            {
                Target2017v2 target_child = targets.get(index_child);

                if (!target_parent.IsIntersecting(target_child))
                {
                    Rect target_child_bounds =
                            target_child.GetBoundingRectangle();
                    //System.out.println(String.format("++%d %d", target_child_bounds.width, target_child_bounds.height));

                    Rect ref = new Rect();
                    ref.x = target_parent_bounds.x;
                    ref.y = target_parent_bounds.y;

                    ref.width = target_parent_bounds.width;
                    ref.height = 
                        (int)(target_parent_bounds.height * (10.0 / 4.0));

                    double expectedHeight = ref.height * TARGET_HEIGHT_RATIO;

                    double expectedHeight_Min = expectedHeight -
                            (expectedHeight * TARGET_MIN_BOUNDING);

                    double expectedHeight_Max = expectedHeight +
                            (expectedHeight * TARGET_MIN_BOUNDING);

                    //System.out.println(String.format("%s <> %s",
                    //        target_parent.toString(), target_child.toString()));
                    //System.out.println(String.format("  -- min=%f  max=%f", 
                    //        expectedHeight_Min, expectedHeight_Max));

                    if ((target_child.Height() < expectedHeight_Max) &&
                        (target_child.Height() > expectedHeight_Min))
                    {
                        // this is a match
                        retval_target = new Target2017v2(
                            ref.x,
                            ref.y,
                            ref.width,
                            (target_child_bounds.y - target_parent_bounds.y) +
                                    target_child_bounds.height
                        );
                        mKeep.add(retval_target);
                        break;
                    }
                }
            }
        }

        return retval_target;
    }

    public List<Target2017v2> GetKept()
    {
        return mKeep;
    }

    public List<Target2017v2> GetDiscards()
    {
        return mDiscard;
    }

    private boolean IsBounded(double value, double low_limit, double high_limit)
    {
        return (low_limit <= value) && (value <= high_limit);
    }

    private void InsertTallestToShortest(Target2017v2 target,
            List<Target2017v2> targetList)
    {
        boolean hasAdded = false;

        for (int index = 0; index < targetList.size(); index++)
        {
            if (target.Height() > targetList.get(index).Height())
            {
                targetList.add(index, target);
                hasAdded = true;
                break;
            }
        }

        if (!hasAdded)
        {
            targetList.add(target);
        }
    }
}

class TargetTracking2017v3_Shooter
{
    private TargetDetection2017v3_Shooter mTargetDetection = null;
    private Mat mInputImage = null;
    private CvSink mImageSink = null;
    private CvSource mImageSource = null;
    private GripPipeline mPipeline = null;
    private NetworkTable mNetworkTable = null;

    private double mResolutionWidth = 0;
    private double mResolutionHeight = 0;
    private double mWidthDegrees = 0;
    private double mHeightDegrees = 0;

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
                TargetTracking.TARGET_STREAM_PORT);
        cvStream.setSource(mImageSource);

        mPipeline = new GripPipeline();

        // All Mats and Lists should be stored outside the
        // loop to avoid allocations as they are expensive to create
        mInputImage = new Mat();

        mTargetDetection = new TargetDetection2017v3_Shooter();

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

            // this returns the target that was chosen to be our goto target
            Target2017v2 target = mTargetDetection.Process(
                    mPipeline.convexHullsOutput());

            double horizontalAngle = Double.MAX_VALUE;
            double verticalAngle = Double.MAX_VALUE;
            double distance = Double.MAX_VALUE;

            Point center = new Point();

            if ((target != null) && (target.GetBoundingRectangle() != null))
            {
                center = target.GetCenter();

                center.y = 240.0 - center.y;
                center.x = 320.0 - center.x;

                // calculate the angles relative to the center of the screen
                //horizontalAngle = CalculateAngleOnScreen(
                //        center.x, mResolutionWidth, mWidthDegrees);
                //verticalAngle = CalculateAngleOnScreen(
                //        center.y, mResolutionHeight, mHeightDegrees);
                //System.out.println(
                //        String.format("horz angle=%f", horizontalAngle));
                //System.out.println(
                //        String.format("vert angle=%f", verticalAngle));

                //distance = CalculateDistance_x(target,
                //        mResolutionHeight, mHeightDegrees);
                //
                double horizontalAngle_radians = CalculateTargetAngle(center.x);
                double verticalAngle_radians = CalculateTargetAngle(center.y);
                distance = CalculateDistance(verticalAngle_radians);

                horizontalAngle = Math.toDegrees(horizontalAngle_radians);
                verticalAngle = Math.toDegrees(verticalAngle_radians);

                double at = Math.atan(center.y / TargetTracking.CAMERA_FOCAL_LENGTH);
                System.out.println(String.format(
                            "y=%f CFL=%f atan=%f",
                            center.y,
                            TargetTracking.CAMERA_FOCAL_LENGTH,
                            at));


                System.out.println(String.format("horz angle=%f", horizontalAngle));
                System.out.println(String.format("vert angle=%f", verticalAngle));
                System.out.println(String.format("distance=%f", distance));
                System.out.println();
            }
            // else - leave the horizontal and vertical = 127.0

            mNetworkTable.putNumber("Shooter Distance", distance);
            mNetworkTable.putNumber("Shooter Horizontal", horizontalAngle);
            mNetworkTable.putNumber("Shooter Vertical", verticalAngle);

            outputImage = DrawPoint(outputImage,
                    new Point(320, 240), new Scalar(255,255, 0));

            outputImage = DrawTargets(outputImage,
                    mTargetDetection.GetDiscards(),
                    new Scalar(28, 28, 28));

            outputImage = DrawTargets(outputImage, 
                    mTargetDetection.GetKept(),
                    new Scalar(255, 0, 255));


            if ((target != null) && (target.GetBoundingRectangle() != null))
            {
                outputImage = DrawPoint(outputImage,
                        center, new Scalar(0, 255, 255));
            }

            // putText(outputImage, "Shooter", new Point(5,5), Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(0,255,255));

            mImageSource.putFrame(outputImage);
        }
    }

    private Mat DrawTargets(Mat inputImage,
            List<Target2017v2> hulls, Scalar color)
    {
        Mat image = inputImage;
        if (hulls != null)
        {
            for (int index = 0; index < hulls.size(); index++)
            {
                Target2017v2 current = hulls.get(index);

                MatOfPoint2f hull = 
                        new MatOfPoint2f(current.GetHullData().toArray());
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

    private Mat DrawPoint(Mat image, Point pt, Scalar color)
    {
        if ((pt.x != -1) && (pt.y != -1))
        {
            Point pt_tl = new Point(pt.x - 2, pt.y - 2);
            Point pt_br = new Point(pt.x + 2, pt.y + 2);
            
            Imgproc.rectangle(image, 
                pt_tl, 
                pt_br,
                color,
                2);
        }

        return image;
    }

    private double CalculateAngleOnScreen(double pixelPosition,
            double pixelRange, double angleRange)
    {
        return
            ((pixelPosition * angleRange ) / pixelRange) - (angleRange / 2.0);
    }

    private double CalculateDistance_x(Target2017v2 target,
                double resolutionPixels, double resolutionDegrees)
    {
        double angle_top;
        double angle_bottom;
        if (false)
        {
            angle_top = (target.Top() * resolutionDegrees) / resolutionPixels;
            angle_bottom = (target.Bottom() * resolutionDegrees) / resolutionPixels;

            angle_top = resolutionDegrees - angle_top;
            angle_bottom = resolutionDegrees - angle_bottom;
        }
        else
        {
            angle_top = CalculateAngleOnScreen(target.Top(),
                    resolutionPixels, resolutionDegrees);
            angle_bottom = CalculateAngleOnScreen(target.Bottom(),
                    resolutionPixels, resolutionDegrees);
        }

        System.out.println(String.format("Angles (deg)= %f %f", 
                angle_top, angle_bottom));

        angle_top = Math.toRadians(angle_top);
        angle_bottom = Math.toRadians(angle_bottom);

        double theta = angle_top - angle_bottom;

        //double distance = 5.0 / Math.tan( (8.0 / 18.75) * theta );
        double distance = 10.25 / Math.tan( 1.864 * theta );

        System.out.println(String.format("y_top=%f y_bottom=%f",
                target.Top(), target.Bottom()));
        System.out.println(String.format("theta = %f - %f = %f",
                angle_top, angle_bottom, theta));
        System.out.println(String.format("tan(theta) = %f", Math.tan(theta)));

        System.out.println(String.format("distance=%f", distance));
        System.out.println(String.format("distance/2=%f", distance / 2.0));
        System.out.println();

        return distance;
    }
    private double CalculateTargetAngle(double pixelPosition)
    {
        double thetaV = 
            Math.atan(pixelPosition / TargetTracking.CAMERA_FOCAL_LENGTH);
        return thetaV;
    }

    private double CalculateDistance(double thetaV_radians)
    {
        double distance =
            (TargetTracking.SHOOTER_TARGET_HEIGHT_CENTER - 
                TargetTracking.CAMERA_SHOOTER_HEIGHT) /
            Math.tan(Math.toRadians(TargetTracking.CAMERA_SHOOTER_ANGLE)
                    + thetaV_radians);
        return distance;
    }
}
