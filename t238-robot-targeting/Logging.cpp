#include "Logging.h"

#include <cstring>
#include <iostream>
using std::cout;
using std::endl;

void log_info(const char *filename, int lineno, const char *msg)
{
    cout << "Info :" << filename << ":" << lineno 
        << ":" << msg
        << endl;
}

void log_error(const char *filename, int lineno, const char *msg,
        int result, int error)
{
    cout << "Error:"
        << filename << ":" << lineno
        << ":errno=" << std::strerror(error)
        << ",result=" << result
        << endl;
}

void log_error_msg(const char *filename, int lineno, const char *msg,
        int error, const char *error_msg)
{
    cout << "Error:"
        << filename << ":" << lineno
        << ":errno=" << error << ' ' << error_msg
        << endl;
}

