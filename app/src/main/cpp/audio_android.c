/**
 * Android audio playback using OpenSL ES.
 * Ported from iOS audio.c (AudioToolbox AudioQueue implementation).
 *
 * Uses OpenSL ES buffer queue for low-latency PCM playback.
 * Format: 48kHz, 2-channel stereo, 16-bit signed integer PCM.
 *
 * Architecture matches iOS implementation:
 * - Circular buffer pool (NUM_AUDIO_BUF buffers)
 * - Silence padding to prevent underruns
 * - Mute support with queue pause/clear
 */

#include "audio_android.h"

#include <stdlib.h>
#include <string.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/log.h>

#define TAG "AudioAndroid"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define NUM_AUDIO_BUF  16
#define BUFFER_SIZE    4096  /* bytes per buffer */
#define SAMPLE_RATE    48000
#define NUM_CHANNELS   2
#define BYTES_PER_FRAME (NUM_CHANNELS * sizeof(int16_t))  /* 4 bytes */
#define LOWEST_NUM_BUFFER 3

struct audio {
    /* OpenSL ES objects */
    SLObjectItf engineObj;
    SLEngineItf engine;
    SLObjectItf outputMixObj;
    SLObjectItf playerObj;
    SLPlayItf player;
    SLAndroidSimpleBufferQueueItf bufferQueue;

    /* Buffer pool */
    uint8_t buffers[NUM_AUDIO_BUF][BUFFER_SIZE];
    int32_t bufferSize[NUM_AUDIO_BUF]; /* 0 = idle, >0 = data size */
    int32_t writeIdx;  /* next buffer to write into */
    int32_t readIdx;   /* next buffer to enqueue for playback */

    /* State */
    bool started;
    bool muted;
    int32_t inUse;     /* approximate bytes in flight */
    int32_t failNum;   /* consecutive write failures */

    /* Silence buffer for underrun prevention */
    uint8_t silenceBuf[BUFFER_SIZE];
};

/* Forward declaration */
static void buffer_queue_callback(SLAndroidSimpleBufferQueueItf bq, void *context);

void audio_init(struct audio **ctx_out)
{
    struct audio *ctx = calloc(1, sizeof(struct audio));
    if (!ctx) {
        LOGE("Failed to allocate audio context");
        *ctx_out = NULL;
        return;
    }

    SLresult result;

    /* Create engine */
    result = slCreateEngine(&ctx->engineObj, 0, NULL, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create OpenSL engine: %d", (int)result);
        free(ctx);
        *ctx_out = NULL;
        return;
    }
    (*ctx->engineObj)->Realize(ctx->engineObj, SL_BOOLEAN_FALSE);
    (*ctx->engineObj)->GetInterface(ctx->engineObj, SL_IID_ENGINE, &ctx->engine);

    /* Create output mix */
    result = (*ctx->engine)->CreateOutputMix(ctx->engine, &ctx->outputMixObj, 0, NULL, NULL);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create output mix: %d", (int)result);
        (*ctx->engineObj)->Destroy(ctx->engineObj);
        free(ctx);
        *ctx_out = NULL;
        return;
    }
    (*ctx->outputMixObj)->Realize(ctx->outputMixObj, SL_BOOLEAN_FALSE);

    /* Configure audio source (buffer queue) */
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
        SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
        NUM_AUDIO_BUF
    };
    SLDataFormat_PCM format_pcm = {
        SL_DATAFORMAT_PCM,
        NUM_CHANNELS,
        SL_SAMPLINGRATE_48,
        SL_PCMSAMPLEFORMAT_FIXED_16,
        SL_PCMSAMPLEFORMAT_FIXED_16,
        SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
        SL_BYTEORDER_LITTLEENDIAN
    };
    SLDataSource audioSrc = { &loc_bufq, &format_pcm };

    /* Configure audio sink (output mix) */
    SLDataLocator_OutputMix loc_outmix = {
        SL_DATALOCATOR_OUTPUTMIX,
        ctx->outputMixObj
    };
    SLDataSink audioSnk = { &loc_outmix, NULL };

    /* Create audio player */
    const SLInterfaceID ids[] = { SL_IID_BUFFERQUEUE };
    const SLboolean req[] = { SL_BOOLEAN_TRUE };
    result = (*ctx->engine)->CreateAudioPlayer(ctx->engine, &ctx->playerObj,
                                                &audioSrc, &audioSnk,
                                                1, ids, req);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create audio player: %d", (int)result);
        (*ctx->outputMixObj)->Destroy(ctx->outputMixObj);
        (*ctx->engineObj)->Destroy(ctx->engineObj);
        free(ctx);
        *ctx_out = NULL;
        return;
    }
    (*ctx->playerObj)->Realize(ctx->playerObj, SL_BOOLEAN_FALSE);

    /* Get play and buffer queue interfaces */
    (*ctx->playerObj)->GetInterface(ctx->playerObj, SL_IID_PLAY, &ctx->player);
    (*ctx->playerObj)->GetInterface(ctx->playerObj, SL_IID_BUFFERQUEUE, &ctx->bufferQueue);

    /* Register buffer queue callback */
    (*ctx->bufferQueue)->RegisterCallback(ctx->bufferQueue, buffer_queue_callback, ctx);

    /* Initialize state */
    ctx->writeIdx = 0;
    ctx->readIdx = 0;
    ctx->started = false;
    ctx->muted = false;
    ctx->inUse = 0;
    ctx->failNum = 0;
    memset(ctx->silenceBuf, 0, BUFFER_SIZE);

    for (int i = 0; i < NUM_AUDIO_BUF; i++) {
        ctx->bufferSize[i] = 0;
    }

    LOGD("Audio initialized: %dHz %dch 16-bit", SAMPLE_RATE, NUM_CHANNELS);
    *ctx_out = ctx;
}

