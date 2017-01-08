#include "Configuration.h"

#include <cstring>

#define DEFAULT_RB_PORT "2016"
#define DEFAULT_RB_IP   "localhost"

#define DEFAULT_DC_PORT "2017"
#define DEFAULT_DC_IP   "localhost"

Configuration Config;

void ConfigurationInitialize()
{
    memset(&Config, 0, sizeof(Config));

    Config.DebugMode = DM_Normal;
    Config.ShowDebugFrameWindow = true;
    Config.AngleWidth = 60.0;

    strcpy(Config.RB_Port, DEFAULT_RB_PORT);
    strcpy(Config.RB_IPAddress, DEFAULT_RB_IP);

    Config.DC_Enable = false;
    strcpy(Config.DC_Port, DEFAULT_DC_PORT);
    strcpy(Config.DC_IPAddress, DEFAULT_DC_IP);
}

