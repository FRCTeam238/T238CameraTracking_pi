#include "HullBuilder.h"

HullBuilder::HullBuilder()
{
    // do nothing
}

ContourList HullBuilder::Process(ContourList &contours)
{
    ContourList hull(contours.size());

    for (size_t index = 0; index < contours.size(); index++)
    {
        convexHull(cv::Mat(contours[index]), hull[index], false);
    }

    return hull;
}

