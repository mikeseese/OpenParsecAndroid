/**
 * Parsec SDK C API compatibility header (V1).
 *
 * This header declares the Parsec SDK types and functions used by the JNI bridge.
 * It provides the minimal API surface needed for OpenParsec Android integration.
 * Struct layouts and function signatures match the official Parsec SDK V1 ABI
 * (as shipped in the MalfoyJW/parsec-sdk Android binaries).
 *
 * The Parsec SDK shared library (libparsec.so) must be placed in
 * app/src/main/jniLibs/{abi}/ for each target architecture.
 */

#ifndef PARSEC_H
#define PARSEC_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif


/*** DEFINITIONS ***/

#define GUEST_NAME_LEN   320
#define ATTEMPT_ID_LEN   56

#define PARSEC_VER_MAJOR 1
#define PARSEC_VER_MINOR 0

#define PARSEC_VER \
    ((uint32_t) (((uint16_t) PARSEC_VER_MAJOR << 16u) | ((uint16_t) PARSEC_VER_MINOR)))

/* Convenience key modifier */
#define MOD_NONE 0


/*** ENUMERATIONS ***/

typedef enum ParsecStatus {
    PARSEC_OK                 = 0,
    WRN_CONTINUE              = 10,
    ERR_DEFAULT               = -1,
    PARSEC_NOT_RUNNING        = -3,
    PARSEC_ALREADY_RUNNING    = -4,
    SO_ERR_OPEN               = -35000,
    SO_ERR_SYMBOL             = -35001,
    PARSEC_ERR_VERSION        = -36000,
    __PARSEC_STATUS_MAKE_32   = 0x7FFFFFFF,
} ParsecStatus;

/* Legacy alias used in Kotlin bridge */
#define PARSEC_ERR ERR_DEFAULT

typedef enum ParsecLogLevel {
    LOG_DEBUG      = 0,
    LOG_INFO       = 1,
    __LOG_MAKE_32  = 0x7FFFFFFF,
} ParsecLogLevel;

typedef enum ParsecMessageType {
    MESSAGE_KEYBOARD       = 1,
    MESSAGE_MOUSE_BUTTON   = 2,
    MESSAGE_MOUSE_WHEEL    = 3,
    MESSAGE_MOUSE_MOTION   = 4,
    MESSAGE_GAMEPAD_BUTTON = 5,
    MESSAGE_GAMEPAD_AXIS   = 6,
    MESSAGE_GAMEPAD_UNPLUG = 7,
    __MESSAGE_MAKE_32      = 0x7FFFFFFF,
} ParsecMessageType;

typedef enum ParsecClientEventType {
    CLIENT_EVENT_CURSOR    = 1,
    CLIENT_EVENT_RUMBLE    = 2,
    CLIENT_EVENT_USER_DATA = 3,
    CLIENT_EVENT_BLOCKED   = 4,
    CLIENT_EVENT_UNBLOCKED = 5,
    __CLIENT_EVENT_MAKE_32 = 0x7FFFFFFF,
} ParsecClientEventType;

typedef enum ParsecGuestState {
    GUEST_WAITING      = 0x01,
    GUEST_CONNECTING   = 0x02,
    GUEST_CONNECTED    = 0x04,
    GUEST_DISCONNECTED = 0x08,
    GUEST_FAILED       = 0x10,
    __GUEST_MAKE_32    = 0x7FFFFFFF,
} ParsecGuestState;

typedef enum ParsecHostMode {
    HOST_NONE      = 0,
    HOST_DESKTOP   = 1,
    HOST_GAME      = 2,
    __HOST_MAKE_32 = 0x7FFFFFFF,
} ParsecHostMode;

/* Opaque handle */
typedef struct Parsec Parsec;

/* Typedefs for input types */
typedef uint32_t ParsecKeycode;
typedef uint32_t ParsecKeymod;
typedef uint32_t ParsecMouseButton;
typedef uint32_t ParsecGamepadButton;
typedef uint32_t ParsecGamepadAxis;


/*** STRUCTS ***/

typedef struct ParsecConfig {
    int32_t upnp;
    int32_t clientPort;
    int32_t hostPort;
} ParsecConfig;

/* --- Cursor --- */

typedef struct ParsecCursor {
    uint32_t size;
    uint32_t positionX;
    uint32_t positionY;
    uint16_t width;
    uint16_t height;
    uint16_t hotX;
    uint16_t hotY;
    bool modeUpdate;
    bool imageUpdate;
    bool relative;
    uint8_t __pad[1];
} ParsecCursor;

/* --- Permissions & Metrics (needed for ParsecGuest / ParsecClientStatus) --- */

typedef struct ParsecPermissions {
    bool gamepad;
    bool keyboard;
    bool mouse;
    uint8_t __pad[1];
} ParsecPermissions;

typedef struct ParsecMetrics {
    float encodeLatency;
    float decodeLatency;
    float networkLatency;
} ParsecMetrics;

typedef struct ParsecGuest {
    ParsecPermissions perms;
    ParsecMetrics metrics;
    ParsecGuestState state;
    uint32_t id;
    uint32_t userID;
    char name[GUEST_NAME_LEN];
    char attemptID[ATTEMPT_ID_LEN];
    bool owner;
    uint8_t __pad[3];
} ParsecGuest;