void audio_destroy(struct audio **ctx_out)
{
    if (!ctx_out || !*ctx_out)
        return;

    struct audio *ctx = *ctx_out;

    if (ctx->playerObj) {
        (*ctx->player)->SetPlayState(ctx->player, SL_PLAYSTATE_STOPPED);
        (*ctx->playerObj)->Destroy(ctx->playerObj);
    }
    if (ctx->outputMixObj) {
        (*ctx->outputMixObj)->Destroy(ctx->outputMixObj);
    }
    if (ctx->engineObj) {
        (*ctx->engineObj)->Destroy(ctx->engineObj);
    }

    free(ctx);
    *ctx_out = NULL;
    LOGD("Audio destroyed");
}

void audio_clear(struct audio **ctx_out)
{
    if (!ctx_out || !*ctx_out)
        return;

    struct audio *ctx = *ctx_out;

    if (ctx->player) {
        (*ctx->player)->SetPlayState(ctx->player, SL_PLAYSTATE_STOPPED);
    }
    if (ctx->bufferQueue) {
        (*ctx->bufferQueue)->Clear(ctx->bufferQueue);
    }

    for (int i = 0; i < NUM_AUDIO_BUF; i++) {
        ctx->bufferSize[i] = 0;
    }
    ctx->writeIdx = 0;
    ctx->readIdx = 0;
    ctx->started = false;
    ctx->inUse = 0;
    ctx->failNum = 0;
}

/**
 * Buffer queue callback - called when a buffer finishes playing.
 * Enqueues the next available buffer or silence if none available.
 */
static void buffer_queue_callback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    struct audio *ctx = (struct audio *)context;
    if (!ctx || ctx->muted)
        return;

    /* Mark the played buffer as idle */
    /* Note: readIdx tracks what was last enqueued, not what's playing.
       The callback means a buffer finished, so we advance our tracking. */

    /* Check if there's a filled buffer to enqueue */
    int nextRead = (ctx->readIdx) % NUM_AUDIO_BUF;
    if (ctx->bufferSize[nextRead] > 0) {
        (*bq)->Enqueue(bq, ctx->buffers[nextRead], ctx->bufferSize[nextRead]);
        ctx->inUse -= ctx->bufferSize[nextRead];
        ctx->bufferSize[nextRead] = 0;
        ctx->readIdx = (nextRead + 1) % NUM_AUDIO_BUF;
    } else {
        /* Underrun: enqueue silence to keep the player alive */
        (*bq)->Enqueue(bq, ctx->silenceBuf, BUFFER_SIZE);
    }
}

/**
 * Audio data callback from Parsec SDK.
 * Called by ParsecClientPollAudio with decoded PCM audio frames.
 */
void audio_cb(const int16_t *pcm, uint32_t frames, void *opaque)
{
    if (frames == 0 || opaque == NULL)
        return;

    struct audio *ctx = (struct audio *)opaque;
    if (ctx->muted)
        return;

    uint32_t dataSize = frames * BYTES_PER_FRAME;
    if (dataSize > BUFFER_SIZE)
        dataSize = BUFFER_SIZE;

    /* Find an idle buffer to write into */
    int idx = ctx->writeIdx;
    if (ctx->bufferSize[idx] != 0) {
        /* All buffers full - drop this audio chunk */
        ctx->failNum++;
        if (ctx->failNum > 10) {
            audio_clear(&ctx);
        }
        return;
    }

    memcpy(ctx->buffers[idx], pcm, dataSize);
    ctx->bufferSize[idx] = dataSize;
    ctx->writeIdx = (idx + 1) % NUM_AUDIO_BUF;
    ctx->failNum = 0;
    ctx->inUse += dataSize;

    if (!ctx->started) {
        /* Enqueue initial buffer and start playback */
        (*ctx->bufferQueue)->Enqueue(ctx->bufferQueue,
                                      ctx->buffers[idx], dataSize);
        ctx->bufferSize[idx] = 0;
        ctx->readIdx = (idx + 1) % NUM_AUDIO_BUF;

        /* Wait for enough data before starting (matches iOS: inUse > 1000 frames) */
        if (ctx->inUse > (int32_t)(1000 * BYTES_PER_FRAME)) {
            (*ctx->player)->SetPlayState(ctx->player, SL_PLAYSTATE_PLAYING);
            ctx->started = true;
            LOGD("Audio playback started");
        }
    }
}

void audio_mute(bool muted, const void *opaque)
{
    if (!opaque)
        return;

    struct audio *ctx = (struct audio *)opaque;
    if (ctx->muted == muted)
        return;

    ctx->muted = muted;

    if (muted) {
        if (ctx->player) {
            (*ctx->player)->SetPlayState(ctx->player, SL_PLAYSTATE_PAUSED);
        }
        audio_clear(&ctx);
        LOGD("Audio muted");
    }
}
