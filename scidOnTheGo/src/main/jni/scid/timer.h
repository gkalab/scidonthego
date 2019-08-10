//////////////////////////////////////////////////////////////////////
//
//  FILE:       timer.h
//              Millisecond resolution timer class
//
//  Part of:    Scid (Shane's Chess Information Database)
//  Version:    1.7
//
//  Notice:     Copyright (c) 1999  Shane Hudson.  All rights reserved.
//
//  Author:     Shane Hudson (sgh@users.sourceforge.net)
//
//////////////////////////////////////////////////////////////////////

#ifndef SCID_TIMER_H
#define SCID_TIMER_H

//////////////////////////////////////////////////////////////////////
// Timer::MilliSecs() returns the number of milliseconds since the
// timer was constructed or its Reset() method was last called.
// It uses gettimeofday() in Unix, or ftime() in Windows.


#    include <sys/time.h>


struct msecTimerT {
    long seconds;
    long milliseconds;
};


inline static void 
setTimer (msecTimerT *t)
{
    // Use gettimeofday() system call in Unix:
    struct timeval timeOfDay;
    gettimeofday (&timeOfDay, NULL);
    t->seconds = timeOfDay.tv_sec;
    t->milliseconds = timeOfDay.tv_usec / 1000;
}


class Timer {

  private:

    msecTimerT StartTime;

  public:
  
    Timer() { Reset (); }
    ~Timer() {}
    void Reset() { setTimer (&StartTime); }

    int MilliSecs (void) {
        msecTimerT nowTime;
        setTimer (&nowTime);
        return 1000 * (nowTime.seconds - StartTime.seconds) +
                    (nowTime.milliseconds - StartTime.milliseconds);
    }

    int CentiSecs (void) {
        msecTimerT nowTime;
        setTimer (&nowTime);
        return 100 * (nowTime.seconds - StartTime.seconds) +
                    (nowTime.milliseconds - StartTime.milliseconds) / 10;
    }

};

#endif

//////////////////////////////////////////////////////////////////////
//  EOF: timer.h
//////////////////////////////////////////////////////////////////////
