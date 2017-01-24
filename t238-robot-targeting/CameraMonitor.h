#ifndef CameraMonitor_h
#define CameraMonitor_h

#include "TargetProcessor.h"

#include <stdexcept>
#include <opencv2/opencv.hpp>
#include <vector>

struct CameraSettings
{
    int LowerHue;
    int UpperHue;
    int LowerSaturation;
    int UpperSaturation;
    int LowerValue;
    int UpperValue;
    int BlurIndex;
};

struct Rectangle
{
    int x1, y1;
    int x2, y2;

    public:
        bool IsSmallerThan(const Rectangle &other) const
        {
            return
                Area() < other.Area();
        }

        int Area() const
        {
            return Width() * Height();
        }

        int Width() const
        {
            return x2 - x1;
        }

        int Height() const
        {
            return y2 - y1;
        }

        int CenterX() const
        {
            return (Width() / 2 ) + x1;
        }

        int CenterY() const
        {
            return (Height() / 2) + y1;
        }
};

typedef std::vector<Rectangle> RectList;

class CameraMonitor
{
    public:
        class CameraFailureException : public std::runtime_error
        {
            public:
                CameraFailureException(const char *what);
        };

    public:
        CameraMonitor();
        ~CameraMonitor();

        void InitializeSettings();
        bool  InitializeCamera();

        bool IsReady() const
        {
            return mCamera.isOpened();
        }

        cv::Mat NextFrame();

    private:
        struct FrameCalculations
        {
            int max_contour;
            int max_width;
            int max_height;

            // the calculated rectangle
            int s_range_x1;
            int s_range_y1;
            int s_range_x2;
            int s_range_y2;

            // the center point of the calculated rectangle
            int center_x;
            int center_y;

            double angle;
            double distance;

            bool isGoodData;
        };

        bool ReadFrame(cv::Mat &frame);

#if 0
        static void DrawAllHull(cv::Mat frame, const ContourList &contours,
            bool isHull);
        static void DrawHull(cv::Mat frame, const ContourList &contours,
                FrameCalculations &calcs, bool isHull);
        static void GetRangeOfContour(const std::vector<cv::Point> &contour,
                int &range_x1, int &range_y1, int &range_x2, int &range_y2);
        static void CalculateHull(cv::Mat frame, const ContourList &contours,
                FrameCalculations &calcs);
        static void DrawHullRectangles(cv::Mat frame,
                const ContourList &contours,
                FrameCalculations &calcs);
#endif

        /* revision 2 */
        Rectangle GetRangeOfContour(std::vector<cv::Point> contour) const;
        //C RectList FindRects(cv::Mat frame, ContourList hull) const;
        RectList FindRects(cv::Mat frame, const ContourList &hull) const;
        cv::Mat DrawRectangles(cv::Mat frame, const RectList &rects) const;

        CameraSettings mSettings;
        int mColorMode;
        cv::VideoCapture mCamera;
        TargetProcessor mTarget;
        ContourList mHull;

        // disable the copy constructor and assignment operator
        CameraMonitor(const CameraMonitor &);
        CameraMonitor &operator=(const CameraMonitor &);
};

#endif
