#ifndef HullBuilder_h
#define HullBuilder_h

#include <opencv2/opencv.hpp>
#include <vector>
typedef std::vector<std::vector<cv::Point> > ContourList;

class HullBuilder
{
    public:
        HullBuilder();
        ContourList Process(ContourList &contours);

    private:

        /* disabled */
        HullBuilder(const HullBuilder &);
        HullBuilder &operator =(HullBuilder &);
};

#endif

