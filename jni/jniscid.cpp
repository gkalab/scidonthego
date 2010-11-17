#include <string.h>
#include <jni.h>
#include <unistd.h>
#include <stdlib.h>

#include "scid/common.h"
#include "scid/index.h"
#include "scid/namebase.h"
#include "scid/gfile.h"
#include "scid/game.h"

int main(int argc, char* argv[]);

static Game * game = new Game;

typedef uint filterOpT;
const filterOpT FILTEROP_AND = 2;
const filterOpT FILTEROP_OR = 1;
const filterOpT FILTEROP_RESET = 0;

/*
 * Class:     org_scid_database_DataBase
 * Method:    loadGame
 * Signature: (Ljava/lang/String;)V
 */
extern "C" JNIEXPORT void JNICALL Java_org_scid_database_DataBase_loadGame
                (JNIEnv* env, jobject obj, jstring fileName, jint gameNo)
{
    game->Clear();
    const char* sourceFileName = (*env).GetStringUTFChars(fileName, NULL);
    if (sourceFileName) {
        Index * sourceIndex = new Index;
        NameBase * sourceNameBase = new NameBase;
        GFile * sourceGFile = new GFile;
        ByteBuffer * bbuf = new ByteBuffer;
        bbuf->SetBufferSize (BBUF_SIZE);
        errorT err = 0;

        sourceIndex->SetFileName(sourceFileName);
        sourceNameBase->SetFileName(sourceFileName);
        if (sourceIndex->OpenIndexFile(FMODE_ReadOnly) != OK) {
            return;
        }
        if (sourceNameBase->ReadNameFile() != OK) {
            return;
        }
        if (sourceGFile->Open(sourceFileName, FMODE_ReadOnly) != OK) {
            return;
        }
        if (gameNo < sourceIndex->GetNumGames()) {
            IndexEntry iE;
            err = sourceIndex->ReadEntries(&iE, gameNo, 1);
            if (err != OK) {
                return;
            }
            bbuf->Empty();
            err = sourceGFile->ReadGame(bbuf, iE.GetOffset(), iE.GetLength());
            if (err != OK) {
                return;
            }
            if (game->Decode(bbuf, GAME_DECODE_ALL) != OK) {
                return;
            }
            game->LoadStandardTags(&iE, sourceNameBase);
            game->AddPgnStyle(PGN_STYLE_TAGS);
            game->AddPgnStyle(PGN_STYLE_COMMENTS);
            game->AddPgnStyle(PGN_STYLE_VARS);
            game->SetPgnFormat(PGN_FORMAT_Plain);
        }
        // cleanup
        sourceIndex->CloseIndexFile();
        sourceNameBase->Clear();
        sourceGFile->Close();
        (*env).ReleaseStringUTFChars(fileName, sourceFileName);
        return;
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getSize
 * Signature: (Ljava/lang/String;)I
 */
extern "C" JNIEXPORT jint JNICALL Java_org_scid_database_DataBase_getSize
        (JNIEnv* env, jobject obj, jstring fileName)
{
    const char* sourceFileName = (*env).GetStringUTFChars(fileName, NULL);
    if (sourceFileName) {
        Index * sourceIndex = new Index;

        sourceIndex->SetFileName(sourceFileName);
        if (sourceIndex->OpenIndexFile(FMODE_ReadOnly) != OK) {
            return 0;
        }
        int result = sourceIndex->GetNumGames();
        // cleanup
        sourceIndex->CloseIndexFile();
        (*env).ReleaseStringUTFChars(fileName, sourceFileName);
        return result;
    }
    return 0;
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getPGN
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getPGN
        (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        TextBuffer * tbuf = new TextBuffer;
        tbuf->SetBufferSize(TBUF_SIZE);
        tbuf->Empty();
        tbuf->SetWrapColumn(99999);
        game->WriteToPGN(tbuf);
        return (*env).NewStringUTF(tbuf->GetBuffer());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getMoves
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getMoves
        (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        TextBuffer * tbuf = new TextBuffer;
        tbuf->SetBufferSize(TBUF_SIZE);
        tbuf->Empty();
        tbuf->SetWrapColumn(99999);

        game->MoveToPly(0);
        moveT * m = (moveT *) new moveT;
        m->prev = m->next = m->varParent = m->varChild = NULL;
        m->numVariations = 0;
        m->comment = NULL;
        m->nagCount = 0;
        m->nags[0] = 0;
        m->marker = NO_MARKER;
        m->san[0] = 0;
        game->WriteMoveList (tbuf, 0, m, true, false);
        delete m;
        tbuf->PrintWord(RESULT_LONGSTR[game->GetResult()]);

        return (*env).NewStringUTF(tbuf->GetBuffer());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getResult
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getResult
        (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        return (*env).NewStringUTF(RESULT_LONGSTR[game->GetResult()]);
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getWhite
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getWhite
                (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        return (*env).NewStringUTF(game->GetWhiteStr());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getBlack
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getBlack
                (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        return (*env).NewStringUTF(game->GetBlackStr());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getEvent
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getEvent
                (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        return (*env).NewStringUTF(game->GetEventStr());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getSite
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getSite
                (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        return (*env).NewStringUTF(game->GetSiteStr());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getDate
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getDate
                (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        char dateStr [20];
        date_DecodeToString(game->GetDate(), dateStr);
        return (*env).NewStringUTF(dateStr);
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}

/*
 * Class:     org_scid_database_DataBase
 * Method:    getRound
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_scid_database_DataBase_getRound
                (JNIEnv* env, jobject obj)
{
    if (game->GetNumHalfMoves() > 0) {
        return (*env).NewStringUTF(game->GetRoundStr());
    } else {
        static char emptyString = 0;
        return (*env).NewStringUTF(&emptyString);
    }
}


extern "C" JNIEXPORT jintArray JNICALL Java_org_scid_database_DataBase_searchBoard
        (JNIEnv *env, jobject obj, jstring fileName, jstring position,
         jint typeOfSearch,
         jint filterOperation, jintArray currentFilter)
{
    jintArray result;
    int filterOp = filterOperation;
    const char* sourceFileName = (*env).GetStringUTFChars(fileName, NULL);
    const char* fen = (*env).GetStringUTFChars(position, NULL);
    if (sourceFileName && fen) {
        Index * sourceIndex = new Index;
        GFile * sourceGFile = new GFile;
        ByteBuffer * bbuf = new ByteBuffer;
        bbuf->SetBufferSize (BBUF_SIZE);
        errorT err = 0;

        sourceIndex->SetFileName(sourceFileName);
        if (sourceIndex->OpenIndexFile(FMODE_ReadOnly) != OK) {
            return (*env).NewIntArray(0);
        }
        if (sourceGFile->Open(sourceFileName, FMODE_ReadOnly) != OK) {
            return (*env).NewIntArray(0);
        }
        int noGames = sourceIndex->GetNumGames();

        if (noGames > 0) {
            // type of search
            bool useHpSigSpeedup = false;
            gameExactMatchT searchType = GAME_EXACT_MATCH_Exact;
            switch (typeOfSearch) {
                case 0:
                    searchType = GAME_EXACT_MATCH_Exact;
                    useHpSigSpeedup = true;
                    break;
                case 1:
                    searchType = GAME_EXACT_MATCH_Pawns;
                    useHpSigSpeedup = true;
                    break;
                case 2:
                    searchType = GAME_EXACT_MATCH_Fyles;
                    break;
                case 3:
                    searchType = GAME_EXACT_MATCH_Material;
                    break;
                default:
                    break;
            }

            // initialize filter
            jint *fill = new jint[noGames];
            memset(fill, 0, sizeof(fill));

            jsize len = (*env).GetArrayLength(currentFilter);
            if (len != noGames) {
                // the currentFilter should have been initialized to the same length as the database, so reset the filter now
                for (int i=0; i<noGames; i++) {
                    fill[i] = 1;
                }
                } else {
                    jint *arr = (*env).GetIntArrayElements(currentFilter, 0);
                    for (int i=0; i<noGames; i++) {
                        fill[i] = arr[i];
                    (*env).ReleaseIntArrayElements(currentFilter, arr, 0);
                }
            }

            // setup FEN position
            game->Clear();
            Position * pos = new Position;
            if (pos->ReadFromFEN (fen) != OK) {
                if (pos->ReadFromLongStr (fen) != OK) {
                    // invalid FEN
                    return (*env).NewIntArray(0);
                }
            }
            // ReadFromFEN checks that there is one king of each side, but it
            // does not check that the position is actually legal:
            if (! pos->IsLegal()) {
               return (*env).NewIntArray(0);
            }
            matSigT msig = matsig_Make (pos->GetMaterial());
            uint hpSig = pos->GetHPSig();

            // If filter operation is to reset the filter, reset it:
            if (filterOp == FILTEROP_RESET) {
                for (int i=0; i < noGames; i++) {
                    fill[i] = 1;
                }
                filterOp = FILTEROP_AND;
            }

            // Here is the loop that searches on each game:
            IndexEntry * ie;
            Game * g = new Game;
            uint gameNum;

            int progress_mod = noGames / 100;
            int lastCallbackGameNo = -1;
            for (gameNum=0; gameNum < sourceIndex->GetNumGames(); gameNum++) {
                //  show progress
                // make sure to only call callback not more than 100 times
                if (gameNum % progress_mod == 0 && gameNum != lastCallbackGameNo) {
                    lastCallbackGameNo = gameNum;
                    jclass cls = env->GetObjectClass(obj);
                    jmethodID mid = env->GetMethodID(cls, "callback", "(I)V");
                    if (mid != 0) {
                        env->CallVoidMethod(obj, mid, gameNum);
                    }
                    env->DeleteLocalRef(cls);
                }

                // First, apply the filter operation:
                if (filterOp == FILTEROP_AND) {  // Skip any games not in the filter:
                    if (fill[gameNum] == 0) {
                    continue;
                    }
                } else /* filterOp==FILTEROP_OR*/ { // Skip any games in the filter:
                    if (fill[gameNum] != 0) {
                    continue;
                    } else {
                    // OK, this game is NOT in the filter.
                    // Add it so filterCounts are kept up to date:
                    fill[gameNum] = 1;
                    }
                }

                IndexEntry ie;
                err = sourceIndex->ReadEntries(&ie, gameNum, 1);
                if (err != OK) {
                    // Skip games with no gamefile record:
                    fill[gameNum] = 0;
                    continue;
                }

                bool possibleMatch = true;
                bool useVars = false;
                // Apply speedups if we are not searching in variations:
                if (! useVars) {
                    if (! ie.GetStartFlag()) {
                        // Speedups that only apply to standard start games:
                        if (useHpSigSpeedup  &&  hpSig != 0xFFFF) {
                            const byte * hpData = ie.GetHomePawnData();
                            if (! hpSig_PossibleMatch (hpSig, hpData)) {
                                possibleMatch = false;
                            }
                        }
                    }

                    // If this game has no promotions, check the material of its final
                    // position, since the searched position might be unreachable:
                    if (possibleMatch) {
                        if (!matsig_isReachable (msig, ie.GetFinalMatSig(),
                                                 ie.GetPromotionsFlag(),
                                                 ie.GetUnderPromoFlag())) {
                                possibleMatch = false;
                            }
                    }
                }

                if (!possibleMatch) {
                    fill[gameNum] = 0;
                    continue;
                }

                // At this point, the game needs to be loaded:
                bbuf->Empty();
                err = sourceGFile->ReadGame(bbuf, ie.GetOffset(), ie.GetLength());
                if (err != OK) {
                    fill[gameNum] = 0;
                    continue;
                }
                uint ply = 0;
                // No searching in variations:
                if (possibleMatch) {
                    if (g->ExactMatch (pos, bbuf, NULL, searchType)) {
                        // Set its auto-load move number to the matching move:
                        ply = g->GetCurrentPly() + 1;
                    }
                }
                if (ply > 255) { ply = 255; }
                // save the ply to filter gameNum
                // if ply==0 --> not found
                fill[gameNum] = ply;
            }
            delete pos;
            result = (*env).NewIntArray(noGames);
            if (result == NULL) {
                return NULL; // out of memory error thrown
            }
            (*env).SetIntArrayRegion(result, 0, noGames, fill);
        } else {
            result = (*env).NewIntArray(0);
        }

        // cleanup
        sourceIndex->CloseIndexFile();
        sourceGFile->Close();
        (*env).ReleaseStringUTFChars(fileName, sourceFileName);
        (*env).ReleaseStringUTFChars(position, fen);
        return result;
    }
    result = (*env).NewIntArray(0);
    return result;
}


/*
 * Class:     org_scid_database_DataBase
 * Method:    create
 * Signature: (Ljava/lang/String;)V
 */
extern "C" JNIEXPORT void JNICALL Java_org_scid_database_DataBase_create
                (JNIEnv* env, jobject obj, jstring fileName)
{
    game->Clear();
    const char* targetFileName = (*env).GetStringUTFChars(fileName, NULL);
    if (targetFileName) {
        Index * targetIndex = new Index;
        NameBase * targetNameBase = new NameBase;
        GFile * targetGFile = new GFile;
        targetIndex->SetFileName (targetFileName);
        targetNameBase->SetFileName (targetFileName);
        // Check that the target database does not already exist:
        Index * tempIndex = new Index;
        tempIndex->SetFileName (targetFileName);
        if (tempIndex->OpenIndexFile(FMODE_ReadOnly) == OK) {
            tempIndex->CloseIndexFile();
            //fprintf (stderr, "Error: the database %s already exists.\n", targetFileName);
            return;
        }

        // Open the target files:
        if (targetIndex->CreateIndexFile(FMODE_WriteOnly) != OK) {
            //fileError ("creating", targetFileName, INDEX_SUFFIX);
            return;
        }
        if (targetGFile->Create (targetFileName, FMODE_WriteOnly) != OK) {
            //fileError ("creating", targetFileName, GFILE_SUFFIX);
            return;
        }

        // Now all files have been read. All we need do is close the new base:
        targetIndex->CloseIndexFile();
        if (targetNameBase->WriteNameFile() != OK) {
            //fileError ("writing", targetFileName, NAMEBASE_SUFFIX);
            return;
        }
        if (targetGFile->Close() != OK) {
            //fileError ("closing", targetFileName, GFILE_SUFFIX);
            return;
        }

        // Remove any treefile for this database:
        removeFile (targetFileName, TREEFILE_SUFFIX);
            (*env).ReleaseStringUTFChars(fileName, targetFileName);
        return;
    }
}

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// matchGameHeader():
//    Called by search header to test a particular game against the
//    header search criteria.
bool
matchGameHeader (IndexEntry * ie, NameBase * nb,
                 bool * mWhite, bool * mBlack,
                 bool * mEvent, bool * mSite, bool *mRound,
                 dateT dateMin, dateT dateMax, bool * results,
                 int weloMin, int weloMax, int beloMin, int beloMax,
                 int diffeloMin, int diffeloMax,
                 ecoT ecoMin, ecoT ecoMax, bool ecoNone,
                 uint halfmovesMin, uint halfmovesMax,
                 bool wToMove, bool bToMove)
{
    // First, check the numeric ranges:

    if (!results[ie->GetResult()]) { return false; }

    uint halfmoves = ie->GetNumHalfMoves();
    if (halfmoves < halfmovesMin  ||  halfmoves > halfmovesMax) {
        return false;
    }
    if ((halfmoves % 2) == 0) {
        // This game ends with White to move:
        if (! wToMove) { return false; }
    } else {
        // This game ends with Black to move:
        if (! bToMove) { return false; }
    }


    dateT date = ie->GetDate();
    if (date < dateMin  ||  date > dateMax) { return false; }

    // Check Elo ratings:
    int whiteElo = (int) ie->GetWhiteElo();
    int blackElo = (int) ie->GetBlackElo();
    if (whiteElo == 0) { whiteElo = nb->GetElo (ie->GetWhite()); }
    if (blackElo == 0) { blackElo = nb->GetElo (ie->GetBlack()); }

    int diffElo = whiteElo - blackElo;
    // Elo difference used to be absolute difference, but now it is
    // just white rating minus black rating, so leave next line commented:
    //if (diffElo < 0) { diffElo = -diffElo; }

    if (whiteElo < weloMin  ||  whiteElo > weloMax) { return false; }
    if (blackElo < beloMin  ||  blackElo > beloMax) { return false; }
    if (diffElo < diffeloMin  ||  diffElo > diffeloMax) { return false; }

    ecoT ecoCode = ie->GetEcoCode();
    if (ecoCode == ECO_None) {
        if (!ecoNone) { return false; }
    } else {
        if (ecoCode < ecoMin  ||  ecoCode > ecoMax) { return false; }
    }

    // Now check the event, site and round fields:
    if (mEvent != NULL  &&  !mEvent[ie->GetEvent()]) { return false; }
    if (mSite != NULL  &&  !mSite[ie->GetSite()]) { return false; }
    if (mRound != NULL  &&  !mRound[ie->GetRound()]) { return false; }

    // Last, we check the players
    if (mWhite != NULL  &&  !mWhite[ie->GetWhite()]) { return false; }
    if (mBlack != NULL  &&  !mBlack[ie->GetBlack()]) { return false; }

    // If we reach here, this game matches all criteria.
    return true;
}

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// matchGameFlags():
//    Called by sc_search_header to test a particular game against the
//    specified index flag restrictions, for example, excluding
//    deleted games or games without comments.
bool
matchGameFlags (IndexEntry * ie, flagT fStdStart, flagT fPromos,
                flagT fComments, flagT fVars, flagT fNags, flagT fDelete,
                flagT fWhiteOp, flagT fBlackOp, flagT fMiddle,
                flagT fEndgame, flagT fNovelty, flagT fPawn,
                flagT fTactics, flagT fKside, flagT fQside,
                flagT fBrill, flagT fBlunder, flagT fUser,
                flagT fCustom1, flagT fCustom2, flagT fCustom3,
                flagT fCustom4, flagT fCustom5, flagT fCustom6 )
{
    bool flag;

    flag = ie->GetStartFlag();
    if ((flag && !flag_Yes(fStdStart))  ||  (!flag && !flag_No(fStdStart))) {
        return false;
    }

    flag = ie->GetPromotionsFlag();
    if ((flag && !flag_Yes(fPromos))  ||  (!flag && !flag_No(fPromos))) {
        return false;
    }

    flag = ie->GetCommentsFlag();
    if ((flag && !flag_Yes(fComments))  ||  (!flag && !flag_No(fComments))) {
        return false;
    }

    flag = ie->GetVariationsFlag();
    if ((flag && !flag_Yes(fVars))  ||  (!flag && !flag_No(fVars))) {
        return false;
    }

    flag = ie->GetNagsFlag();
    if ((flag && !flag_Yes(fNags))  ||  (!flag && !flag_No(fNags))) {
        return false;
    }

    flag = ie->GetDeleteFlag();
    if ((flag && !flag_Yes(fDelete))  ||  (!flag && !flag_No(fDelete))) {
        return false;
    }

    flag = ie->GetWhiteOpFlag();
    if ((flag && !flag_Yes(fWhiteOp))  ||  (!flag && !flag_No(fWhiteOp))) {
        return false;
    }

    flag = ie->GetBlackOpFlag();
    if ((flag && !flag_Yes(fBlackOp))  ||  (!flag && !flag_No(fBlackOp))) {
        return false;
    }

    flag = ie->GetMiddlegameFlag();
    if ((flag && !flag_Yes(fMiddle))  ||  (!flag && !flag_No(fMiddle))) {
        return false;
    }

    flag = ie->GetEndgameFlag();
    if ((flag && !flag_Yes(fEndgame))  ||  (!flag && !flag_No(fEndgame))) {
        return false;
    }

    flag = ie->GetNoveltyFlag();
    if ((flag && !flag_Yes(fNovelty))  ||  (!flag && !flag_No(fNovelty))) {
        return false;
    }

    flag = ie->GetPawnStructFlag();
    if ((flag && !flag_Yes(fPawn))  ||  (!flag && !flag_No(fPawn))) {
        return false;
    }

    flag = ie->GetTacticsFlag();
    if ((flag && !flag_Yes(fTactics))  ||  (!flag && !flag_No(fTactics))) {
        return false;
    }

    flag = ie->GetKingsideFlag();
    if ((flag && !flag_Yes(fKside))  ||  (!flag && !flag_No(fKside))) {
        return false;
    }

    flag = ie->GetQueensideFlag();
    if ((flag && !flag_Yes(fQside))  ||  (!flag && !flag_No(fQside))) {
        return false;
    }

    flag = ie->GetBrilliancyFlag();
    if ((flag && !flag_Yes(fBrill))  ||  (!flag && !flag_No(fBrill))) {
        return false;
    }

    flag = ie->GetBlunderFlag();
    if ((flag && !flag_Yes(fBlunder))  ||  (!flag && !flag_No(fBlunder))) {
        return false;
    }

    flag = ie->GetUserFlag();
    if ((flag && !flag_Yes(fUser))  ||  (!flag && !flag_No(fUser))) {
        return false;
    }

    flag = ie->GetCustomFlag(1);
    if ((flag && !flag_Yes(fCustom1))  ||  (!flag && !flag_No(fCustom1))) {
      return false;
    }

    flag = ie->GetCustomFlag(2);
    if ((flag && !flag_Yes(fCustom2))  ||  (!flag && !flag_No(fCustom2))) {
      return false;
    }

    flag = ie->GetCustomFlag(3);
    if ((flag && !flag_Yes(fCustom3))  ||  (!flag && !flag_No(fCustom3))) {
      return false;
    }

    flag = ie->GetCustomFlag(4);
    if ((flag && !flag_Yes(fCustom4))  ||  (!flag && !flag_No(fCustom4))) {
      return false;
    }

    flag = ie->GetCustomFlag(5);
    if ((flag && !flag_Yes(fCustom5))  ||  (!flag && !flag_No(fCustom5))) {
      return false;
    }

    flag = ie->GetCustomFlag(6);
    if ((flag && !flag_Yes(fCustom6))  ||  (!flag && !flag_No(fCustom6))) {
      return false;
    }

    // If we reach here, the game matched all flag restrictions.
    return true;
}

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//    Searches by header information.
extern "C" JNIEXPORT jintArray JNICALL Java_org_scid_database_DataBase_searchHeader
        (JNIEnv *env, jobject obj, jstring fileName, jstring white, jstring black, jboolean ignoreColors,
         jboolean result_win_white, jboolean result_draw, jboolean result_win_black, jboolean result_none,
         jstring event, jstring site, jstring ecoFrom, jstring ecoTo, jboolean includeEcoNone,
         jint filterOperation, jintArray currentFilter)
{
    jintArray result;
    int filterOp = filterOperation;
    const char* sourceFileName = (*env).GetStringUTFChars(fileName, NULL);
    const char* strWhite = (*env).GetStringUTFChars(white, NULL);
    const char* strBlack = (*env).GetStringUTFChars(black, NULL);
    const char* strEvent = (*env).GetStringUTFChars(event, NULL);
    const char* strSite = (*env).GetStringUTFChars(site, NULL);
    const char* strEcoFrom = (*env).GetStringUTFChars(ecoFrom, NULL);
    const char* strEcoTo = (*env).GetStringUTFChars(ecoTo, NULL);
    if (!sourceFileName || !strWhite || !strBlack || !strEvent || !strSite || !strEcoFrom || !strEcoTo) {
      result = (*env).NewIntArray(0);
      return result;
    }
    Index * sourceIndex = new Index;
    NameBase * sourceNameBase = new NameBase;
    GFile * sourceGFile = new GFile;
    errorT err = 0;

    sourceIndex->SetFileName(sourceFileName);
    sourceNameBase->SetFileName(sourceFileName);
    if (sourceIndex->OpenIndexFile(FMODE_ReadOnly) != OK) {
        return (*env).NewIntArray(0);
    }
    if (sourceNameBase->ReadNameFile() != OK) {
        return (*env).NewIntArray(0);
    }
    if (sourceGFile->Open(sourceFileName, FMODE_ReadOnly) != OK) {
        return (*env).NewIntArray(0);
    }
    int noGames = sourceIndex->GetNumGames();

    if (noGames <= 0) {
        result = (*env).NewIntArray(0);
        return result;
    }

    // initialize filter
    jint *fill = new jint[noGames];
    memset(fill, 0, sizeof(fill));

    jsize len = (*env).GetArrayLength(currentFilter);
    if (len != noGames) {
        // the currentFilter should have been initialized to the same length as the database, so reset the filter now
        for (int i=0; i<noGames; i++) {
            fill[i] = 1;
        }
    } else {
        jint *arr = (*env).GetIntArrayElements(currentFilter, 0);
        for (int i=0; i<noGames; i++) {
            fill[i] = arr[i];
        }
        (*env).ReleaseIntArrayElements(currentFilter, arr, 0);
    }

    char * sWhite = NULL;
    char * sBlack = NULL;
    char * sEvent = NULL;
    char * sSite  = NULL;
    char * sRound = NULL;

    bool * mWhite = NULL;
    bool * mBlack = NULL;
    bool * mEvent = NULL;
    bool * mSite = NULL;
    bool * mRound = NULL;

    dateT dateRange[2];
    dateRange[0] = ZERO_DATE;
    dateRange[1] = DATE_MAKE (YEAR_MAX, 12, 31);

    bool results [NUM_RESULT_TYPES];
    bool resultsF [NUM_RESULT_TYPES];  // Flipped results for ignore-colors.
    results[RESULT_White] = results[RESULT_Black] = true;
    results[RESULT_Draw] = results[RESULT_None] = true;

    uint wEloRange [2];   // White rating range
    uint bEloRange [2];   // Black rating range
    int  dEloRange [2];   // Rating difference (White minus Black) range
    wEloRange[0] = bEloRange[0] = 0;
    wEloRange[1] = bEloRange[1] = MAX_ELO;
    dEloRange[0] = - (int)MAX_ELO;
    dEloRange[1] = MAX_ELO;

    bool * wTitles = NULL;
    bool * bTitles = NULL;

    bool wToMove = true;
    bool bToMove = true;

    uint halfMoveRange[2];
    halfMoveRange[0] = 0;
    halfMoveRange[1] = 999;

    ecoT ecoRange [2];    // ECO code range.
    ecoRange[0] = eco_FromString ("A00");
    ecoRange[1] = eco_FromString ("E99");
    bool ecoNone = includeEcoNone;  // Whether to include games with no ECO code.

    // gameNumRange: a range of game numbers to search.
    int gameNumRange[2];
    gameNumRange[0] = 1;   // Default: start searching at 1st game.
    gameNumRange[1] = -1;  // Default: stop searching at last game.

    flagT fStdStart = FLAG_BOTH;
    flagT fPromotions = FLAG_BOTH;
    flagT fComments = FLAG_BOTH;
    flagT fVariations = FLAG_BOTH;
    flagT fAnnotations = FLAG_BOTH;
    flagT fDelete = FLAG_BOTH;
    flagT fWhiteOp = FLAG_BOTH;
    flagT fBlackOp = FLAG_BOTH;
    flagT fMiddlegame = FLAG_BOTH;
    flagT fEndgame = FLAG_BOTH;
    flagT fNovelty = FLAG_BOTH;
    flagT fPawnStruct = FLAG_BOTH;
    flagT fTactics = FLAG_BOTH;
    flagT fKside = FLAG_BOTH;
    flagT fQside = FLAG_BOTH;
    flagT fBrilliancy = FLAG_BOTH;
    flagT fBlunder = FLAG_BOTH;
    flagT fUser = FLAG_BOTH;
    flagT fCustom1 = FLAG_BOTH;
    flagT fCustom2 = FLAG_BOTH;
    flagT fCustom3 = FLAG_BOTH;
    flagT fCustom4 = FLAG_BOTH;
    flagT fCustom5 = FLAG_BOTH;
    flagT fCustom6 = FLAG_BOTH;

    if (strWhite[0] != 0) {
        sWhite = strDuplicate (strWhite);
    }
    if (strBlack[0] != 0) {
        sBlack = strDuplicate (strBlack);
    }
    if (strEvent[0] != 0) {
        sEvent = strDuplicate (strEvent);
    }
    if (strSite[0] != 0) {
        sSite = strDuplicate (strSite);
    }
    results[RESULT_White] = result_win_white;
    results[RESULT_Draw] = result_draw;
    results[RESULT_Black] = result_win_black;
    results[RESULT_None] = result_none;

    if (strEcoFrom[0] != 0 && strEcoTo[0] != 0) {
        ecoRange[0] = eco_FromString (strEcoFrom);
        ecoRange[1] = eco_FromString (strEcoTo);
    }

    // Set up White name matches array:
    if (sWhite != NULL  &&  sWhite[0] != 0) {
        char * search = sWhite;
        // Search players for match on White name:
        idNumberT numNames = sourceNameBase->GetNumNames(NAME_PLAYER);
        mWhite = new bool [numNames];
        for (idNumberT i=0; i < numNames; i++) {
            char * name = sourceNameBase->GetName (NAME_PLAYER, i);
            mWhite[i] = strAlphaContains (name, search);
        }
    }

    // Set up Black name matches array:
    if (sBlack != NULL  &&  sBlack[0] != 0) {
        char * search = sBlack;
        // Search players for match on Black name:
        idNumberT numNames = sourceNameBase->GetNumNames(NAME_PLAYER);
        mBlack = new bool [numNames];
        for (idNumberT i=0; i < numNames; i++) {
            char * name = sourceNameBase->GetName (NAME_PLAYER, i);
            mBlack[i] = strAlphaContains (name, search);
        }
    }

    // Set up Event name matches array:
    if (sEvent != NULL  &&  sEvent[0] != 0) {
        char * search = sEvent;
        // Search players for match on Event name:
        idNumberT numNames = sourceNameBase->GetNumNames(NAME_EVENT);
        mEvent = new bool [numNames];
        for (idNumberT i=0; i < numNames; i++) {
            char * name = sourceNameBase->GetName (NAME_EVENT, i);
            mEvent[i] = strAlphaContains (name, search);
        }
    }

    // Set up Site name matches array:
    if (sSite != NULL  &&  sSite[0] != 0) {
        char * search = sSite;
        // Search players for match on Site name:
        idNumberT numNames = sourceNameBase->GetNumNames(NAME_SITE);
        mSite = new bool [numNames];
        for (idNumberT i=0; i < numNames; i++) {
            char * name = sourceNameBase->GetName (NAME_SITE, i);
            mSite[i] = strAlphaContains (name, search);
        }
    }

    // Set up Round name matches array:
    if (sRound != NULL  &&  sRound[0] != 0) {
        char * search = sRound;
        // Search players for match on Event name:
        idNumberT numNames = sourceNameBase->GetNumNames(NAME_ROUND);
        mRound = new bool [numNames];
        for (idNumberT i=0; i < numNames; i++) {
            char * name = sourceNameBase->GetName (NAME_ROUND, i);
            mRound[i] = strAlphaContains (name, search);
        }
    }

    // Set up flipped results flags for ignore-colors option:
    resultsF[RESULT_White] = results[RESULT_Black];
    resultsF[RESULT_Draw]  = results[RESULT_Draw];
    resultsF[RESULT_Black] = results[RESULT_White];
    resultsF[RESULT_None]  = results[RESULT_None];

    // Swap rating difference values if necesary:
    if (dEloRange[0] > dEloRange[1]) {
        int x = dEloRange[0]; dEloRange[0] = dEloRange[1]; dEloRange[1] = x;
    }

    // Set eco maximum to be the largest subcode, for example,
    // "B07" -> "B07z4" to make sure subcodes are included in the range:
    ecoRange[1] = eco_LastSubCode (ecoRange[1]);

    // Set up game number range:
    // Note that a negative number means a count from the end,
    // so -1 = last game, -2 = second to last, etc.
    // Convert any negative values to positive:
    if (gameNumRange[0] < 0) { gameNumRange[0] += noGames + 1; }
    if (gameNumRange[1] < 0) { gameNumRange[1] += noGames + 1; }
    if (gameNumRange[0] < 0) { gameNumRange[0] = 0; }
    if (gameNumRange[1] < 0) { gameNumRange[1] = 0; }
    uint gameNumMin = (uint) gameNumRange[0];
    uint gameNumMax = (uint) gameNumRange[1];
    if (gameNumMin > noGames) { gameNumMin = noGames; }
    if (gameNumMax > noGames) { gameNumMax = noGames; }
    // Swap them if necessary so min <= max:
    if (gameNumMin > gameNumMax) {
        uint temp = gameNumMin; gameNumMin = gameNumMax; gameNumMax = temp;
    }

    char temp[250];
    IndexEntry * ie;
    uint updateStart, update;
    updateStart = update = 5000;  // Update progress bar every 5000 games

    // If filter operation is to reset the filter, reset it:
    if (filterOp == FILTEROP_RESET) {
        for (int i=0; i < noGames; i++) {
            fill[i] = 1;
        }
        filterOp = FILTEROP_AND;
    }

    // Here is the loop that searches on each game:
    int progress_mod = noGames / 100;
    uint i=0;
    int lastCallbackGameNo = -1;
    for (; i < noGames; i++) {
        // show progress
        // make sure to only call callback not more than 100 times
        if (i % progress_mod == 0 && i != lastCallbackGameNo) {
            lastCallbackGameNo = i;
            jclass cls = env->GetObjectClass(obj);
            jmethodID mid = env->GetMethodID(cls, "callback", "(I)V");
            if (mid != 0) {
                env->CallVoidMethod(obj, mid, i);
            }
            env->DeleteLocalRef(cls);
        }

        // First, apply the filter operation:
        if (filterOp == FILTEROP_AND) {  // Skip any games not in the filter:
            if (fill[i] == 0) {
                continue;
            }
        } else /* filterOp == FILTEROP_OR*/ { // Skip any games in the filter:
            if (fill[i] != 0) {
                continue;
            } else {
                // OK, this game is NOT in the filter.
                // Add it so filterCounts are kept up to date:
                fill[i] = 1;
            }
        }

        // Skip games outside the specified game number range:
        if (i+1 < gameNumMin  ||  i+1 > gameNumMax) {
            fill[i] = 0;
            continue;
        }

        ie = sourceIndex->FetchEntry (i);
        if (ie->GetLength() == 0) {  // Skip games with no gamefile record
            fill[i] = 0;
            continue;
        }

        bool match = false;
        if (matchGameFlags (ie, fStdStart, fPromotions,
                            fComments, fVariations, fAnnotations, fDelete,
                            fWhiteOp, fBlackOp, fMiddlegame, fEndgame,
                            fNovelty, fPawnStruct, fTactics, fKside,
                            fQside, fBrilliancy, fBlunder, fUser,
                            fCustom1, fCustom2, fCustom3, fCustom4, fCustom5, fCustom6
                           )) {
            if (matchGameHeader (ie, sourceNameBase, mWhite, mBlack,
                                 mEvent, mSite, mRound,
                                 dateRange[0], dateRange[1], results,
                                 wEloRange[0], wEloRange[1],
                                 bEloRange[0], bEloRange[1],
                                 dEloRange[0], dEloRange[1],
                                 ecoRange[0], ecoRange[1], ecoNone,
                                 halfMoveRange[0], halfMoveRange[1],
                                 wToMove, bToMove)) {
                match = true;
            }

            // Try with inverted players/ratings/results if ignoring colors:

            if (!match  &&  ignoreColors  &&
                matchGameHeader (ie, sourceNameBase, mBlack, mWhite,
                                 mEvent, mSite, mRound,
                                 dateRange[0], dateRange[1], resultsF,
                                 bEloRange[0], bEloRange[1],
                                 wEloRange[0], wEloRange[1],
                                 -dEloRange[1], -dEloRange[0],
                                 ecoRange[0], ecoRange[1], ecoNone,
                                 halfMoveRange[0], halfMoveRange[1],
                                 bToMove, wToMove)) {
                match = true;
            }
        }

        if (match) {
            // Game matched, so update the filter value. Only change it
            // to 1 if it is currently 0:
            if (fill[i] == 0) {
                fill[i] == 1; // set ply number to 1
            }
        } else {
            // This game did NOT match:
            fill[i] = 0; // set ply number to 0
        }
    }

    result = (*env).NewIntArray(i);
    if (i > 0) {
        if (result == NULL) {
            return NULL; // out of memory error thrown
        }
        (*env).SetIntArrayRegion(result, 0, i, fill);
    }

    if (sWhite != NULL) { delete[] sWhite; }
    if (sBlack != NULL) { delete[] sBlack; }
    if (sEvent != NULL) { delete[] sEvent; }
    if (sSite  != NULL) { delete[] sSite;  }
    if (sRound != NULL) { delete[] sRound; }
    if (mWhite != NULL) { delete[] mWhite; }
    if (mBlack != NULL) { delete[] mBlack; }
    if (mEvent != NULL) { delete[] mEvent; }
    if (mSite  != NULL) { delete[] mSite;  }
    if (mRound != NULL) { delete[] mRound; }
    if (wTitles != NULL) { delete[] wTitles; }
    if (bTitles != NULL) { delete[] bTitles; }

    // cleanup
    sourceIndex->CloseIndexFile();
    sourceNameBase->Clear();
    sourceGFile->Close();
    (*env).ReleaseStringUTFChars(fileName, sourceFileName);
    (*env).ReleaseStringUTFChars(white, strWhite);
    (*env).ReleaseStringUTFChars(black, strBlack);
    (*env).ReleaseStringUTFChars(event, strEvent);
    (*env).ReleaseStringUTFChars(site, strSite);
    (*env).ReleaseStringUTFChars(ecoFrom, strEcoFrom);
    (*env).ReleaseStringUTFChars(ecoTo, strEcoTo);
    return result;
}
