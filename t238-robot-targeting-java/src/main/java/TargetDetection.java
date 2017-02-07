import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;

abstract class TargetDetection
{
    protected ArrayList<MatOfPoint> mFilteredHulls = null;
    protected ArrayList<MatOfPoint> mDiscardedHulls = null;

    public abstract Target Process(ArrayList<MatOfPoint> hulls);

    public ArrayList<MatOfPoint> GetFilteredHulls()
    {
        return mFilteredHulls;
    }

    public ArrayList<MatOfPoint> GetDiscardedHulls()
    {
        return mDiscardedHulls;
    }
}

class TargetDetection2016 extends TargetDetection
{
    public Target Process(ArrayList<MatOfPoint> hulls)
    {
        mFilteredHulls = hulls;

        Target target = new Target();

        for (int index = 0; index < hulls.size(); index++)
        {
            if (IsLargerThanTarget(hulls.get(index), target))
            {
                target.SetTarget(hulls.get(index));
            }
        }

        return target;
    }

    private boolean IsLargerThanTarget(MatOfPoint hull, Target target)
    {
        Target hullTarget = new Target();
        hullTarget.SetTarget(hull);

        return (hullTarget.Width() * hullTarget.Height()) >
                (target.Width() * target.Height());
    }

}

class TargetDetection2017_Gear extends TargetDetection
{
    private double RATIO_LOW = 2.0/5.0 - 0.2;
    private double RATIO_HIGH = 2.0/5.0 + 0.2;

    public Target Process(ArrayList<MatOfPoint> hulls)
    {
        Target target = new Target();
        mFilteredHulls = new ArrayList<MatOfPoint>();
        mDiscardedHulls = new ArrayList<MatOfPoint>();

        // order by box ratio of each potential target
        for (int index = 0; index < hulls.size(); index++)
        {
            String msg;

            MatOfPoint hull = hulls.get(index);

            target.SetTarget(hull);
            if ((target.Height() > 10.0) && (target.Width() > 10))
            {
                double ratio = (double)target.Width() / (double)target.Height();
                if ((ratio > RATIO_LOW) && (ratio < RATIO_HIGH))
                {
                    InsertLeftToRight(mFilteredHulls, target);

                    //msg = String.format("KEEP %f %dx%d", ratio,
                    //    target.Width(),
                    //    target.Height());
                    //;
                    //System.out.println(msg);
                }
                else
                {
                    mDiscardedHulls.add(hull);
                    /*
                    msg = String.format("DISC %f %f,%f",
                        ratio,
                        (double)target.Width(),
                        (double)target.Height());
                    */
                }
            }
        }

        if (mFilteredHulls.size() > 0)
        {
            DumpHulls(mFilteredHulls);
            target = AnalyzeHulls(mFilteredHulls);
            System.out.println();
        }

        return target;
    }

    private void DumpHulls(ArrayList<MatOfPoint> hulls)
    {
        for (int index = 0; index < hulls.size(); index++)
        {
            Target t = new Target(hulls.get(index));

            String msg = String.format("%d %s [c=%s]", index, 
                t.GetBounds(), t.Center());
            System.out.println(msg);
        }
        System.out.println("-----");
    }

    private void InsertLeftToRight(ArrayList<MatOfPoint> hulls, Target newHull)
    {
        boolean isAdded = false;

        for (int index = 0; index < hulls.size(); index++)
        {
            Target current = new Target(hulls.get(index));

            if ((current.GetData() != newHull.GetData()) && 
                    (current.Left() > newHull.Left()))
            {
                hulls.add(index, newHull.GetData());

                //String msg = String.format("ADD %s", newHull.GetBounds());
                //System.out.println(msg);

                isAdded = true;
                break;
            }
        }

        if (!isAdded)
        {
            hulls.add(newHull.GetData());

            //String msg = String.format("ADD %s", newHull.GetBounds());
            //System.out.println(msg);
        }
    }

