#include "scid/common.h"
#include "scid/index.h"
#include "scid/namebase.h"
#include "scid/gfile.h"
#include "scid/game.h"
#include "scid/pgnparse.h"

#include <android/log.h>
#include <jni.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

#include <algorithm>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
using namespace std;


/// JNI helpers
#define  LOG_TAG    "SCIDjni"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

// Java class method
#define JCM(resultType, methodName, ...)                    \
    extern "C" JNIEXPORT resultType                         \
    JNICALL Java_org_scid_database_DataBase_##methodName    \
  (JNIEnv* env, jclass cls, ##__VA_ARGS__)

#define CHECK(op)                               \
    if((op) != OK) return false

#define CHECKL(op,...)                          \
    if((op) != OK){                             \
        LOGW(__VA_ARGS__);                      \
        return false;                           \
    }

#define FILE_LOADED                             \
    if(not fileLoaded){                         \
        LOGE("%s: no file loaded", __func__);   \
        return 0;                               \
    }

#define GAME_LOADED                             \
    if(not gameLoaded){                         \
        LOGE("%s: no game loaded", __func__);   \
        return 0;                               \
    }

#define PROPER_NAME_TYPE                                    \
    if(not (nameType >= 0 and nameType < NUM_NAME_TYPES)){  \
        LOGE("%s: bad nameType %d", __func__, nameType);    \
        return 0;                                           \
    }

#define PREPARE_PROGRESS(noGames)                                       \
    jmethodID midIsCanceled, midPublishProgress;                        \
    uint progressDelta, nextCallbackGameNo;                             \
    if(progress){                                                       \
        jclass cls = env->GetObjectClass(progress);                     \
        midPublishProgress = env->GetMethodID(cls, "publishProgress", "(I)V"); \
        midIsCanceled = env->GetMethodID(cls, "isCancelled", "()Z");    \
        env->DeleteLocalRef(cls);                                       \
        progressDelta = (noGames) / 100;                                \
        nextCallbackGameNo = progressDelta;                             \
    } else {                                                            \
        LOGD("No progress");                                            \
    }

#define DO_PROGRESS(gameNum, noGames)                               \
    if(progress and (gameNum) >= nextCallbackGameNo){               \
        nextCallbackGameNo = (gameNum) + progressDelta;             \
        int percent = float(gameNum)*100/(noGames);                 \
        env->CallVoidMethod(progress, midPublishProgress, percent); \
        if(env->CallBooleanMethod(progress, midIsCanceled)){        \
            LOGD("cancelled");                                      \
            break;                                                  \
        }                                                           \
    }

class AutoJString {             // automatic release of jstring chars
    JNIEnv* env;
    jstring j;                  // java string
    const char* c;              // C string
public:
    AutoJString(JNIEnv* env, jstring j)
        :env(env),
         j(j),
         c(env->GetStringUTFChars(j,0))
    {
        // LOGI("( %s", c);
    }
    ~AutoJString(){
        // LOGI(") %s", c);
        env->ReleaseStringUTFChars(j, c);
    }
    const char* c_str() const { return c; }
    operator const char*() const { return c; }
};
#define AJS(name) const AutoJString name(env, j##name) // local "str" from parameter "jstr"

class AutoJArray {              // automatic release of short[] chars
    JNIEnv* env;
    jshortArray j;              // java array
    jshort* c;                  // C array
public:
    AutoJArray(JNIEnv* env, jshortArray j)
        :env(env),
         j(j),
         c(env->GetShortArrayElements(j,0)){}
    ~AutoJArray(){
        env->ReleaseShortArrayElements(j, c, 0);
    }
    operator jshort* () { return c; }
};
#define AJA(name) AutoJArray name(env, j##name) // local "arr" from parameter "jarr"

/// Global state
typedef uint filterOpT;
const filterOpT FILTEROP_RESET = 0, FILTEROP_OR = 1, FILTEROP_AND = 2;

static Index sourceIndex;
static NameBase sourceNameBase;
static GFile sourceGFile;
static ByteBuffer bbuf;
static bool fileLoaded = false;

static bool gameLoaded = false;
static gameNumberT gameId;
static IndexEntry ie;
static Game game;

static void unloadGame(){
    if(gameLoaded){
        game.Clear();
        gameLoaded = false;
    }
}
static void unloadFile(){
    unloadGame();
    if(fileLoaded){
        LOGI("unloading index");
        sourceIndex.CloseIndexFile();
        sourceIndex.Clear();
        sourceNameBase.Clear();
        sourceGFile.Close();
        fileLoaded = false;
    }
}
static errorT reopenIndexForWriting(){
    if(sourceIndex.GetFileMode() != FMODE_Both){
        sourceIndex.CloseIndexFile();
        if(sourceIndex.OpenIndexFile(FMODE_Both) != OK){
            LOGE("cannot open index for writing");
            sourceIndex.OpenIndexFile(FMODE_ReadOnly);
            return ERROR;
        }
    }
    return OK;
}
static errorT reopenGFileForWriting(){
    if(sourceGFile.GetFileMode() != FMODE_Both){
        sourceGFile.Close();
        string fname = sourceGFile.GetFileNameWithSuffix();
        if(sourceGFile.Open(fname.c_str(), FMODE_Both, "") != OK){
            LOGE("cannot open game file for writing");
            sourceGFile.Open(fname.c_str(), FMODE_ReadOnly, "");
            return ERROR;
        }
    }
    return OK;
}

/// Loading and operations with loaded file
JCM(jboolean, loadFile, jstring jfname){
    AJS(fname);
    if(fileLoaded and strcmp(fname, sourceIndex.GetFileName()) == 0){
        LOGI("loadFile: file %s is already loaded", fname.c_str());
        return true;
    }

    unloadFile();
    LOGI("loadFile: %s\n", fname.c_str());
    sourceIndex.SetFileName(fname);

    sourceNameBase.SetFileName(fname);
    CHECKL(sourceIndex.OpenIndexFile(FMODE_ReadOnly), "OpenIndexFile");
    CHECKL(sourceNameBase.ReadNameFile(),"ReadNameFile");
    CHECKL(sourceGFile.Open(fname, FMODE_ReadOnly),"Open");
    bbuf.SetBufferSize(BBUF_SIZE);
    LOGI("file loaded\n");
    fileLoaded = true;
    return true;
}
JCM(jint, getSize){
    FILE_LOADED;
    return sourceIndex.GetNumGames();
}
JCM(jint, getNamesCount, jint nameType){
    PROPER_NAME_TYPE;
    FILE_LOADED;
    return sourceNameBase.GetNumNames(nameType);
}
JCM(jstring, getName, jint nameType, jint id){
    PROPER_NAME_TYPE;
    FILE_LOADED;
    return env->NewStringUTF(sourceNameBase.GetName(nameType, id));
}
// getMatchingNames must be reentrant and use case-insensitive
// comparison (thus we cannot use the name tree)
struct CaseCmp{
    nameT nameType;
    const char* name(jint id){
        return sourceNameBase.GetName(nameType, id);
    }
    bool operator()(jint a, jint b){
        return strcasecmp(name(a),name(b)) < 0;
    }
};
JCM(jintArray, getMatchingNames, jint nameType, jstring jprefix){
    PROPER_NAME_TYPE;
    FILE_LOADED;
    AJS(prefix);
    vector<jint> matches; // idNumberT is uint and thus compatible with jint
    uint numNames = sourceNameBase.GetNumNames(nameType);
    if(size_t len = strlen(prefix)){
        for(uint i = 0; i < numNames; ++i){
            if(strncasecmp(prefix, sourceNameBase.GetName(nameType, i), len) == 0)
                matches.push_back(i);
        }
    }else{                      // if prefix is "", then return all names
        matches.resize(numNames);
        for(uint i = 0; i < numNames; ++i)
            matches[i] = i;
    }
    LOGD("getNames: got %d", matches.size());

    CaseCmp caseCmp = {nameType};
    sort(matches.begin(), matches.end(), caseCmp);

    jintArray result = env->NewIntArray(matches.size());
    env->SetIntArrayRegion(result, 0, matches.size(), &matches[0]);
    return result;
}

/// Loading and operations with the loaded game
JCM(jboolean, loadGame, jint gameId, jboolean onlyHeaders){
    FILE_LOADED;
    unloadGame();
    if(gameId < 0 or gameId >= sourceIndex.GetNumGames()){
        LOGE("loadGame: %d is out of range", gameId);
        return false;
    }
    ::gameId = gameId;
    CHECK(sourceIndex.ReadEntries(&ie, gameId, 1));
    bbuf.Empty();
    CHECK(sourceGFile.ReadGame(&bbuf, ie.GetOffset(), ie.GetLength()));
    if(onlyHeaders){
        game.SetNumHalfMoves(ie.GetNumHalfMoves());
    } else {
        CHECKL(game.Decode(&bbuf, GAME_DECODE_ALL), "Unable to decode game.");
    }
    game.LoadStandardTags(&ie, &sourceNameBase);
    game.AddPgnStyle(PGN_STYLE_TAGS);
    game.AddPgnStyle(PGN_STYLE_COMMENTS);
    game.AddPgnStyle(PGN_STYLE_VARS);
    game.SetPgnFormat(PGN_FORMAT_Plain);

    gameLoaded = true;
    return true;
}
JCM(jbyteArray, getPGN){
    GAME_LOADED;
    TextBuffer tbuf;
    tbuf.SetBufferSize(TBUF_SIZE);
    tbuf.Empty();
    tbuf.SetWrapColumn(99999);
    game.WriteToPGN(&tbuf);
    int length = strlen(tbuf.GetBuffer()); // TODO: why strlen?
    jbyteArray result = env->NewByteArray(length);
    env->SetByteArrayRegion(result, 0, length,(const jbyte*) tbuf.GetBuffer());
    return result;
}
JCM(jstring, getMoves){
    GAME_LOADED;
    if(game.GetNumHalfMoves() == 0)
        return env->NewStringUTF("");

    TextBuffer tbuf;
    tbuf.SetBufferSize(TBUF_SIZE);
    tbuf.Empty();
    tbuf.SetWrapColumn(99999);

    game.MoveToPly(0);
    moveT m;
    m.prev = m.next = m.varParent = m.varChild = 0;
    m.numVariations = 0;
    m.comment = 0;
    m.nagCount = 0;
    m.nags[0] = 0;
    m.marker = NO_MARKER;
    m.san[0] = 0;
    game.WriteMoveList(&tbuf, 0, &m, true, false);
    tbuf.PrintWord(RESULT_LONGSTR[game.GetResult()]);

    return env->NewStringUTF(tbuf.GetBuffer());
}
JCM(jint, getResult){
    GAME_LOADED;
    return game.GetResult();
}
#define _(funcionName, fieldAccessor)                                   \
    JCM(jbyteArray, funcionName){                                       \
        GAME_LOADED;                                                    \
        int length = strlen(game.fieldAccessor());                      \
        jbyteArray result = env->NewByteArray(length);                  \
        env->SetByteArrayRegion(result, 0, length,                      \
                                (const jbyte*) game.fieldAccessor());   \
        return result;                                                  \
    }
_(getWhite, GetWhiteStr)
_(getBlack, GetBlackStr)
_(getEvent, GetEventStr)
_(getSite, GetSiteStr)
_(getRound, GetRoundStr)
#undef _
JCM(jstring, getDate){
    GAME_LOADED;
    char dateStr[20];
    date_DecodeToString(game.GetDate(), dateStr);
    return env->NewStringUTF(dateStr);
}
JCM(jint, getWhiteElo){
    GAME_LOADED;
    return game.GetWhiteElo();
}
JCM(jint, getBlackElo){
    GAME_LOADED;
    return game.GetBlackElo();
}
JCM(jboolean, isFavorite){
    GAME_LOADED;
    return ie.GetUserFlag();
}
JCM(jboolean, isDeleted){
    GAME_LOADED;
    return ie.GetDeleteFlag();
}

/// Create database (new or import)
JCM(jstring, create, jstring jtargetFileName){
    AJS(targetFileName);
    if(not targetFileName)
        return 0;

    Index targetIndex;
    NameBase targetNameBase;
    GFile targetGFile;
    targetIndex.SetFileName(targetFileName);
    targetIndex.SetDescription("");
    targetNameBase.SetFileName(targetFileName);
    // Check that the target database does not already exist:
    Index tempIndex;
    tempIndex.SetFileName(targetFileName);
    if(tempIndex.OpenIndexFile(FMODE_ReadOnly) == OK){
        tempIndex.CloseIndexFile();
        return env->NewStringUTF("Error: the database already exists.");
    }

#define _(op,msg) if((op) != OK) return env->NewStringUTF(msg)
    // Open the target files:
    _(targetIndex.CreateIndexFile(FMODE_Both), "Error creating index file.");
    _(targetNameBase.WriteNameFile(), "Error writing name base file.");
    _(targetGFile.Create(targetFileName, FMODE_Both), "Error creating game file.");
    LOGD("Index file written.");
    _(targetIndex.WriteHeader(), "Error writing index header.");

    // Now all files have been created. All we need do is close the new base:
    targetIndex.CloseIndexFile();
    _(targetGFile.Close(), "Error closing game file.");

    // Remove any treefile for this database:
    removeFile(targetFileName, TREEFILE_SUFFIX);
#undef _
    return env->NewStringUTF("");
}
JCM(jstring, importPgn, jstring jpgnName, jobject progress){
    AJS(pgnName);
    if(not pgnName) return 0;

#define _(op,msg) if((op) != OK) return env->NewStringUTF(msg)

    MFile pgnFile;
    _(pgnFile.Open(pgnName, FMODE_ReadOnly), "Could not open pgn file");
    PgnParser pgnParser(&pgnFile);
    ByteBuffer bbuf;
    Index idx;
    Game game;
    GFile gameFile;
    NameBase nb;
    IndexEntry ie;
    uint t = 0;   // = time(0);
    int lastCallbackPercent = -1;
    uint pgnFileSize = fileSize(pgnName, "");
    // Ensure positive file size counter to avoid division by zero:
    if(pgnFileSize < 1){ pgnFileSize = 1; }
    PREPARE_PROGRESS(pgnFileSize);

    // Make baseName from pgnName if baseName is not provided:
    fileNameT baseName;
    strCopy(baseName, pgnName); // TODO: security flaw if len(pgnName) > 512
    // Trim the ".pgn" suffix:
    strTrimFileSuffix(baseName);

    // Try opening the log file:
    fileNameT fname;
    strCopy(fname, baseName);
    strAppend(fname, ".err");
    FILE * logFile = fopen(fname, "w");

    string resultString = "";
    if(logFile == 0){
        resultString.append("Could not open log file.\n");
        goto cleanup;
    }

    scid_Init();

    if((gameFile.Create(baseName, FMODE_WriteOnly)) != OK){
        // could not create the game file
        pgnFile.Close();
        goto cleanup;
    }
    idx.SetFileName(baseName);
    idx.CreateIndexFile(FMODE_WriteOnly);
    gameNumberT gNumber;

    bbuf.SetBufferSize(BBUF_SIZE); // 32768

    pgnParser.SetErrorFile(logFile);
    pgnParser.SetPreGameText(true);

    // TODO: Add command line option for ignored tags, rather than
    //       just hardcoding PlyCount as the only ignored tag.
    pgnParser.AddIgnoredTag("PlyCount");

    // Add each game found to the database:
    while(pgnParser.ParseGame(&game) != ERROR_NotFound){
        ie.Init();

        if(idx.AddGame(&gNumber, &ie) != OK){
            resultString.append("Too many games!");
            goto cleanup;
        }

        // Add the names to the namebase:
        idNumberT id = 0;

        if(nb.AddName(NAME_PLAYER, game.GetWhiteStr(), &id) != OK){
            resultString.append("Too many names: ");
            resultString.append(NAME_PLAYER);
            goto cleanup;
        }
        nb.IncFrequency(NAME_PLAYER, id, 1);
        ie.SetWhite(id);

        if(nb.AddName(NAME_PLAYER, game.GetBlackStr(), &id) != OK){
            resultString.append("Too many names: ");
            resultString.append(NAME_PLAYER);
            goto cleanup;
        }
        nb.IncFrequency(NAME_PLAYER, id, 1);
        ie.SetBlack(id);

        if(nb.AddName(NAME_EVENT, game.GetEventStr(), &id) != OK){
            resultString.append("Too many names: ");
            resultString.append(NAME_TYPE_STRING [NAME_EVENT]);
            goto cleanup;
        }
        nb.IncFrequency(NAME_EVENT, id, 1);
        ie.SetEvent(id);

        if(nb.AddName(NAME_SITE, game.GetSiteStr(), &id) != OK){
            resultString.append("Too many names: ");
            resultString.append(NAME_TYPE_STRING [NAME_SITE]);
            goto cleanup;
        }
        nb.IncFrequency(NAME_SITE, id, 1);
        ie.SetSite(id);

        if(nb.AddName(NAME_ROUND, game.GetRoundStr(), &id) != OK){
            resultString.append("Too many names: ");
            resultString.append(NAME_TYPE_STRING [NAME_ROUND]);
            goto cleanup;
        }
        nb.IncFrequency(NAME_ROUND, id, 1);
        ie.SetRound(id);

        bbuf.Empty();
        if(game.Encode(&bbuf, &ie) != OK){
            resultString.append("Fatal error encoding game!\n");
            goto cleanup;
        }
        uint offset = 0;
        if(gameFile.AddGame(&bbuf, &offset) != OK){
            resultString.append("Fatal error writing game file!\n");
            goto cleanup;
        }
        ie.SetOffset(offset);
        ie.SetLength(bbuf.GetByteCount());
        idx.WriteEntries(&ie, gNumber, 1);

        int bytesSeen = pgnParser.BytesUsed();
        DO_PROGRESS(bytesSeen, pgnFileSize);
    }

    nb.SetTimeStamp(t);
    nb.SetFileName(baseName);
    if(nb.WriteNameFile() != OK){
        resultString.append("Fatal error writing name file!\n");
        goto cleanup;
    }

    /*printf("\nDatabase `%s': %d games, %d players, %d events, %d sites.\n",
      baseName, idx.GetNumGames(), nb.GetNumNames(NAME_PLAYER),
      nb.GetNumNames(NAME_EVENT), nb.GetNumNames(NAME_SITE));*/
    fclose(logFile);
    if(pgnParser.ErrorCount() > 0){
        FILE * errFile = fopen(fname, "r");
        char line[100];
        while( fgets(line, sizeof(line), errFile) != 0 ){
            resultString.append(line);
        }
        fclose(errFile);
    }
    removeFile(baseName, ".err");
    gameFile.Close();
    idx.CloseIndexFile();
    idx.Clear();

    // If there is a tree cache file for this database, it is out of date:
    removeFile(baseName, TREEFILE_SUFFIX);
    pgnFile.Close();
#undef _
 cleanup:
    return env->NewStringUTF(resultString.c_str());
}

/// Filtering
JCM(jboolean, searchBoard,
    jstring jfen, jint/*gameExactMatchT*/ searchType,
    jint filterOperation, jshortArray/*in-out*/ jfilter, jobject progress){
    FILE_LOADED;

    AJS(fen);
    if(not fen){
        LOGE("searchBoard: fen is null");
        return false;
    }
    // setup FEN position
    Position pos;
    if(not ((pos.ReadFromFEN(fen) == OK or pos.ReadFromLongStr(fen) == OK)
         and pos.IsLegal())){
        LOGE("searchBoard: invalid FEN '%s'", fen.c_str());
        return false;
    }
    matSigT msig = matsig_Make(pos.GetMaterial());
    uint hpSig;
    bool useHpSigSpeedup;
    switch(searchType){
    case GAME_EXACT_MATCH_Exact:
    case GAME_EXACT_MATCH_Pawns:
        hpSig = pos.GetHPSig();
        useHpSigSpeedup = true;
        break;
    case GAME_EXACT_MATCH_Fyles:
    case GAME_EXACT_MATCH_Material:
        useHpSigSpeedup = false;
        break;
    default:
        LOGE("searchBoard: wrong typeOfSearch %d", searchType);
        return false;
    }

    AJA(filter);
    if(not filter){
        LOGE("searchBoard: filter is null");
        return false;
    }
    gameNumberT noGames = sourceIndex.GetNumGames();
    if(noGames != env->GetArrayLength(jfilter)){
        LOGE("searchBoard: filter has wrong length");
        return false;
    }

    // Here is the loop that searches on each game:
    Game g;
    PREPARE_PROGRESS(noGames);
    for(gameNumberT i = 0; i < noGames; ++i){
        DO_PROGRESS(i, noGames);
        // First, apply the filter operation:
        if(filterOperation == FILTEROP_AND and !filter[i]
           or filterOperation == FILTEROP_OR and filter[i]){
            // no need to change filter[i]
            continue;
        }
        IndexEntry ie;
        if(sourceIndex.ReadEntries(&ie, i, 1) != OK){ // TODO: maybe FetchEntry?
            // Skip games with no gamefile record:
            filter[i] = 0;
            continue;
        }

        bool possibleMatch = true;
        bool useVars = false; // TODO: allow user to change
        // Apply speedups if we are not searching in variations:
        if(not useVars){
            if(not ie.GetStartFlag()){
                // Speedups that only apply to standard start games:
                if(useHpSigSpeedup and hpSig != 0xFFFF){
                    const byte * hpData = ie.GetHomePawnData();
                    if(not hpSig_PossibleMatch(hpSig, hpData)){
                        possibleMatch = false;
                    }
                }
            }

            // If this game has no promotions, check the material of its final
            // position, since the searched position might be unreachable:
            if(possibleMatch){
                if(not matsig_isReachable(msig, ie.GetFinalMatSig(),
                                         ie.GetPromotionsFlag(),
                                         ie.GetUnderPromoFlag())){
                    possibleMatch = false;
                }
            }
        }

        if(not possibleMatch){
            filter[i] = 0;
            continue;
        }

        // At this point, the game needs to be loaded:
        bbuf.Empty();
        if(sourceGFile.ReadGame(&bbuf, ie.GetOffset(), ie.GetLength()) != OK){
            LOGI("searchBoard: cannot read game %d", i);
            filter[i] = 0;
            continue;
        }
        uint ply = 0;
        // No searching in variations:
        if(possibleMatch){
            if(g.ExactMatch(&pos, &bbuf, 0, gameExactMatchT(searchType))){
                // Set its auto-load move number to the matching move:
                ply = g.GetCurrentPly() + 1;
                const int MAX_JSHORT = (1<<15) - 1; // 2^15 - 1
                if(ply > MAX_JSHORT) ply = MAX_JSHORT;
            }
        }
        filter[i] = jshort(ply);
    }
    return true;
}
// Called by search header to test a particular game against the
// header search criteria.
inline bool matchGameHeader
(IndexEntry * ie, NameBase * nb,
 const bit_vector& mWhite, const bit_vector& mBlack, bool ignoreColors,
 const bit_vector& mEvent, const bit_vector& mSite, const bit_vector& mRound,
 dateT dateMin, dateT dateMax,
 bool results[NUM_RESULT_TYPES],
 bool allowUnknownElo,
 int whiteEloMin, int whiteEloMax, int blackEloMin, int blackEloMax,
 int diffEloMin, int diffEloMax,
 int minEloMin, int minEloMax, int maxEloMin, int maxEloMax,
 uint halfMovesMin, uint halfMovesMax, bool halfMovesEven, bool halfMovesOdd,
 ecoT ecoMin, ecoT ecoMax, bool ecoNone,
 bool annotatedOnly){
#define CI(op) /* continue if */ if(not (op)) return false
#define CIIR(a) /*continue if in range */ \
        CI((a) >= (a##Min) and (a) <= (a##Max))
#define _(Name) CI(m##Name.empty() or m##Name[ie->Get##Name()])
    if(ignoreColors){
        CI(mWhite.empty() or (mWhite[ie->GetWhite()] or mWhite[ie->GetBlack()]));
        CI(mBlack.empty() or (mBlack[ie->GetWhite()] or mBlack[ie->GetBlack()]));
    }else{
        _(White); _(Black);
    }
    _(Event); _(Site); _(Round);
#undef _
    CI(results[ie->GetResult()]);

    uint halfMoves = ie->GetNumHalfMoves(); CIIR(halfMoves);
    if((halfMoves % 2) == 0){
        // This game ends with White to move *if* White moves first
        CI(halfMovesEven);
    } else {
        CI(halfMovesOdd);
    }

    dateT date = ie->GetDate(); CIIR(date);

    // Check Elo ratings:
    int whiteElo = ie->GetWhiteElo();
    int blackElo = ie->GetBlackElo();
    if(whiteElo == 0){ whiteElo = nb->GetElo(ie->GetWhite()); }
    if(blackElo == 0){ blackElo = nb->GetElo(ie->GetBlack()); }
    if(whiteElo and blackElo){  // diff, min, max are useful only if both ELOs are known
        int minElo = min(whiteElo, blackElo); CIIR(minElo);
        int maxElo = min(whiteElo, blackElo); CIIR(maxElo);
        int diffElo = maxElo - minElo; CIIR(diffElo);
    }else{ // there are unknown ELOs
        CI(allowUnknownElo);
    }
    if(whiteElo) CIIR(whiteElo);
    if(blackElo) CIIR(blackElo);

    ecoT eco = ie->GetEcoCode();
    if(eco == ECO_None){
        CI(ecoNone);
    } else {
        CIIR(eco);
    }

    if(annotatedOnly)
        CI(ie->GetCommentsFlag() or ie->GetVariationsFlag() or ie->GetNagsFlag());
#undef CI
#undef CIIR
    // If we reach here, this game matches all criteria.
    return true;
}
/* TODO
// Called by search header to test a particular game against the
// specified index flag restrictions, for example, excluding
// deleted games or games without comments.
inline bool matchGameFlags
(IndexEntry * ie,
 flagT fStart, flagT fPromotions, flagT fComments, flagT fVariations, flagT fNags,
 flagT fDelete, flagT fWhiteOp, flagT fBlackOp, flagT fMiddlegame, flagT fEndgame,
 flagT fNovelty, flagT fPawnStruct, flagT fTactics, flagT fKingside, flagT fQueenside,
 flagT fBrilliancy, flagT fBlunder, flagT fUser,
 flagT fCustom1, flagT fCustom2, flagT fCustom3,
 flagT fCustom4, flagT fCustom5, flagT fCustom6){
    bool flag;
#define _(flagName)                                 \
    flag = ie->Get##flagName##Flag();               \
    if(    flag and not flag_Yes(f##flagName) or    \
       not flag and not flag_No(f##flagName))       \
       return false

    _(Start); _(Promotions); _(Comments); _(Variations); _(Nags);
    _(Delete); _(WhiteOp); _(BlackOp); _(Middlegame); _(Endgame);
    _(Novelty); _(PawnStruct); _(Tactics); _(Kingside); _(Queenside);
    _(Brilliancy); _(Blunder); _(User);
#undef _
#define _(n)                                    \
    flag = ie->GetCustomFlag(n);                \
    if( flag and !flag_Yes(fCustom##n) or        \
       !flag and !flag_No (fCustom##n))          \
       return false
    _(1); _(2); _(3); _(4); _(5); _(6);
#undef _
    // If we reach here, the game matched all flag restrictions.
    return true;
}
*/
JCM(jboolean, searchHeader,
    jobject request, jint filterOperation, jshortArray/*in-out*/ jfilter, jobject progress){
    FILE_LOADED;

    jclass requestClass = env->GetObjectClass(request);
    // String, boolean, int fields of request
#define SF(field)                                                       \
    jstring j##field = jstring(env->GetObjectField                      \
        (request,                                                       \
         env->GetFieldID(requestClass, #field, "Ljava/lang/String;"))); \
    if(not j##field){                                                   \
        LOGE("searchHeader: " #field " is null");                       \
        return false;                                                   \
    }                                                                   \
    AJS(field)
#define BF(field)                                               \
    jboolean field = env->GetBooleanField                       \
        (request, env->GetFieldID(requestClass, #field, "Z"))
#define IF(field)                                               \
    jint field = env->GetIntField                               \
        (request, env->GetFieldID(requestClass, #field, "I"))

#define _(name) SF(name); BF(name##Exact)
    _(white); _(black); _(event); _(site); _(round);
#undef _

    SF(ecoFrom); SF(ecoTo);

    BF(ignoreColors);
    BF(resultNone); BF(resultWhiteWins); BF(resultBlackWins); BF(resultDraw);
    BF(ecoNone); BF(allowUnknownElo);  BF(annotatedOnly);
    BF(halfMovesEven); BF(halfMovesOdd);

#define _(a) IF(a##Min); IF(a##Max)
    _(date); _(id);
    _(whiteElo); _(blackElo);
    _(diffElo); _(minElo); _(maxElo);
    _(halfMoves);
#undef _

#undef SF
#undef BF

    AJA(filter);
    if(not filter){
        LOGE("searchHeader: filter is null");
        return false;
    }
    gameNumberT noGames = sourceIndex.GetNumGames();
    if(noGames != env->GetArrayLength(jfilter)){
        LOGE("searchHeader: filter has wrong length");
        return false;
    }

    bool results[NUM_RESULT_TYPES] = // order from RESULT_None, ...
        {resultNone, resultWhiteWins, resultBlackWins, resultDraw};

    ecoT ecoMin, ecoMax;    // ECO code range.
    if(ecoFrom[0] and ecoTo[0]){
        ecoMin = eco_FromString(ecoFrom);
        // Set eco maximum to be the largest subcode, for example,
        // "B07" -> "B07z4" to make sure subcodes are included in the range:
        ecoMax = eco_LastSubCode(eco_FromString(ecoTo));
    } else {
        ecoMin = eco_FromString("A00");
        ecoMax = eco_FromString("E99");
    }

    /* TODO
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
    */

#define _(name, Name, TYPE)                                             \
    bit_vector m##Name;                                                 \
    if(name[0]){                                                        \
        idNumberT numNames = sourceNameBase.GetNumNames(NAME_##TYPE);   \
        m##Name.resize(numNames);                                       \
        if(name##Exact){                                                \
            idNumberT id;                                               \
            if(sourceNameBase.FindExactName(NAME_##TYPE, name, &id) == OK) \
                m##Name[id] = true;                                     \
            else{                                                       \
                LOGW("searchHeader: " #name " does not match exactly"); \
                return false;                                           \
            }                                                           \
        }else{                                                          \
            for(idNumberT i = 0; i < numNames; ++i)                     \
                m##Name[i] = strAlphaContains                           \
                    (sourceNameBase.GetName(NAME_##TYPE, i), name);     \
        }                                                               \
    }
    _(white, White, PLAYER);
    _(black, Black, PLAYER);
    _(event, Event, EVENT);
    _(site, Site, SITE);
    _(round, Round, ROUND);
#undef _

    // Here is the loop that searches on each game:
    PREPARE_PROGRESS(noGames);
    for(uint i = 0; i < noGames; ++i){
        DO_PROGRESS(i, noGames);
        // First, apply the filter operation:
        if(filterOperation == FILTEROP_AND and !filter[i]
           or filterOperation == FILTEROP_OR and filter[i]){
            // no need to change filter[i]
            continue;
        }
        IndexEntry * ie;
        if((i >= idMin and i <= idMax)
           and (ie = sourceIndex.FetchEntry(i)) != 0
           and ie->GetLength() // Skip games with no gamefile record
           /* TODO
           and
           matchGameFlags(ie, fStdStart, fPromotions,
                          fComments, fVariations, fAnnotations, fDelete,
                          fWhiteOp, fBlackOp, fMiddlegame, fEndgame,
                          fNovelty, fPawnStruct, fTactics, fKside,
                          fQside, fBrilliancy, fBlunder, fUser,
                          fCustom1, fCustom2, fCustom3, fCustom4, fCustom5, fCustom6)
           */
           and
           matchGameHeader(ie, &sourceNameBase,
                           mWhite, mBlack, ignoreColors,
                           mEvent, mSite, mRound,
                           dateMin, dateMax,
                           results,
                           allowUnknownElo,
                           whiteEloMin, whiteEloMax, blackEloMin, blackEloMax,
                           diffEloMin, diffEloMax,
                           minEloMin, minEloMax, maxEloMin, maxEloMax,
                           halfMovesMin, halfMovesMax, halfMovesEven, halfMovesOdd,
                           ecoMin, ecoMax, ecoNone,
                           annotatedOnly)){
            // Game matches
            if(filter[i] == 0)
                filter[i] = 1;
            // otherwise preserve non-zero value of filter[i]
        } else {
            // This game does NOT match
            filter[i] = 0;
        }
    }
    return true;
}
JCM(jintArray, getFavorites, jobject progress){
    FILE_LOADED;
    vector<int> result;
    int noGames = sourceIndex.GetNumGames();
    if(noGames <= 0){
        LOGI("getFavorites: no games in sourceIndex");
        return env->NewIntArray(0);
    }

    // Here is the loop that searches on each game:
    PREPARE_PROGRESS(noGames);
    for(uint i = 0; i < noGames; ++i){
        DO_PROGRESS(i, noGames);
        IndexEntry * ie = sourceIndex.FetchEntry(i);
        if(ie->GetLength() != 0 and ie->GetUserFlag()){
            // game has record and the user flag is set
            result.push_back(i);
        }
    }

    jintArray jresult = env->NewIntArray(result.size());
    if(result.size() and jresult)
        env->SetIntArrayRegion(jresult, 0, result.size(), &result[0]);
    return jresult;
}

/// Modifications
JCM(jboolean, setFavorite, jboolean isFavorite){
    FILE_LOADED;
    GAME_LOADED;
    if(isFavorite != ie.GetUserFlag()){
        CHECK(reopenIndexForWriting());
        ie.SetUserFlag(isFavorite);
        sourceIndex.WriteEntries(&ie, gameId, 1);
    }
    return true;
}
JCM(jboolean, setDeleted, jboolean isDeleted){
    FILE_LOADED;
    GAME_LOADED;
    if(isDeleted != ie.GetDeleteFlag()){
        CHECK(reopenIndexForWriting());
        ie.SetDeleteFlag(isDeleted);
        game.SetAltered(isDeleted);
        sourceIndex.WriteEntries(&ie, gameId, 1);
    }
    return true;
}
JCM(jstring, saveGame, jint gameId, jstring jpgn){
    FILE_LOADED;
    unloadGame();

    AJS(pgn);
    if(not pgn){
        LOGE("saveGame: pgn is null");
        return 0;
    }

#define _(op,msg) if((op) != OK) return env->NewStringUTF(msg)

    ByteBuffer *bbuf = new ByteBuffer;
    bbuf->SetBufferSize(BBUF_SIZE);
    PgnParser parser;
    parser.Reset(pgn);
    uint size=16000;
    LOGD("parsing game");
    parser.ParseGame(&game);
    LOGD("create index entry");
    // Grab a new idx entry, if needed:
    IndexEntry * oldIE = 0;
    IndexEntry * iE = new IndexEntry;
    iE->Init();

    bbuf->Empty();
    LOGD("encode game");
    _(game.Encode(bbuf, iE), "Error encoding game.");
    LOGD("finished encoding game");

    bool replaceMode = false;
    gameNumberT gNumber = gameId;
    if(gameId >= 0){
        replaceMode = true;
    }

    LOGD("Saving game.");

    _(reopenIndexForWriting(), "Unable to reopen index file for writing.");
    _(reopenGFileForWriting(), "Unable to reopen game file for writing.");
    _(sourceIndex.ReadEntries(iE, gNumber, 1), "Error reading index entry.");
    LOGD("All files loaded.");

    // game.Encode computes flags, so we have to re-set flags if replace mode
    if(replaceMode){
        oldIE = sourceIndex.FetchEntry(gNumber);
        LOGD("Old index entry fetched.");
        // Remember previous user-settable flags:
        for(uint flag = 0; flag < IDX_NUM_FLAGS; ++flag){
            char flags [32];
            oldIE->GetFlagStr(flags, 0);
            iE->SetFlagStr(flags);
        }
    } else {
        // add game without resetting the index, because it has been filled by game.encode above
        _(sourceIndex.AddGame(&gNumber, iE, false), "Too many games in this database.");
    }
    int noGames = sourceIndex.GetNumGames();

    bbuf->BackToStart();

    // Now try writing the game to the gfile:
    LOGD("Trying to write game to gfile.");
    uint offset = 0;
    _(sourceGFile.AddGame(bbuf, &offset), "Error writing game file.");
    iE->SetOffset(offset);
    iE->SetLength(bbuf->GetByteCount());
    LOGD("Game written to gfile.");

    // Now we add the names to the NameBase
    // If replacing, we decrement the frequency of the old names.
    const char * s;
    idNumberT id = 0;

#define __(Name, TYPE)                                                  \
    s = game.Get##Name##Str();  if(not s){ s = "?"; }                      \
    _(sourceNameBase.AddName(NAME_##TYPE, s, &id),                      \
      "Cannot add " #Name " as " #TYPE ".");                            \
    sourceNameBase.IncFrequency(NAME_##TYPE, id, 1);                    \
    iE->Set##Name(id);                                                  \
    LOGD(#Name " written to name base.")

    __(White, PLAYER);
    __(Black, PLAYER);
    __(Event, EVENT);
    __(Site, SITE);
    __(Round, ROUND);
#undef __

    // If replacing, decrement the frequency of the old names:
    if(replaceMode){
        sourceNameBase.IncFrequency(NAME_PLAYER, oldIE->GetWhite(), -1);
        sourceNameBase.IncFrequency(NAME_PLAYER, oldIE->GetBlack(), -1);
        sourceNameBase.IncFrequency(NAME_EVENT,  oldIE->GetEvent(), -1);
        sourceNameBase.IncFrequency(NAME_SITE,   oldIE->GetSite(),  -1);
        sourceNameBase.IncFrequency(NAME_ROUND,  oldIE->GetRound(), -1);
    }
    LOGD("Frequencies incremented.");

    iE->SetResult(game.GetResult());

    // Flush the gfile so it is up-to-date with other files:
    // This made copying games between databases VERY slow, so it
    // is now done elsewhere OUTSIDE a loop that copies many
    // games, such as in sc_filter_copy().
    sourceGFile.FlushAll();
    LOGD("All flushed.");

    // Last of all, we write the new idxEntry
    _(sourceIndex.WriteEntries(iE, gNumber, 1), "Error writing index file.");
    LOGD("Index file written.");
    _(sourceIndex.WriteHeader(), "Error writing index header.");
    LOGD("Index header written.");
    _(sourceNameBase.WriteNameFile(), "Error writing name file.");
    LOGD("Name file written.");

    // We need to increase the filter size if a game was added:
    if(not replaceMode){
        // TODO
    }
    return env->NewStringUTF("");
#undef _
}
// Local Variables:
// tab-width: 4
// c-basic-offset: 4
// End:
