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

/*
class Target2017v2
{
    private MatOfPoint mHull = null;
    private Rect mBounds = null;

    public Target2017v2()
    {
        SetHullData(null);
    }

    public Target2017v2(MatOfPoint hull)
    {
        SetHullData(hull);
    }

    public void SetHullData(MatOfPoint hull)
    {
        if (hull == null)
        {
            mHull = null;
            mBounds = null;
        }
        else
        {
            mHull = hull;
            mBounds = Imgproc.boundingRect(mHull);
        }
    }

    public MatOfPoint GetHullData()
    {
        return mHull;
    }

    public Point GetCenter()
    {
        Rect rect = GetBoundingRectangle();

        if ((rect.width > 0.0) && (rect.height > 0.0))
        {
            return new Point(
                rect.x + (rect.width / 2.0),
                rect.y + (rect.height / 2.0));
        }
        else
        {
            return new Point(-1, -1);
        }
    }

    public Rect GetBoundingRectangle()
    {
        return mBounds;
    }

    public double Width()
    {
        if (mBounds == null)
        {
            return 0.0;
        }
        else
        {
            return mBounds.width;
        }
    }

    public double Height()
    {
        if (mBounds == null)
        {
            return 0.0;
        }
        else
        {
            return mBounds.height;
        }
    }

    public boolean IsLargerThan(Target2017v2 other)
    {
        if ((Width() * Height()) > (other.Width() * other.Height()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
*/


class TargetDetection2017v2_Shooter
{
    private final double TARGET_RATIO = 2.0 / 5.0;
    private final double TARGET_RATIO_BOUNDING = 0.50;
    private final double TARGET_RATIO_LOW =
            TARGET_RATIO - (TARGET_RATIO * TARGET_RATIO_BOUNDING);
    private final double TARGET_RATIO_HIGH =
            TARGET_RATIO + (TARGET_RATIO * TARGET_RATIO_BOUNDING);
    private final double TARGET_MIN_WIDTH = 15.0;

    private List<Target2017v2> mKeep;
    private List<Target2017v2> mDiscard;

    public Target2017v2 Process(ArrayList<MatOfPoint> hulls)
    {
        Target2017v2 returnTarget = null;

        //TODO maybe make these linked lists - 
        //   linked list would be better insertion, but would also want
        //   to make the system use iterators instead of indexing
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

                returnTarget = target;
            }
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

    private boolean IsBounded(double value, double low_limit, double high_limit)
    {
        return (low_limit <= value) && (value <= high_limit);
    }

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

class TargetTracking2017v2_Shooter
{
    private TargetDetection2017v2_Gear mTargetDetection = null;
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
        //mHSVImage = new Mat();

        mTargetDetection = new TargetDetection2017v2_Gear();

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

            double horizontalAngle = 127.0;
            double verticalAngle = 127.0;

            Point center = new Point();

            if ((target != null) && (target.GetBoundingRectangle() != null))
            {
                System.out.println(String.format("w=%f", target.Width()));
                center = target.GetCenter();

                //TODO NOTE This is specific to the expected direction
                //               of the target
                // shift the center to the right by 2.5 times the current
                // target width
                double original = center.x;
                center.x += target.Width() * 2;

                //System.out.println(String.format("%f %f %f",
                //        original, center.x, target.Width()));

                // calculate the angles relative to the center of the screen
                horizontalAngle = CalculateAngleOnScreen(
                        center.x, mResolutionWidth, mWidthDegrees);
                verticalAngle = CalculateAngleOnScreen(
                        center.y, mResolutionHeight, mHeightDegrees);
            }
            // else - leave the horizontal and vertical = 127.0

            mNetworkTable.putNumber("Gear Horizontal", horizontalAngle);
            mNetworkTable.putNumber("Gear Vertical", verticalAngle);

            outputImage = DrawTargets(outputImage,
                    mTargetDetection.GetDiscards(),
                    new Scalar(28, 28, 28));

            outputImage = DrawTargets(outputImage, 
                    mTargetDetection.GetKept(),
                    new Scalar(255, 255, 0));

            if ((target != null) && (target.GetBoundingRectangle() != null))
            {
                outputImage = DrawPoint(outputImage,
                        center, new Scalar(0, 0, 255));

            }

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

}
