
CXXFLAGS = -Wall -Werror -g `pkg-config --cflags opencv` --std=c++11
LIBS = `pkg-config --libs opencv` -lpthread

OBJS = main.o CameraMonitor.o TargetProcessor.o \
	   ReportingThread.o Configuration.o
EXE = a

.PHONY: all
all : $(EXE)

a : $(OBJS)
	$(CXX) -o $@ $^ $(LIBS)

main.o: main.cpp ReportingThread.h CameraMonitor.h TargetProcessor.h \
 Configuration.h
	$(CXX) -c $< $(CXXFLAGS) -o $@

ReportingThread.o: ReportingThread.cpp ReportingThread.h
	$(CXX) -c $< $(CXXFLAGS) -o $@

CameraMonitor.o: CameraMonitor.cpp Configuration.h CameraMonitor.h \
 TargetProcessor.h ReportingThread.h
	$(CXX) -c $< $(CXXFLAGS) -o $@

TargetProcessor.o: TargetProcessor.cpp TargetProcessor.h Configuration.h
	$(CXX) -c $< $(CXXFLAGS) -o $@

Configuration.o: Configuration.cpp Configuration.h
	$(CXX) -c $< $(CXXFLAGS) -o $@

.PHONY: deps
deps :
	$(CXX) -MM *.cpp

.PHONY: clean
clean:
	rm -f $(OBJS) $(EXE)
	rm -f *~ .*~


