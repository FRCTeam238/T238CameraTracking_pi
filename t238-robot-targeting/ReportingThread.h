#ifndef ReportingThread_h
#define ReportingThread_h

struct CameraData
{
    uint8_t  angle_h; // horizontal angle from center of camera
    uint8_t  angle_v; // vertical angle from center of camera
    uint8_t  reserved1;
    uint8_t  fps;

    uint32_t timestamp;

    uint32_t frameCount;

    uint16_t pos_x;
    uint16_t pos_y;
};

/** The IP and port number of the target client 
 *
 */
#define DEFAULT_ROBOT_IP   "localhost"
//#define CLIENT_IP   "192.168.1.33"
//#define CLIENT_IP   "192.168.1.3"
#define DEFAULT_ROBOT_PORT "2016"

/** The timing in microseconds of the iteration loop. The
 *  system will attempt to send a CameraData packet once
 *  for each loop.
 */
#define CLIENT_INTERVAL_USEC 1000 * 100

/* External declarations */
extern void StartReportingThread();
extern void UpdateCameraData(int angle_h, int angle_v, long frameCount);

#endif

