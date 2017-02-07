import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import java.util.List;
import java.util.ListIterator;

class Target
{
    private MatOfPoint mTargetData = null;
    private Rect mBounds = null;

    public Target()
    {
        SetTarget(null);
    }

    public Target(MatOfPoint point)
    {
        SetTarget(point);
    }

    public Target(Rect rect)
    {
        SetTarget(new MatOfPoint(rect.tl(), rect.br()));
    }

    public Target(double x, double y, double w, double h)
    {
        SetTarget(new MatOfPoint(new Point(x,y), new Point(x + w, y + h)));
    }

    public void SetTarget(MatOfPoint targetData)
    {
        mTargetData = targetData;
        if (targetData != null)
        {
            mBounds = Imgproc.boundingRect(targetData);
        }
        else
        {
            mBounds = null;
        }
    }

    public MatOfPoint GetData() { return mTargetData; }
    public boolean HasData() { return GetData() != null; }

    public double Width() { return mBounds.width; }
    public double Height() { return mBounds.height; }
    public double Left() { return mBounds.x; }
    public double Right() { return mBounds.x + mBounds.width; }
    public double Top() { return mBounds.y; }
    public double Bottom() { return mBounds.y + mBounds.height; }

    public Point Center()
    {
        return new Point(
                Left() + (Width() / 2),
                Top() + (Height() / 2));
    }

    public Rect GetBounds()
    {
        return mBounds;
    }
}

