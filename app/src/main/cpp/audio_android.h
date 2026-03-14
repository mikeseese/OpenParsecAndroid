/**
 * Android audio playback using OpenSL ES.
 * Ported from iOS audio.c (AudioToolbox implementation).
 *
 * Provides PCM audio playback at 48kHz stereo 16-bit,
 * matching the Parsec SDK audio callback format.
 */

#ifndef AUDIO_ANDROID_H
#define AUDIO_ANDROID_H

#include <stdint.h>
#include <stdbool.h>

struct audio;

void audio_init(struct audio **ctx_out);
void audio_destroy(struct audio **ctx_out);
void audio_clear(struct audio **ctx_out);
void audio_cb(const int16_t *pcm, uint32_t frames, void *opaque);
void audio_mute(bool muted, const void *opaque);

#endif /* AUDIO_ANDROID_H */