    private Target AnalyzeHulls(ArrayList<MatOfPoint> hulls)
    {
        Target target = new Target();

        if (hulls.size() >= 2)
        {
            for (int index_current = 0;
                    index_current < (hulls.size() - 1);
                    index_current++)
            {
                Target target_current = new Target(hulls.get(index_current));
                Point current_center = target_current.Center();

                double right_edge_min = 
                    current_center.x + 
                    ((target_current.Width() / 2.0) * 8.25) - 
                    (target_current.Width() * 0.2);

                double right_edge_max =
                    current_center.x +
                    ((target_current.Width() / 2.0) * 8.25) + 
                    (target_current.Width() * 0.2);

                double horizontal_min =
                    current_center.y - (target_current.Height() * 0.5);

                double horizontal_max =
                    current_center.y + (target_current.Height() * 0.5);

                String msg;
                        
                msg = String.format("%d %f %f %f %f", 
                    index_current,
                    right_edge_min, right_edge_max,
                    horizontal_min, horizontal_max);
                System.out.println(msg);

                for (int index_test = index_current + 1;
                        index_test < hulls.size();
                        index_test++)
                {
                    Target target_test =
                            new Target(hulls.get(index_test));
                    Point target_test_center = target_test.Center();

                    msg = String.format("    %d %s",
                            index_test,
                            target_test_center);
                    System.out.println(msg);

                    if ((right_edge_min < target_test_center.x) &&
                        (right_edge_max > target_test_center.x) &&
                        (horizontal_min < target_test_center.y) &&
                        (horizontal_max > target_test_center.y))
                    {
                        Point p1 = current_center;
                        Point p2 = target_test_center;

                        target = new Target(
                            p1.x,
                            p1.y, 
                            p2.x - p1.x + 1,
                            p2.y - p1.y + 1);
                        break;
                    }
                }
            }
        }

        return target;
    }

    private Target AnalyzeHulls_x(ArrayList<MatOfPoint> hulls)
    {
        Target target = new Target();

        if (hulls.size() >= 2)
        {
            // we know ...
            //   the overall width is 10.25
            //   the distance between center points is 8.25
            for (int index_current = 0;
                    index_current < (hulls.size() - 1);
                    index_current++)
            {
                System.out.println(String.format("i1=%d", index_current));

                Target target_current = new Target(hulls.get(index_current));
                double width_min = ((double)target.Width() * 4.0) * 0.95;
                double width_max = ((double)target.Width() * 4.0) * 1.05;

                double height_min = 1;
                double height_max = 1;

                for (int index_test = index_current;
                    index_test < hulls.size();
                    index_test++)
                {
                    System.out.println(String.format("i2=%d", index_current));
                    Target target_test = new Target(hulls.get(index_test));
                    Point center_current = target_current.Center();
                    Point center_test = target_test.Center();

                    String msg = String.format("[%d %f %f] (%f  %f) => %f",
                            target.Width(), width_min, width_max,
                            center_current.x + width_min, 
                            center_current.x + width_max,
                            center_test.x);
                    System.out.println(msg);

                    double width_overall = center_test.x - center_current.x;
                    double height_overall =
                            Math.abs(center_test.y - center_current.y);

                    if ((width_min < width_overall) &&
                            (width_max > width_overall) &&
                            (height_min < height_overall) &&
                            (height_max > height_overall))
                    {
                        //Rect rect = new Rect(target_current.TopLeft(),
                        //        target_test.BottomRight());
                        Rect rect = target_test.GetBounds();
                        target = new Target(rect);
                        break;
                    }
                }
            }
        }
        // esle - no good target area, so just let it go

        return target;
    }

/*
    private Target AnalyzeHulls(ArrayList<MatOfPoint> hulls)
    {
        Target target = new Target();
        Point tl = new Point();
        Point br = new Point();

        // loop through all the hulls to find two rectangles that might
        // be within an area that could represent the two target stripes
        // for the gear target

        return target;
    }
*/
}

