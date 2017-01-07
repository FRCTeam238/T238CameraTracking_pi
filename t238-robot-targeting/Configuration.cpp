#include "Configuration.h"

#include <cstring>

#define DEFAULT_ROBOT_PORT "2016"
#define DEFAULT_ROBOT_IP "localhost"

Configuration Config;

void ConfigurationInitialize()
{
    memset(&Config, 0, sizeof(Config));

    Config.DebugMode = DM_Normal;
    Config.ShowDebugFrameWindow = true;
    Config.AngleWidth = 60.0;
    Config.RobotPort = DEFAULT_ROBOT_PORT;
    Config.RobotIPAddress = DEFAULT_ROBOT_IP;
}

