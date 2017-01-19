#ifndef ContourBuilder_h
#define ContourBuilder_h

#include <opencv2/opencv.hpp>
#include <vector>
typedef std::vector<std::vector<cv::Point> > ContourList;

class ContourBuilder
{
    public:
        ContourBuilder();
        ContourList Process(cv::Mat mat);

    private:
        int mMode;
        int mMethod;

        /* disabled */
        ContourBuilder(const ContourBuilder &);
        ContourBuilder &operator =(ContourBuilder &);
};

#endif
