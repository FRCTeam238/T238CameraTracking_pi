#include "ContourBuilder.h"

ContourBuilder::ContourBuilder()
    : mMode(CV_RETR_TREE), mMethod(CV_CHAIN_APPROX_SIMPLE)
{
    // do nothing
}

ContourList ContourBuilder::Process(cv::Mat mat)
{
    ContourList contours;
    std::vector<cv::Vec4i> notused; // contours;

    cv::findContours(mat, contours, notused, mMode, mMethod,
            cv::Point(0,0));

    return contours;
}

