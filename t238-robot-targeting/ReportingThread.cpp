#include <iostream>
#include <cstdio>
#include <cstdint>
#include <cstring>
#include <cerrno>
#include <cstdlib>
#include <cmath>

#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>


#include "ReportingThread.h"
#include "Configuration.h"
#include "Logging.h"

using std::cout;
using std::endl;

/** 
 *
 * Reference: http://stackoverflow.com/questions/3756323/getting-the-current-time-in-milliseconds
 *
 */
long get_current_time_with_ms (void)
{
    long            ms; // Milliseconds
    time_t          s;  // Seconds
    struct timespec spec;

    clock_gettime(CLOCK_REALTIME, &spec);

    s  = spec.tv_sec;
    ms = round(spec.tv_nsec / 1.0e6); // Convert nanoseconds to milliseconds

    //printf("Current time: %"PRIdMAX".%03ld seconds since the Epoch\n",
    //       (intmax_t)s, ms);
    return (s * 1000) + ms;

}

/** The reporting thread is responsible for sending camera responses
 *  to the RoboRIO's UDP client.
 *
 *  The client will receive a data structure defined as CameraData. See
 *  the ReportingThread.h for its definition.
 *
 *  The destination of the messages is identified by the DEFAULT_ROBOT_IP and
 *  DEFAULT_ROBOT_PORT defines.
 *
 */

static pthread_t sReportingThread;
static CameraData sCameraData;
pthread_mutex_t cameraDataMutex = PTHREAD_MUTEX_INITIALIZER;
static long cameraDataTimeStart = 0;

void UpdateCameraData(int angle, int direction, long frameCount)
{
    pthread_mutex_lock(&cameraDataMutex);

    sCameraData.angle = (uint8_t)angle;
    sCameraData.direction = (uint8_t)direction;
    sCameraData.frameCount = (uint32_t)frameCount;

    pthread_mutex_unlock(&cameraDataMutex);
}


/** Send the camera data to the target device.
 *
 */
static bool SendCameraData(int sockfd)
{
    bool retval = false;

    pthread_mutex_lock(&cameraDataMutex);
    // sCameraData.frameCount++; -- don't set this here, let the camera
    // thread do it
    sCameraData.timestamp = get_current_time_with_ms();

    // prevent divide by zero
    if (sCameraData.frameCount == 0)
    {
        sCameraData.fps = 0;
    }
    else
    {
        sCameraData.fps =
            (sCameraData.timestamp - cameraDataTimeStart) /
                sCameraData.frameCount;
    }

    // this only returns time in seconds
    //time_t tt = time(NULL);
    //sCameraData.timestamp = tt;

    int result = send(sockfd, (void*)&sCameraData, sizeof(sCameraData), 0);
    pthread_mutex_unlock(&cameraDataMutex);
    if (result == sizeof(sCameraData))
    {
        retval = true;
        //cout << "Sent: " << result << endl;
    }
    else
    {
        log_error(__FILE__, __LINE__, "send", result, errno);
    }

    return retval;
}

/** Initialize the communications socket. 
 *
 * Reference: http://beej.us/guide/bgnet/
 */
static int OpenCommunicationsSocket()
{
    struct addrinfo hints, *res;
    int sockfd;

    // first, load up address structs with getaddrinfo():

    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_UNSPEC;  // use IPv4 or IPv6, whichever
    hints.ai_socktype = SOCK_DGRAM;

    int addrerr = getaddrinfo(
            Config.RB_IPAddress,
            Config.RB_Port,
            &hints,
            &res);
    if (addrerr != 0)
    {

        log_error_msg(__FILE__, __LINE__, "getaddrinfo",
                addrerr, gai_strerror(addrerr));
        sockfd = -1;
    }
    else
    {
        std::string msg = "";
        msg += std::string("getaddrinfo:connected:ip=")
            + Config.RB_IPAddress 
            + ",port=" 
            + Config.RB_Port;
        log_info(__FILE__, __LINE__, msg.c_str());
    }

    // make a socket:

    errno = 0;
    sockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
    if (sockfd < 0)
    {
        int error = errno;
        log_error(__FILE__, __LINE__, "socket", 0, error);
        sockfd = -1;
    }

    // connect it to the address and port we passed in to getaddrinfo():

    if (sockfd >= 0)
    {
        errno = 0;
        int rr = connect(sockfd, res->ai_addr, res->ai_addrlen);
        if (rr < 0)
        {
            close(sockfd);
            sockfd = -1;
            int error = errno;
            log_error(__FILE__, __LINE__, "connect", 0, error);
        }
    }

    return sockfd;
}

/** Main loop for the Camera to Client device communications thread.
 *
 *  The communications thread spends most of its time in this loop sending
 *  a message to the client device every CLIENT_INTERVAL_USEC.
 *
 */
static void RunCommunicationsLoop(int sockfd)
{
    bool done = false;

    cameraDataTimeStart = get_current_time_with_ms();

    for (;!done;)
    {
        usleep(CLIENT_INTERVAL_USEC);

        if (SendCameraData(sockfd))
        {
            // successfully sent data - maybe do something before
            // going back to sleep
        }
        else
        {
            done = true;
        }
    }
}

/** Entry point for the reporting thread.
 *  
 *  This function will initialize the communications socket to the
 *  client device and enter the main communications loop.
 *
 */
static void *ReportThreadEntry(void *)
{
    memset(&sCameraData, 0, sizeof(sCameraData));

    //TODO jump out when we receive a request to terminate
    for (bool done = false; !done; )
    {
        int sockfd = OpenCommunicationsSocket();
        if (sockfd == -1)
        {
            int error = errno;
            log_error(__FILE__, __LINE__, "socket", 0, error);
        }
        else
        {
            RunCommunicationsLoop(sockfd);
            close(sockfd);
            log_info(__FILE__, __LINE__, "reconnecting");
        }
    }

    return NULL;
}

void StartReportingThread()
{
    pthread_create(&sReportingThread, NULL, ReportThreadEntry, NULL);
}