/* --- Message structs --- */

typedef struct ParsecKeyboardMessage {
    ParsecKeycode code;
    ParsecKeymod mod;
    bool pressed;
    uint8_t __pad[3];
} ParsecKeyboardMessage;

typedef struct ParsecMouseButtonMessage {
    ParsecMouseButton button;
    bool pressed;
    uint8_t __pad[3];
} ParsecMouseButtonMessage;

typedef struct ParsecMouseWheelMessage {
    int32_t x;
    int32_t y;
} ParsecMouseWheelMessage;

typedef struct ParsecMouseMotionMessage {
    int32_t x;
    int32_t y;
    bool relative;
    uint8_t __pad[3];
} ParsecMouseMotionMessage;

typedef struct ParsecGamepadButtonMessage {
    ParsecGamepadButton button;
    uint32_t id;
    bool pressed;
    uint8_t __pad[3];
} ParsecGamepadButtonMessage;

typedef struct ParsecGamepadAxisMessage {
    ParsecGamepadAxis axis;
    uint32_t id;
    int16_t value;
    uint8_t __pad[2];
} ParsecGamepadAxisMessage;

typedef struct ParsecGamepadUnplugMessage {
    uint32_t id;
} ParsecGamepadUnplugMessage;

typedef struct ParsecMessage {
    ParsecMessageType type;
    union {
        ParsecKeyboardMessage keyboard;
        ParsecMouseButtonMessage mouseButton;
        ParsecMouseWheelMessage mouseWheel;
        ParsecMouseMotionMessage mouseMotion;
        ParsecGamepadButtonMessage gamepadButton;
        ParsecGamepadAxisMessage gamepadAxis;
        ParsecGamepadUnplugMessage gamepadUnplug;
    };
} ParsecMessage;

/* --- Client config --- */

typedef struct ParsecClientConfig {
    int32_t decoderSoftware;
    int32_t mediaContainer;
    int32_t protocol;
    int32_t resolutionX;
    int32_t resolutionY;
    int32_t refreshRate;
    bool pngCursor;
    uint8_t __pad[3];
} ParsecClientConfig;

/* --- Client Status --- */

typedef struct ParsecClientStatus {
    ParsecMetrics metrics;
    char attemptID[ATTEMPT_ID_LEN];
    bool networkFailure;
    bool decoderFallback;
    uint8_t __pad[1];
} ParsecClientStatus;

/* --- Event structs --- */

typedef struct ParsecClientCursorEvent {
    ParsecCursor cursor;
    uint32_t key;
} ParsecClientCursorEvent;

typedef struct ParsecClientRumbleEvent {
    uint32_t gamepadID;
    uint8_t motorBig;
    uint8_t motorSmall;
    uint8_t __pad[2];
} ParsecClientRumbleEvent;

typedef struct ParsecClientUserDataEvent {
    uint32_t id;
    uint32_t key;
} ParsecClientUserDataEvent;

typedef struct ParsecClientEvent {
    ParsecClientEventType type;
    union {
        ParsecClientCursorEvent cursor;
        ParsecClientRumbleEvent rumble;
        ParsecClientUserDataEvent userData;
    };
} ParsecClientEvent;


/*** CALLBACKS ***/

typedef void (*ParsecAudioCallback)(int16_t *pcm, uint32_t frames, void *opaque);
typedef void (*ParsecLogCallback)(ParsecLogLevel level, char *msg, void *opaque);


/*** SDK FUNCTIONS ***/

ParsecStatus ParsecInit(uint32_t ver, ParsecConfig *cfg, void *reserved, Parsec **ps);
void ParsecDestroy(Parsec *ps);

void ParsecSetLogCallback(ParsecLogCallback callback, void *opaque);
uint32_t ParsecVersion(void);

void *ParsecGetBuffer(Parsec *ps, uint32_t key);
void ParsecFree(void *ptr);

ParsecStatus ParsecClientConnect(Parsec *ps, ParsecClientConfig *cfg,
                                  char *sessionID, char *peerID);
void ParsecClientDisconnect(Parsec *ps);
ParsecStatus ParsecClientGetStatus(Parsec *ps, ParsecClientStatus *status);

ParsecStatus ParsecClientSetDimensions(Parsec *ps, uint32_t width,
                                        uint32_t height, float scale);

ParsecStatus ParsecClientGLRenderFrame(Parsec *ps, uint32_t timeout);
void ParsecClientGLDestroy(Parsec *ps);

ParsecStatus ParsecClientPollAudio(Parsec *ps, ParsecAudioCallback callback,
                                    uint32_t timeout, void *opaque);

bool ParsecClientPollEvents(Parsec *ps, uint32_t timeout,
                             ParsecClientEvent *event);

ParsecStatus ParsecClientSendMessage(Parsec *ps, ParsecMessage *msg);
ParsecStatus ParsecClientSendUserData(Parsec *ps, uint32_t id, char *text);

#ifdef __cplusplus
}
#endif

#endif /* PARSEC_H */
