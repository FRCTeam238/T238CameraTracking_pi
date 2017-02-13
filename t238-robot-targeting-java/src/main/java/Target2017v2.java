import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Rect;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

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

