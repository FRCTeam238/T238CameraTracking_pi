#include "FilterColorThreshold.h"

FilterColorThreshold::FilterColorThreshold(cv::Scalar low, cv::Scalar high) :
    mRangeLower(low), mRangeUpper(high)
{
    // do nothing
}

FilterColorThreshold::~FilterColorThreshold()
{
    // do nothing
}

cv::Mat FilterColorThreshold::Process(cv::Mat frame)
{
    cv::Mat retval;

    cv::inRange(frame, mRangeLower, mRangeUpper, retval);

    return retval;
}

