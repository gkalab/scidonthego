//////////////////////////////////////////////////////////////////////
//
//  FILE:       mfile.h
//              MFile class
//
//  Part of:    Scid (Shane's Chess Information Database)
//  Version:    2.0
//
//  Notice:     Copyright (c) 2000  Shane Hudson.  All rights reserved.
//
//  Author:     Shane Hudson (sgh@users.sourceforge.net)
//
//////////////////////////////////////////////////////////////////////


// An MFile is a file that can be a regular file, or memory-only with
// no actual file on any device.

#ifndef SCID_MFILE_H
#define SCID_MFILE_H

#include "common.h"
#include "dstring.h"
#include "error.h"

enum mfileT {
    MFILE_REGULAR = 0, MFILE_MEMORY
};

class MFile
{
  private:
    FILE *      Handle;         // For regular files.
    fileModeT   FileMode;
    mfileT      Type;
    char *      FileName;

    // The next few fields are used for in-memory files.
    uint        Capacity;
    uint        Location;
    byte *      Data;
    byte *      CurrentPtr;

    char *      FileBuffer;  // Only for files with unusual buffer size.

    void  Extend();

  public:
    MFile() { Init(); }
    ~MFile() {
        if (Handle != NULL) { Close(); }
        if (Data != NULL) { delete[] Data; }
        if (FileBuffer != NULL) { delete[] FileBuffer; }
        if (FileName != NULL) { delete[] FileName; }
    }

    void Init();

    fileModeT Mode() { return FileMode; }

    errorT Create (const char * name, fileModeT fmode);
    errorT Open  (const char * name, fileModeT fmode);
    void   CreateMemory () { Close(); Init(); }
    errorT Close ();

    void   SetBufferSize (uint bufsize);

    uint   Size ();
    uint   Tell () { return Location; }
    errorT Seek (uint position);
    errorT Flush ();
    inline bool EndOfFile();

    errorT        WriteNBytes (const char * str, uint length);
    errorT        ReadNBytes (char * str, uint length);
    errorT        ReadLine (char * str, uint maxLength);
    errorT        ReadLine (DString * dstr);
    inline errorT WriteOneByte (byte value);
    errorT        WriteTwoBytes (uint value);
    errorT        WriteThreeBytes (uint value);
    errorT        WriteFourBytes (uint value);
    inline int    ReadOneByte ();
    uint          ReadTwoBytes ();
    uint          ReadThreeBytes ();
    uint          ReadFourBytes ();

    inline char * GetFileName ();
};


inline char *
MFile::GetFileName ()
{
    if (FileName == NULL) {
        return (char *)"";
    } else {
        return FileName;
    }
}

inline bool
MFile::EndOfFile ()
{
    switch (Type) {
    case MFILE_MEMORY:
        return (Location >= Capacity);
    case MFILE_REGULAR:
        return feof(Handle);
    default:
        return false;
    }
}

inline errorT
MFile::WriteOneByte (byte value)
{
    ASSERT (FileMode != FMODE_ReadOnly);
    if (Type == MFILE_MEMORY) {
        if (Location >= Capacity) { Extend(); }
        *CurrentPtr++ = value;
        Location++;
        return OK;
    }
    Location++;
    return (putc(value, Handle) == EOF) ? ERROR_FileWrite : OK;
}

inline int
MFile::ReadOneByte ()
{
    ASSERT (FileMode != FMODE_WriteOnly);
    if (Type == MFILE_MEMORY) {
        if (Location >= Capacity) { return EOF; }
        byte value = *CurrentPtr;
        Location++;
        CurrentPtr++;
        return (int) value;
    }
    Location++;
    return  getc(Handle);
}

#endif  // SCID_MFILE_H

