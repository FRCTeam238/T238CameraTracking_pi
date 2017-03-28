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
import org.opencv.core.Size;
import org.opencv.core.*;
import org.opencv.imgproc.*;

/* Detect targets within the provided hull data.
 *
 * Hulls are arrays of points describing a detected shape from
 * an image. This list is produced from the image pipeline in
 * GripPipeline.
 * 
 * The Process method will select targets that fit the 
 * expected look of a target. When some of these targets
 * are found the system tries to find a matching pair.
 * 
 * If only a single target is found then that is returned
 * and the caller is expected to make sense of seeing only
 * one. When a pair is found then the left most target area
 * is returned with the anticipation that the caller can
 * figure out the overall size of the target.
 *
 */
class TargetDetection2017v3_Gear 
{
    public final double TARGET_RATIO = 2.0 / 5.0;
    public final double TARGET_RATIO_BOUNDING = 0.40;
    public final double TARGET_RATIO_LOW =
            TARGET_RATIO - (TARGET_RATIO * TARGET_RATIO_BOUNDING);
    public final double TARGET_RATIO_HIGH =
            TARGET_RATIO + (TARGET_RATIO * TARGET_RATIO_BOUNDING);
    public final double TARGET_MIN_WIDTH = 15.0;
    public static final double TARGET_SIZE_APPROX_LOW = 0.70;
    public static final double TARGET_SIZE_APPROX_HIGH = 1.30;
    public static final int TARGET_DIRECTION_FROM_LEFT = 0;
    public static final int TARGET_DIRECTION_FROM_RIGHT = 1;
    public static final double TARGET_TO_CENTER_MULTIPLIER = 2.2;

    private List<Target2017v2> mKeep;
    private List<Target2017v2> mDiscard;

    public Target2017v2 Process(ArrayList<MatOfPoint> hulls)
    {
        Target2017v2 returnTarget = null;

        //TODO maybe make these linked lists - 
        //   linked list would be better insertion, but would also want
        //   to switch to using iterators instead of random access lookups
        //   in loops.
        //
        mKeep = new ArrayList<Target2017v2>();
        mDiscard = new ArrayList<Target2017v2>();
        
        //System.out.println(String.format("hulls.size=%d", hulls.size()));
        for (int index = 0; index < hulls.size(); index++)
        {
            Target2017v2 target = new Target2017v2(hulls.get(index));

            if (!IsBounded(
                    target.Width() / target.Height(),
                    TARGET_RATIO_LOW,
                    TARGET_RATIO_HIGH)|| 
                (target.Width() < TARGET_MIN_WIDTH))
            {
                mDiscard.add(target);
            }
            else
            {
                InsertLargestToSmallest(target, mKeep);
            }
        }

        if (mKeep.size() > 0)
        {
            returnTarget = ChooseTarget(mKeep);
            //if (returnTarget != null)
            //    System.out.println(returnTarget.GetBoundingRectangle());
        }

        return returnTarget;
    }

    /*
       This function tries to choose the left target of a target 
       pair.

       This function will choose the best target from the list of
       possible targets.

       This function assumes that all targets are possible candidates.
       The caller has sifted out obviously unreasonable ones and passed
       in targets ordered largest to smallest.

       The criteria to be chosen ...
            1. If there is only one target in the list, then pick it
            2. If there is more than one target the system assumes
               that the largest target is one of interest and attempts
               to find a second one that is approximately the same
               size.

               If a second target is found then the left most of the
               two is returned.

            Future enhancements:
              - The two selected targets should be approximately the
                same size AND approximately side-by-side AND
                within a certain distance of each other.

                The width of each target is 1/5 of the width of the
                space between the left-most and right-most edges.

     */
    private Target2017v2 ChooseTarget(List<Target2017v2> targets)
    {
        Target2017v2 returnTarget = null;

        if (targets.size() == 1)
        {
            //System.out.println("choose 0 by default");
            returnTarget = targets.get(0);
        }
        else if (targets.size() >= 2)
        {
            Target2017v2 target0 = targets.get(0);
            returnTarget = target0;
            int returnIndex = 0;

            /* find a target with approximately the same shape and
               size as the initial target on the list.

               TODO There are some difficiencies with the pattern here
               where it basically walks the list starting from the top
               and finds the last target that matches it. It may be
               questionable whether it really contributes anything 
               interesting - OTHER THAN that it will generally find the
               left-most target of the correct size.
             */
            for (int index = 1; index < targets.size(); index++)
            {
                Target2017v2 targetX = targets.get(index);

                //System.out.println(String.format("    %f <> %f = %f",
                //        targetX.Area(), target0.Area(),
                //        targetX.Area() / target0.Area()));
                if (IsBounded(
                           targetX.Area() / target0.Area(),
                            TARGET_SIZE_APPROX_LOW,
                            TARGET_SIZE_APPROX_HIGH))
                {
                    // if the compared target is more left than the current
                    // one then pick that to return
                    if (target0.Left() > targetX.Left())
                    {
                        returnIndex = index;
                        returnTarget = targetX;
                    }
                }
                else
                {
                    //System.out.println(String.format("tried n=%d", index));
                    break;
                }
            }

            //System.out.println(String.format("i=%d", returnIndex));
        }

        return returnTarget;
    }

