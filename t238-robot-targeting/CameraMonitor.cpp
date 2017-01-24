#include <iostream>

#include "Configuration.h"
#include "CameraMonitor.h"
#include "ReportingThread.h"
#include "Logging.h"

using std::cout;
using std::endl;
using std::string;
using namespace cv;

CameraMonitor::CameraFailureException::CameraFailureException(const char *what)
     : std::runtime_error(what)
{
    // do nothing
}

CameraMonitor::CameraMonitor()
    : mTarget(CV_RGB2HSV_FULL, 7,
            Scalar(30, 50, 216), Scalar(60, 238, 238))
{
    // do nothing
}

CameraMonitor::~CameraMonitor()
{
    if (mCamera.isOpened())
    {
        mCamera.release();
    }
}

void CameraMonitor::InitializeSettings()
{
    memset(&mSettings, 0, sizeof(mSettings));
#if 1
    mSettings.LowerHue = 30;
    mSettings.UpperHue = 80;
    mSettings.LowerSaturation = 56;
    mSettings.UpperSaturation = 238;
    mSettings.LowerValue = 200;
    mSettings.UpperValue = 237;
    mSettings.BlurIndex = 7;
#else
    mSettings.LowerHue = 30;
    mSettings.UpperHue = 60;
    mSettings.LowerSaturation = 56;
    mSettings.UpperSaturation = 238;
    mSettings.LowerValue = 216;
    mSettings.UpperValue = 237;
    mSettings.BlurIndex = 7;
#endif

    mColorMode = CV_RGB2HSV_FULL;

    mTarget.Initialize(
            mColorMode,
            mSettings.LowerHue,
            mSettings.UpperHue,
            mSettings.LowerSaturation,
            mSettings.UpperSaturation,
            mSettings.LowerValue,
            mSettings.UpperValue,
            mSettings.BlurIndex);
}

bool CameraMonitor::InitializeCamera()
{
    bool retval = false;
    try
    {
        if (Config.SI_Enable)
        {
            string msg = "";
            msg += string("camera image override:filename=") +
                Config.SI_Filename;

            log_info(__FILE__, __LINE__, msg.c_str());
            retval = true;
        }
        else if (!mCamera.open(0))
        {
            log_error_msg(__FILE__, __LINE__,
                    "camera failed to open", -1, NULL);
        }
        else
        {
            retval = true;
        }
    }
    catch(cv::Exception &e)
    {
        const char *err = e.what();
        log_error_msg(__FILE__, __LINE__, "camera open failure", -1, err);
    }

    return retval;
}

bool CameraMonitor::ReadFrame(Mat &frame)
{
    bool retval = false;

    if (Config.SI_Enable)
    {
        frame = imread(Config.SI_Filename, CV_LOAD_IMAGE_COLOR);
        if (!frame.data)
        {
            string msg = std::string("Failed to load image:filename=") +
                Config.SI_Filename;
            log_error_msg(__FILE__, __LINE__, "imread", -1, msg.c_str());
            retval = false;
        }
        else
        {
            rectangle(frame,
                Point(100,200),
                Point(200,400),
                Scalar(40, 150, 240), CV_FILLED);
            retval = true;
        }
    }
    else
    {
        retval = mCamera.read(frame);
    }

    return retval;
}

#if 1

#if 1
Rectangle CameraMonitor::GetRangeOfContour(std::vector<cv::Point> contour) const
{
    Rectangle rect;

    rect.x1 = INT_MAX;
    rect.y1 = INT_MAX;
    rect.x2 = INT_MIN;
    rect.y2 = INT_MIN;

    for (size_t index = 0; index < contour.size(); index++)
    {
        cv::Point pt = contour[index];

        if (pt.x < rect.x1)
        {
            rect.x1 = pt.x;
        }

        if (pt.x > rect.x2)
        {
            rect.x2 = pt.x;
        }

        if (pt.y < rect.y1)
        {
            rect.y1 = pt.y;
        }

        if (pt.y > rect.y2)
        {
            rect.y2 = pt.y;
        }
    }

    return rect;
}

RectList CameraMonitor::FindRects(cv::Mat frame, const ContourList &hull) const
{
    RectList rectlist;

    for (ContourList::const_iterator it = hull.begin();
        it != hull.end(); it++)
    {
        Rectangle rect = GetRangeOfContour(*it);

        rectlist.push_back(rect);
    }

    return rectlist;
}

#else

Rectangle CameraMonitor::GetRangeOfContour(std::vector<cv::Point> contour) const
{
    Rectangle rect;

    rect.x1 = INT_MAX;
    rect.y1 = INT_MAX;
    rect.x2 = INT_MIN;
    rect.y2 = INT_MIN;

    for (size_t index = 0; index < contour.size(); index++)
    {
        cv::Point pt = contour[index];

        if (pt.x < rect.x1)
        {
            rect.x1 = pt.x;
        }

        if (pt.x > rect.x2)
        {
            rect.x2 = pt.x;
        }

        if (pt.y < rect.y1)
        {
            rect.y1 = pt.y;
        }

        if (pt.y > rect.y2)
        {
            rect.y2 = pt.y;
        }
    }

    return rect;
}

RectList CameraMonitor::FindRects(cv::Mat frame, ContourList hull) const
{
    RectList rectlist;

    for (size_t index = 0; index < hull.size(); index++)
    {
        Rectangle rect = GetRangeOfContour(hull[index]);

        rectlist.push_back(rect);
    }

    return rectlist;
}
#endif

