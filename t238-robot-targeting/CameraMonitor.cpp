#include <iostream>

#include "Configuration.h"
#include "CameraMonitor.h"
#include "ReportingThread.h"

using std::cout;
using std::endl;

CameraMonitor::CameraMonitor()
    : mTarget(CV_RGB2HSV_FULL, 7,
            cv::Scalar(30, 50, 216), cv::Scalar(60, 238, 238))
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
    mSettings.LowerHue = 30;
    mSettings.UpperHue = 60;
    mSettings.LowerSaturation = 56;
    mSettings.UpperSaturation = 238;
    mSettings.LowerValue = 216;
    mSettings.UpperValue = 237;
    mSettings.BlurIndex = 7;

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

void CameraMonitor::InitializeCamera()
{
    try
    {
        if (!mCamera.open(0))
        {
            cout << "mCamera.open failure: Failed to open the camera" << endl;
            exit(EXIT_FAILURE);
        }
    }
    catch(cv::Exception &e)
    {
        const char *err = e.what();
        cout << "error mCamera.open failed: " << err << endl;
        exit(EXIT_FAILURE);
    }
}

cv::Mat CameraMonitor::NextFrame()
{
    cv::Mat frame;

    if (!mCamera.read(frame))
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
            DrawHull(frame, mHull, calcs);
            imshow("edges", frame);
            cv::waitKey(30);
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
            double angle_d = calcs.angle;
            double distance_d = calcs.distance;

            if (angle_d > 60)
            {
                angle_d = 60.0;
            }
            else if (angle_d < -60)
            {
                angle_d = -60.0;
            }

            int angle_i = (int)(angle_d * 2.0);
            int distance_i = (int)(distance_d);

            UpdateCameraData(angle_i, distance_i, frameCount);
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


void CameraMonitor::CalculateHull(cv::Mat frame, ContourList &contours,
        FrameCalculations &calcs)
{
    memset(&calcs, 0, sizeof(calcs));
    calcs.max_contour = -1;

    for (size_t index = 0; index < contours.size(); index++)
    {
        std::vector<cv::Point> &contour = contours[index];

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

        //int screen_center_x = frame.rows / 2;
        //int screen_center_y = frame.cols / 2;

        //cout << "screen_center_x=" << screen_center_x << endl;
        //cout << "screen_center_y=" << screen_center_y << endl;

        //cout << "center_x=" << calcs.center_x << endl;
        //cout << "center_y=" << calcs.center_y << endl;

        double dd = ((double)calcs.center_x / (double)frame.size().width);
        double angle = (Config.AngleWidth * dd) -
                (Config.AngleWidth / 2.0);
        calcs.angle = angle;

#if 0
        cout << "ap="
            << calcs.center_x
            << " / "
            << frame.size().width
            << " = "
            << dd
            << endl;

        cout << "AngleWidth=" << Config.AngleWidth << endl;
        cout << "angle=" << angle << endl;
#endif

        //TODO calculate the height - distance to target
    }
}

void CameraMonitor::DrawHull(cv::Mat frame, ContourList &contours,
        FrameCalculations &calcs)
{
    CalculateHull(frame, contours, calcs);

    if (calcs.max_contour >= 0)
    {
        rectangle(frame,
            cv::Point(calcs.s_range_x1, calcs.s_range_y1),
            cv::Point(calcs.s_range_x2, calcs.s_range_y2),
            cv::Scalar(0,0,255));
        //cout << s_range_x1 << " " << s_range_y1 << endl;
        //cout << s_range_x2 << " " << s_range_y2 << endl;
    }
}

void CameraMonitor::GetRangeOfContour(std::vector<cv::Point> &contour,
    int &range_x1, int &range_y1, int &range_x2, int &range_y2)
{
    int min_x = 10000;
    int min_y = 10000;
    int max_x = 0;
    int max_y = 0;

    for (size_t index = 0; index < contour.size(); index++)
    {
        cv::Point &point = contour[index];

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

        //cout << point << endl;
    }

    //cout << "======" << endl;

    range_x1 = min_x;
    range_y1 = min_y;
    range_x2 = max_x;
    range_y2 = max_y;
}

