
CXXFLAGS = -Wall -Werror -g `pkg-config --cflags opencv` --std=c++11
LIBS = `pkg-config --libs opencv` -lpthread

OBJS = main.o CameraMonitor.o TargetProcessor.o \
	   ReportingThread.o Configuration.o Logging.o \
	   HullBuilder.o ContourBuilder.o

EXE = t238-targeting

.PHONY: all
all : $(EXE)

t238-targeting : $(OBJS)
	$(CXX) -o $@ $^ $(LIBS)

CameraMonitor.o: CameraMonitor.cpp Configuration.h CameraMonitor.h \
 TargetProcessor.h ReportingThread.h Logging.h
CameraMonitorDebug.o: CameraMonitorDebug.cpp Configuration.h \
 CameraMonitor.h TargetProcessor.h
Configuration.o: Configuration.cpp Configuration.h
ContourBuilder.o: ContourBuilder.cpp ContourBuilder.h
HullBuilder.o: HullBuilder.cpp HullBuilder.h
Logging.o: Logging.cpp Logging.h
main.o: main.cpp ReportingThread.h CameraMonitor.h TargetProcessor.h \
 Configuration.h Logging.h
ReportingThread.o: ReportingThread.cpp ReportingThread.h Configuration.h \
 Logging.h
TargetProcessor.o: TargetProcessor.cpp TargetProcessor.h Configuration.h



.PHONY: deps
deps :
	$(CXX) $(CXXFLAGS) -MM main.cpp ReportingThread.cpp CameraMonitor.cpp \
        TargetProcessor.cpp Configuration.cpp

.PHONY: clean
clean:
	rm -f $(OBJS) $(EXE)
	rm -f *~ .*~


