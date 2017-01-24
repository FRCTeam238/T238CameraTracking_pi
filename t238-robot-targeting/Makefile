
CXXFLAGS = -Wall -Werror -g `pkg-config --cflags opencv` --std=c++11
LIBS = `pkg-config --libs opencv` -lpthread

OBJS = main.o CameraMonitor.o TargetProcessor.o \
    ReportingThread.o Configuration.o Logging.o \
    HullBuilder.o ContourBuilder.o \
    caBlur.o caConvertColor.o caFilterColorThreshold.o


EXE = t238-targeting

.PHONY: all
all : $(EXE)

t238-targeting : $(OBJS)
	$(CXX) -o $@ $^ $(LIBS)

main.o: main.cpp ReportingThread.h CameraMonitor.h TargetProcessor.h \
 HullBuilder.h ContourBuilder.h caBlur.h caConvertColor.h \
 caFilterColorThreshold.h Configuration.h Logging.h
ReportingThread.o: ReportingThread.cpp ReportingThread.h Configuration.h \
 Logging.h
CameraMonitor.o: CameraMonitor.cpp Configuration.h CameraMonitor.h \
 TargetProcessor.h HullBuilder.h ContourBuilder.h caBlur.h \
 caConvertColor.h caFilterColorThreshold.h ReportingThread.h Logging.h
TargetProcessor.o: TargetProcessor.cpp TargetProcessor.h HullBuilder.h \
 ContourBuilder.h caBlur.h caConvertColor.h caFilterColorThreshold.h \
 Configuration.h
Configuration.o: Configuration.cpp Configuration.h



.PHONY: deps
deps :
	$(CXX) $(CXXFLAGS) -MM main.cpp ReportingThread.cpp CameraMonitor.cpp \
        TargetProcessor.cpp Configuration.cpp

.PHONY: clean
clean:
	rm -f $(OBJS) $(EXE)
	rm -f *~ .*~


