#ifndef Logging_h
#define Logging_h

void log_info(const char *filename, int lineno, const char *msg);
void log_error(const char *filename, int lineno, const char *msg,
        int result, int error);
void log_error_msg(const char *filename, int lineno, const char *msg,
        int error, const char *error_msg);

#endif

