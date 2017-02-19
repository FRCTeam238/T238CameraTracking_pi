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

    public Target2017v2(double x, double y, double w, double h)
    {
        MatOfPoint points = new MatOfPoint(
            new Point(x,y),
            new Point(x + w, y + h)
        );
        SetHullData(points);
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
        double retval = 0.0;

        if (mBounds != null)
        {
            retval = mBounds.width;
        }

        return retval;
    }

    public double Height()
    {
        double retval = 0.0;

        if (mBounds != null)
        {
            retval = mBounds.height;
        }

        return retval;
    }

    public double Area() 
    {
        double retval = 0.0;

        if (mBounds != null)
        {
            retval = mBounds.area();
        }
        return retval;
    }

    public double Top()
    {
        double retval = 0.0;

        if (mBounds != null)
        {
            retval = mBounds.y;
        }

        return retval;
    }

    public double Bottom()
    {
        double retval = 0.0;
        
        if (mBounds != null)
        {
            retval = mBounds.y + mBounds.height;
        }

        return retval;
    }


    public double Left()
    {
        double retval = 0.0;

        if (mBounds != null)
        {
            retval = mBounds.x;
        }

        return retval;
    }

    public double Right()
    {
        double retval = 0.0;
        
        if (mBounds != null)
        {
            retval = mBounds.x + mBounds.width;
        }

        return retval;
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

    public boolean IsIntersecting(Target2017v2 other)
    {
        return false;
    }

    public String toString()
    {
        if (mBounds == null)
        {
            return "TT[null]";
        }
        else
        {
            return "TT[" + mBounds.toString() + "]";
        }
    }

}

