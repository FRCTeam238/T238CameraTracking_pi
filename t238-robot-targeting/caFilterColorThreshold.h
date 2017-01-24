#ifndef FilterColorThreshold_h
#define FilterColorThreshold_h

#include <opencv2/opencv.hpp>

class FilterColorThreshold
{
    public:
        FilterColorThreshold(cv::Scalar low, cv::Scalar high);
        ~FilterColorThreshold();

        cv::Mat Process(cv::Mat frame);

        void SetRange(cv::Scalar lower, cv::Scalar upper)
        {
            mRangeLower = lower;
            mRangeUpper = upper;
        }

    private:
        cv::Scalar mRangeLower;
        cv::Scalar mRangeUpper;

        // disabled
        FilterColorThreshold(FilterColorThreshold &);
        FilterColorThreshold &operator =(FilterColorThreshold &);
};

#endif

