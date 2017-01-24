#ifndef TargetProcessor_h
#define TargetProcessor_h

#include "HullBuilder.h"
#include "ContourBuilder.h"

#include <opencv2/opencv.hpp>
#include <vector>
typedef std::vector<std::vector<cv::Point> > ContourList;

#include "caBlur.h"
#include "caConvertColor.h"
#include "caFilterColorThreshold.h"

class TargetProcessor
{
    public:
#if 1
        TargetProcessor(int colorMode, int blurIndex,
            cv::Scalar threshLow, cv::Scalar threshHigh);
#else
        TargetProcessor();
#endif
        ~TargetProcessor();

        void Initialize(int colorMode,
                int lowerHue, int upperHue,
                int lowerSaturation, int upperSaturation,
                int lowerValue, int upperValue,
                int blurIndex);

        void Process(cv::Mat frame);

        const ContourList &GetHull() const
        {
            return mHull;
        }

        const ContourList &GetContours() const
        {
            return mContours;
        }

    private:
        ContourList mContours;
        ContourList mHull;

        int mBlurIndex;

        ConvertColor convertColor;
        Blur blur;
        FilterColorThreshold threshold;
        ContourBuilder contourBuilder;
        HullBuilder hullBuilder;

        // disable the copy constructor and assignment operator
        TargetProcessor(const TargetProcessor &);
        TargetProcessor &operator=(const TargetProcessor &);
};

#endif

