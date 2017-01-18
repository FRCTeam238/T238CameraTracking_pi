#include "Configuration.h"
#include "CameraMonitor.h"

using std::cout;
using std::endl;
using namespace cv;

static Scalar ss[4] =
{
    Scalar(0, 0, 128),
    Scalar(128, 0, 0),
    Scalar(25, 25, 25),
    Scalar(128, 128, 0)
};

using std::vector;

void CameraMonitor::DrawAllHull(Mat frame, const ContourList &contours,
        bool isHull)
{
    for (size_t contourIndex = 0; contourIndex < contours.size(); 
        contourIndex++)
    {
        int colorIndex = contourIndex % 4;
        vector<cv::Point> contour = contours[contourIndex];

        for (size_t pointInContour = 0; pointInContour < contour.size();
            pointInContour++)
        {
            cv::Point point = contour[pointInContour];

            if (isHull)
            {
                rectangle(frame,
                    point - Point(1,2),
                    point + Point(1,2),
                    ss[colorIndex],
                    CV_FILLED
                );
            }
            else
            {
                rectangle(frame,
                    point - Point(2,1),
                    point + Point(2,1),
                    ss[colorIndex],
                    CV_FILLED
                );
            }
        }
    }

#if 1
    static size_t last_hull_count = 50000000;
    if (last_hull_count != contours.size())
    {
        last_hull_count = contours.size();
        cout << "hull count=" << contours.size() << endl;
    }
#endif
}

void CameraMonitor::DrawHull(cv::Mat frame, const ContourList &contours,
        FrameCalculations &calcs, bool isHull)
{
    if (Config.DB_DrawAllHull)
    {
        DrawAllHull(frame, contours, isHull);
    }
    else
    {
#if 1
        DrawHullRectangles(frame, contours, calcs);
#else
        CalculateHull(frame, contours, calcs);

        if (calcs.max_contour >= 0)
        {
            rectangle(frame,
                Point(calcs.s_range_x1, calcs.s_range_y1),
                Point(calcs.s_range_x2, calcs.s_range_y2),
                Scalar(0,255,255));
            //cout << calcs.s_range_x1 << " " << calcs.s_range_y1 << endl;
            //cout << calcs.s_range_x2 << " " << calcs.s_range_y2 << endl;
        }
        else
        {
            cout << "No contours" << endl;
        }
#endif
    }
}

void CameraMonitor::DrawHullRectangles(Mat frame,
    const ContourList &contours, FrameCalculations &calcs)
{
    CalculateHull(frame, contours, calcs);

    if (calcs.max_contour >= 0)
    {
        rectangle(frame,
            Point(calcs.s_range_x1, calcs.s_range_y1),
            Point(calcs.s_range_x2, calcs.s_range_y2),
            Scalar(0, 128, 255), 2);
        //cout << calcs.s_range_x1 << " " << calcs.s_range_y1 << endl;
        //cout << calcs.s_range_x2 << " " << calcs.s_range_y2 << endl;
    }
    else
    {
        cout << "No contours" << endl;
    }
}


