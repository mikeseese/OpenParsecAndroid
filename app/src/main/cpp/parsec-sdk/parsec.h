/**
 * Parsec SDK C API compatibility header.
 *
 * This header declares the Parsec SDK types and functions used by the JNI bridge.
 * It provides the minimal API surface needed for OpenParsec Android integration.
 *
 * IMPORTANT: For production use, replace this file with the official Parsec SDK
 * header from https://parsec.app/docs/sdk. The struct layouts and function
 * signatures here match the official SDK API.
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

/* Version */
#define PARSEC_VER_MAJOR 8
#define PARSEC_VER_MINOR 0

/* Default stream index */
#define DEFAULT_STREAM 0

/* Log levels */
#define LOG_DEBUG 0
#define LOG_INFO  1

/* Status codes */
typedef int32_t ParsecStatus;
#define PARSEC_OK           0
#define PARSEC_CONNECTING   (-6001)
#define PARSEC_ERR          (-1)

/* Opaque handle */
typedef struct Parsec Parsec;

/* Keycodes */
typedef uint32_t ParsecKeycode;

/* Key modifier flags */
#define MOD_NONE 0

/* Message types */
typedef enum ParsecMessageType {
    MESSAGE_KEYBOARD       = 1,
    MESSAGE_MOUSE_BUTTON   = 2,
    MESSAGE_MOUSE_WHEEL    = 3,
    MESSAGE_MOUSE_MOTION   = 4,
    MESSAGE_GAMEPAD_BUTTON = 5,
    MESSAGE_GAMEPAD_AXIS   = 6,
    MESSAGE_GAMEPAD_UNPLUG = 7,
} ParsecMessageType;

/* Event types */
typedef enum ParsecClientEventType {
    CLIENT_EVENT_CURSOR    = 1,
    CLIENT_EVENT_USER_DATA = 2,
} ParsecClientEventType;

/* Mouse button type */
typedef uint32_t ParsecMouseButton;

/* Gamepad types */
typedef uint32_t ParsecGamepadButton;
typedef uint32_t ParsecGamepadAxis;

/* --- Message structs --- */

typedef struct ParsecKeyboardMessage {
    ParsecKeycode code;
    uint32_t mod;
    bool pressed;
    uint8_t __pad[3];
} ParsecKeyboardMessage;

typedef struct ParsecMouseButtonMessage {
    ParsecMouseButton button;
    bool pressed;
} ParsecMouseButtonMessage;

typedef struct ParsecMouseMotionMessage {
    int32_t x;
    int32_t y;
    bool relative;
} ParsecMouseMotionMessage;

typedef struct ParsecMouseWheelMessage {
    int32_t x;
    int32_t y;
} ParsecMouseWheelMessage;

typedef struct ParsecGamepadButtonMessage {
    uint32_t id;
    ParsecGamepadButton button;
    bool pressed;
} ParsecGamepadButtonMessage;

typedef struct ParsecGamepadAxisMessage {
    uint32_t id;
    ParsecGamepadAxis axis;
    int16_t value;
} ParsecGamepadAxisMessage;

typedef struct ParsecGamepadUnplugMessage {
    uint32_t id;
} ParsecGamepadUnplugMessage;

typedef struct ParsecMessage {
    ParsecMessageType type;
    union {
        ParsecKeyboardMessage keyboard;
        ParsecMouseButtonMessage mouseButton;
        ParsecMouseMotionMessage mouseMotion;
        ParsecMouseWheelMessage mouseWheel;
        ParsecGamepadButtonMessage gamepadButton;
        ParsecGamepadAxisMessage gamepadAxis;
        ParsecGamepadUnplugMessage gamepadUnplug;
    };
} ParsecMessage;

/* --- Client config --- */

typedef struct ParsecClientVideoConfig {
    uint8_t decoderIndex;
    int32_t resolutionX;
    int32_t resolutionY;
    bool decoderCompatibility;
    bool decoderH265;
} ParsecClientVideoConfig;

typedef struct ParsecClientConfig {
    ParsecClientVideoConfig video[2];
    int32_t mediaContainer;
    int32_t protocol;
    bool pngCursor;
} ParsecClientConfig;

/* --- Event structs --- */

typedef struct ParsecCursor {
    bool hidden;
    bool relative;
    bool imageUpdate;
    uint32_t size;
    uint16_t width;
    uint16_t height;
    uint16_t hotX;
    uint16_t hotY;
    int32_t positionX;
    int32_t positionY;
} ParsecCursor;

typedef struct ParsecClientCursorEvent {
    uint32_t key;
    ParsecCursor cursor;
} ParsecClientCursorEvent;

typedef struct ParsecClientUserDataEvent {
    uint32_t id;
    uint32_t key;
} ParsecClientUserDataEvent;

typedef struct ParsecClientEvent {
    ParsecClientEventType type;
    union {
        ParsecClientCursorEvent cursor;
        ParsecClientUserDataEvent userData;
    };
} ParsecClientEvent;

/* --- Status --- */

typedef struct ParsecDecoderInfo {
    uint32_t width;
    uint32_t height;
} ParsecDecoderInfo;

typedef struct ParsecClientStatus {
    ParsecDecoderInfo decoder[2];
} ParsecClientStatus;

/* --- Callbacks --- */

typedef void (*ParsecAudioFunc)(const int16_t *pcm, uint32_t frames, void *opaque);
typedef void (*ParsecLogCallback)(int32_t level, const char *msg, void *opaque);

/* --- SDK Functions --- */

ParsecStatus ParsecInit(uint32_t ver, void *cfg, const void *reserved, Parsec **parsec);
void ParsecDestroy(Parsec *parsec);

void ParsecSetLogCallback(ParsecLogCallback callback, void *opaque);

ParsecStatus ParsecClientConnect(Parsec *parsec, ParsecClientConfig *cfg,
                                  const char *sessionId, const char *peerId);
void ParsecClientDisconnect(Parsec *parsec);
ParsecStatus ParsecClientGetStatus(Parsec *parsec, ParsecClientStatus *status);

void ParsecClientSetDimensions(Parsec *parsec, uint8_t stream,
                                uint32_t width, uint32_t height, float scale);

ParsecStatus ParsecClientGLRenderFrame(Parsec *parsec, uint8_t stream,
                                        void *d3dDevice, void *d3dContext,
                                        uint32_t timeout);

void ParsecClientPollAudio(Parsec *parsec, ParsecAudioFunc audioFunc,
                            uint32_t timeout, void *opaque);

bool ParsecClientPollEvents(Parsec *parsec, uint32_t timeout,
                             ParsecClientEvent *event);

void ParsecClientSendMessage(Parsec *parsec, ParsecMessage *msg);
void ParsecClientSendUserData(Parsec *parsec, uint32_t type, const char *data);

void *ParsecGetBuffer(Parsec *parsec, uint32_t key);
void ParsecFree(void *ptr);

#ifdef __cplusplus
}
#endif

#endif /* PARSEC_H */