cv::Mat CameraMonitor::DrawRectangles(cv::Mat frame, const RectList &rects) const
{
    static Scalar ss[5] =
    {
        Scalar(  0,   0, 128),
        Scalar(  0, 128,   0),
        Scalar(128,   0,   0),
        Scalar(128, 128,   0),
        Scalar(  0, 128, 128)
    };


    for (size_t index = 0; index < rects.size(); index++)
    {
        const Rectangle &rect = rects[index];
        Point tl =
        {
            rect.x1,
            rect.y1
        };

        Point br =
        {
            rect.x2,
            rect.y2
        };

        size_t colorIndex = index % (sizeof(ss) / sizeof(ss[0]));

        rectangle(frame,
            tl,
            br,
            ss[colorIndex]);
    }

    return frame;
}

Mat CameraMonitor::NextFrame()
{
    Mat frame;

    if (!ReadFrame(frame))
    {
        log_error_msg(__FILE__, __LINE__, "NextFrame",
            -1, "Failed to read frame");
    }
    else
    {
        mTarget.Process(frame);
        mHull = mTarget.GetHull();

        RectList rects = FindRects(frame, mHull);

        frame = DrawRectangles(frame, rects);
        imshow("edges", frame);
        waitKey(30);
    }

    return frame;
}

#else
Mat CameraMonitor::NextFrame()
{
    Mat frame;

    if (!ReadFrame(frame))
    {
        // no able to read a frame, break out now
        //TODO throw an exception?
    }
    else
    {
        mTarget.Process(frame);

        mHull = mTarget.GetHull();

        FrameCalculations calcs;

        if (Config.ShowDebugFrameWindow)
        {
            //DrawHull(frame, mTarget.GetContours(), calcs, false);
            DrawHull(frame, mHull, calcs, true);
            DrawHullRectangles(frame, mHull, calcs);
            imshow("edges", frame);
            waitKey(30);
        }
        else
        {
            CalculateHull(frame, mHull, calcs);
        }

        static int frameCount = 0;
        //TODO the frame count is used to determine the 
        //  fps - at some point convert this to be a rolling avg
        //  instead of a true average over the life of the program
        frameCount++;
        if (frameCount == 0)
        {
            frameCount = 1;
        }

        if (Config.DebugMode == DM_Normal)
        {
            if (calcs.isGoodData)
            {
                double angle_d = calcs.angle;
                double distance_d = calcs.distance;

                if (angle_d > 60)
                {
                    angle_d = 127.0;
                }
                else if (angle_d < -60)
                {
                    angle_d = 127.0;
                }

                int angle_i = (int)(angle_d * 2.0);
                int distance_i = (int)(distance_d);

                UpdateCameraData(angle_i, distance_i, frameCount);
            }
            else
            {
                UpdateCameraData(127, 127, frameCount);
            }
        }
        else if (Config.DebugMode == DM_RotatingNumbers)
        {
            // emit results to the transmit buffer here
            static int angle = 0;
            static int direction = 0;

            UpdateCameraData(angle, direction, frameCount);

            angle++;
            direction--;
        }
    }

    return frame;
}

void CameraMonitor::CalculateHull(Mat frame, const ContourList &contours,
        FrameCalculations &calcs)
{
    memset(&calcs, 0, sizeof(calcs));
    calcs.isGoodData = false;
    calcs.max_contour = -1;
    calcs.s_range_x1 = 127;
    calcs.s_range_y1 = 127;
    calcs.s_range_x2 = 127;
    calcs.s_range_y2 = 127;
    calcs.angle = 127;
    calcs.distance = 127;

    for (size_t index = 0; index < contours.size(); index++)
    {
        const std::vector<Point> &contour = contours[index];

        int range_x1, range_y1;
        int range_x2, range_y2;

        GetRangeOfContour(contour,
                range_x1, range_y1, range_x2, range_y2);

        int width = range_x2 - range_x1;
        int height = range_y2 - range_y1;

        if ((width > calcs.max_width) || (height > calcs.max_height))
        {
            calcs.max_contour = index;
            calcs.max_width = width;
            calcs.max_height = height;

            calcs.s_range_x1 = range_x1;
            calcs.s_range_y1 = range_y1;
            calcs.s_range_x2 = range_x2;
            calcs.s_range_y2 = range_y2;
        }
    }
    
    if (calcs.max_contour >= 0)
    {
        calcs.center_x = calcs.s_range_x1 + (calcs.max_width / 2);
        calcs.center_y = calcs.s_range_y1 + (calcs.max_height / 2);

        double dd = ((double)calcs.center_x / (double)frame.size().width);
        double angle = (Config.AngleWidth * dd) -
                (Config.AngleWidth / 2.0);
        calcs.angle = angle;
        //TODO calculate the height - distance to target
        calcs.isGoodData = true;
    }
    else
    {
        calcs.isGoodData = false;
        calcs.angle = 127;
        calcs.distance = 127;
    }
}

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
        DrawHullRectangles(frame, contours, calcs);
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
    }
    else
    {
        cout << "No contours" << endl;
    }
}

void CameraMonitor::GetRangeOfContour(const std::vector<Point> &contour,
    int &range_x1, int &range_y1, int &range_x2, int &range_y2)
{
    int min_x = 10000;
    int min_y = 10000;
    int max_x = 0;
    int max_y = 0;

    for (size_t index = 0; index < contour.size(); index++)
    {
        const Point &point = contour[index];

        if (point.x < min_x)
        {
            min_x = point.x;
        }

        if (point.x > max_x)
        {
            max_x = point.x;
        }

        if (point.y < min_y)
        {
            min_y = point.y;
        }

        if (point.y > max_y)
        {
            max_y = point.y;
        }
    }

    range_x1 = min_x;
    range_y1 = min_y;
    range_x2 = max_x;
    range_y2 = max_y;
}

#endif