    public List<Target2017v2> GetKept()
    {
        return mKeep;
    }

    public List<Target2017v2> GetDiscards()
    {
        return mDiscard;
    }

    /* Test whether a value is inside a limited range. */
    private boolean IsBounded(double value,
            double low_limit, double high_limit)
    {
        return (low_limit <= value) && (value <= high_limit);
    }

    /* Insert a new target onto the target list. Keep the target
       list in sort from largest to smallest.
     */
    private void InsertLargestToSmallest(Target2017v2 target,
            List<Target2017v2> targetList)
    {
        boolean hasAdded = false;

        for (int index = 0; index < targetList.size(); index++)
        {
            if (target.IsLargerThan(targetList.get(index)))
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

/*
 */
class TargetTracking2017v3_Gear
{
    private TargetDetection2017v3_Gear mTargetDetection = null;
    private Mat mInputImage = null;
    private CvSink mImageSink = null;
    private CvSource mImageSource = null;
    private GripPipeline mPipeline = null;
    private NetworkTable mNetworkTable = null;

    private double mResolutionWidth = 0;
    private double mResolutionHeight = 0;
    private double mWidthDegrees = 0;
    private double mHeightDegrees = 0;

    /* Collect parameters to prepare for processing images.
     */
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
                (int)resolutionWidth, (int)resolutionHeight, 30);

        // this defines an output stream where we can show the // calculated target parameters
        MjpegServer cvStream = new MjpegServer("Target Stream",
                TargetTracking.TARGET_STREAM_PORT);
        cvStream.setSource(mImageSource);

        // the image filter pipeline produced in Grip.
        mPipeline = new GripPipeline();

        // All Mats and Lists should be stored outside the
        // loop to avoid allocations as they are expensive to create
        mInputImage = new Mat();

        mTargetDetection = new TargetDetection2017v3_Gear();

        mResolutionWidth = resolutionWidth;
        mResolutionHeight = resolutionHeight;
        mWidthDegrees = widthDegrees;
        mHeightDegrees = heightDegrees;
    }

    /* This method will read a camera image frame, find targets,
       and produce the related angles on the driver station.

       After a camera frame is read the function process through
       the Grip pipeline, and then uses a TargetDetection object
       to detect targets. When one is detected the angles 
       relative to the screen center are pushed into the NetworkTable.
     */
    public void Process()
    {
        // Grab a frame. If it has a frame time of 0, there was an error.
        // Just skip and continue
        long frameTime = mImageSink.grabFrame(mInputImage);
        if (frameTime == 0)
        {
            mNetworkTable.putBoolean("Gear Camera Ready", false);
            System.out.println("ERROR: Gear camera error");
        }
        else
        {
            mNetworkTable.putBoolean("Gear Camera Ready", true);

            Mat outputImage = mInputImage;

            mPipeline.process(mInputImage);

            // this returns the target that was chosen to be our goto target
            Target2017v2 target = mTargetDetection.Process(
                    mPipeline.convexHullsOutput());

            double horizontalAngle = Double.MAX_VALUE;
            double verticalAngle = Double.MAX_VALUE;

            Point center = new Point();
            double distance = 100000.0;

            if ((target != null) && (target.GetBoundingRectangle() != null))
            {
                center = target.GetCenter();

                // shift the center to the left/right by 2.5 times the current
                // target width
                double original = center.x;

                /* We can approach the gear target from either the left
                   or the right. On the left we expect to have full view
                   of the target area on the left, but not on the right,
                   so we shift our center point to the right.

                   When we approach from the right we want to shift our
                   center point to the left.
                 */
                int targetDirection = GetTargetDirection();
                switch (targetDirection)
                {
                    default:
                        //TODO TBD Throw an exception instead?
                    case Configuration.TRACKING_GEAR_SIDE_left:
                        center.x += target.Width() *
                            TargetDetection2017v3_Gear.TARGET_TO_CENTER_MULTIPLIER;
                        break;

                    case Configuration.TRACKING_GEAR_SIDE_right:
                        center.x -= target.Width() *
                            TargetDetection2017v3_Gear.TARGET_TO_CENTER_MULTIPLIER;
                    break;
                }

                distance = CalculateDistance(target,
                        mResolutionHeight, mHeightDegrees);

                // calculate the angles relative to the center of the screen
                horizontalAngle = CalculateAngleOnScreen(
                        center.x, mResolutionWidth, mWidthDegrees);
                verticalAngle = CalculateAngleOnScreen(
                        center.y, mResolutionHeight, mHeightDegrees);
            }
            // else - leave the horizontal and vertical = 127.0

            mNetworkTable.putNumber("Gear Distance", distance);
            mNetworkTable.putNumber("Gear Horizontal", horizontalAngle);
            mNetworkTable.putNumber("Gear Vertical", verticalAngle);

            outputImage = DrawReticle(outputImage, new Scalar(80,80,255));

            outputImage = DrawTargets(outputImage,
                    mTargetDetection.GetDiscards(),
                    new Scalar(28, 28, 28));

            outputImage = DrawTargets(outputImage, 
                    mTargetDetection.GetKept(),
                    new Scalar(255, 0, 255));

            if ((target != null) && (target.GetBoundingRectangle() != null))
            {
                outputImage = DrawPoint(outputImage,
                        center, new Scalar(0, 0, 255));
            }

            mImageSource.putFrame(outputImage);
        }
    }

    /* Checks the current Gear Side setting ...

       Are we approaching from the left side of the gear peg or
       from the right.

       The value comes from the NetworkTable ("Gear Side") parameter
       but if that is not defined the value is provided by the 
       Configuration object.

     */
    private int GetTargetDirection()
    {
        int retval = Configuration.GetInstance().GetTrackingGearSide();

        try 
        {
            double side = mNetworkTable.getNumber("Gear Side");
            if (side <= 0.0)
            {
                retval = Configuration.TRACKING_GEAR_SIDE_left;
            }
            else
            {
                retval = Configuration.TRACKING_GEAR_SIDE_right;
            }
        }
        catch (Exception ex)
        {
            //System.out.println(ex);
            // couldn't read the network table value, do nothing, let the
            // configuration have control
        }

        return retval;
    }

    /* Draw the rectangle and center point of each hull provided.
     */
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
                    color, -1);
            }
        }

        return image;

    }

    /* Draw a small rectangle to represent a point.
     */
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
                -1);
        }

        return image;
    }

    /* Draw a reticle at the center of the provided image.
     */
    private Mat DrawReticle(Mat image, Scalar color)
    {
        Size size = image.size();
        Point pt1;
        Point pt2;

        pt1 = new Point( (size.width / 2) - 4, size.height / 2);
        pt2 = new Point( (size.width / 2) + 4, size.height / 2);
        Imgproc.line(image, pt1, pt2, color);

        pt1 = new Point( size.width / 2, (size.height / 2) - 4);
        pt2 = new Point( size.width / 2, (size.height / 2) + 4);
        Imgproc.line(image, pt1, pt2, color);

        return image;
    }

    /* Determine the angle of a point relative to the screen.

     */
    private double CalculateAngleOnScreen(double pixelPosition,
            double pixelRange, double angleRange)
    {
        return
            ((pixelPosition * angleRange ) / pixelRange) - (angleRange / 2.0);
    }

    private double CalculateDistance(Target2017v2 target,
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
}
