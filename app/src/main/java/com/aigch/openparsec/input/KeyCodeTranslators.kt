package com.aigch.openparsec.input

import android.view.KeyEvent

/**
 * Key code translator for Android to Parsec keycodes.
 * Ported from iOS KeyCodeTranslators.swift
 *
 * Maps Android KeyEvent keycodes to Parsec USB HID keycodes.
 */
object KeyCodeTranslators {
    /**
     * Convert Android KeyEvent keycode to Parsec keycode (USB HID).
     */
    fun androidKeyCodeToParsec(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> 4
            KeyEvent.KEYCODE_B -> 5
            KeyEvent.KEYCODE_C -> 6
            KeyEvent.KEYCODE_D -> 7
            KeyEvent.KEYCODE_E -> 8
            KeyEvent.KEYCODE_F -> 9
            KeyEvent.KEYCODE_G -> 10
            KeyEvent.KEYCODE_H -> 11
            KeyEvent.KEYCODE_I -> 12
            KeyEvent.KEYCODE_J -> 13
            KeyEvent.KEYCODE_K -> 14
            KeyEvent.KEYCODE_L -> 15
            KeyEvent.KEYCODE_M -> 16
            KeyEvent.KEYCODE_N -> 17
            KeyEvent.KEYCODE_O -> 18
            KeyEvent.KEYCODE_P -> 19
            KeyEvent.KEYCODE_Q -> 20
            KeyEvent.KEYCODE_R -> 21
            KeyEvent.KEYCODE_S -> 22
            KeyEvent.KEYCODE_T -> 23
            KeyEvent.KEYCODE_U -> 24
            KeyEvent.KEYCODE_V -> 25
            KeyEvent.KEYCODE_W -> 26
            KeyEvent.KEYCODE_X -> 27
            KeyEvent.KEYCODE_Y -> 28
            KeyEvent.KEYCODE_Z -> 29
            KeyEvent.KEYCODE_1 -> 30
            KeyEvent.KEYCODE_2 -> 31
            KeyEvent.KEYCODE_3 -> 32
            KeyEvent.KEYCODE_4 -> 33
            KeyEvent.KEYCODE_5 -> 34
            KeyEvent.KEYCODE_6 -> 35
            KeyEvent.KEYCODE_7 -> 36
            KeyEvent.KEYCODE_8 -> 37
            KeyEvent.KEYCODE_9 -> 38
            KeyEvent.KEYCODE_0 -> 39
            KeyEvent.KEYCODE_ENTER -> 40
            KeyEvent.KEYCODE_ESCAPE -> 41
            KeyEvent.KEYCODE_DEL -> 42 // Backspace
            KeyEvent.KEYCODE_TAB -> 43
            KeyEvent.KEYCODE_SPACE -> 44
            KeyEvent.KEYCODE_MINUS -> 45
            KeyEvent.KEYCODE_EQUALS -> 46
            KeyEvent.KEYCODE_LEFT_BRACKET -> 47
            KeyEvent.KEYCODE_RIGHT_BRACKET -> 48
            KeyEvent.KEYCODE_BACKSLASH -> 49
            KeyEvent.KEYCODE_SEMICOLON -> 51
            KeyEvent.KEYCODE_APOSTROPHE -> 52
            KeyEvent.KEYCODE_GRAVE -> 53
            KeyEvent.KEYCODE_COMMA -> 54
            KeyEvent.KEYCODE_PERIOD -> 55
            KeyEvent.KEYCODE_SLASH -> 56
            KeyEvent.KEYCODE_CAPS_LOCK -> 57
            KeyEvent.KEYCODE_F1 -> 58
            KeyEvent.KEYCODE_F2 -> 59
            KeyEvent.KEYCODE_F3 -> 60
            KeyEvent.KEYCODE_F4 -> 61
            KeyEvent.KEYCODE_F5 -> 62
            KeyEvent.KEYCODE_F6 -> 63
            KeyEvent.KEYCODE_F7 -> 64
            KeyEvent.KEYCODE_F8 -> 65
            KeyEvent.KEYCODE_F9 -> 66
            KeyEvent.KEYCODE_F10 -> 67
            KeyEvent.KEYCODE_F11 -> 68
            KeyEvent.KEYCODE_F12 -> 69
            KeyEvent.KEYCODE_SYSRQ -> 70 // Print Screen
            KeyEvent.KEYCODE_SCROLL_LOCK -> 71
            KeyEvent.KEYCODE_BREAK -> 72 // Pause
            KeyEvent.KEYCODE_INSERT -> 73
            KeyEvent.KEYCODE_MOVE_HOME -> 74
            KeyEvent.KEYCODE_PAGE_UP -> 75
            KeyEvent.KEYCODE_FORWARD_DEL -> 76
            KeyEvent.KEYCODE_MOVE_END -> 77
            KeyEvent.KEYCODE_PAGE_DOWN -> 78
            KeyEvent.KEYCODE_DPAD_RIGHT -> 79
            KeyEvent.KEYCODE_DPAD_LEFT -> 80
            KeyEvent.KEYCODE_DPAD_DOWN -> 81
            KeyEvent.KEYCODE_DPAD_UP -> 82
            KeyEvent.KEYCODE_NUM_LOCK -> 83
            KeyEvent.KEYCODE_NUMPAD_DIVIDE -> 84
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> 85
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> 86
            KeyEvent.KEYCODE_NUMPAD_ADD -> 87
            KeyEvent.KEYCODE_NUMPAD_ENTER -> 88
            KeyEvent.KEYCODE_NUMPAD_1 -> 89
            KeyEvent.KEYCODE_NUMPAD_2 -> 90
            KeyEvent.KEYCODE_NUMPAD_3 -> 91
            KeyEvent.KEYCODE_NUMPAD_4 -> 92
            KeyEvent.KEYCODE_NUMPAD_5 -> 93
            KeyEvent.KEYCODE_NUMPAD_6 -> 94
            KeyEvent.KEYCODE_NUMPAD_7 -> 95
            KeyEvent.KEYCODE_NUMPAD_8 -> 96
            KeyEvent.KEYCODE_NUMPAD_9 -> 97
            KeyEvent.KEYCODE_NUMPAD_0 -> 98
            KeyEvent.KEYCODE_NUMPAD_DOT -> 99
            KeyEvent.KEYCODE_MENU -> 118
            KeyEvent.KEYCODE_VOLUME_MUTE -> 127
            KeyEvent.KEYCODE_VOLUME_UP -> 128
            KeyEvent.KEYCODE_VOLUME_DOWN -> 129
            KeyEvent.KEYCODE_CTRL_LEFT -> 224
            KeyEvent.KEYCODE_SHIFT_LEFT -> 225
            KeyEvent.KEYCODE_ALT_LEFT -> 226
            KeyEvent.KEYCODE_META_LEFT -> 227
            KeyEvent.KEYCODE_CTRL_RIGHT -> 228
            KeyEvent.KEYCODE_SHIFT_RIGHT -> 229
            KeyEvent.KEYCODE_ALT_RIGHT -> 230
            KeyEvent.KEYCODE_META_RIGHT -> 231
            KeyEvent.KEYCODE_MEDIA_NEXT -> 258
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> 259
            KeyEvent.KEYCODE_MEDIA_STOP -> 260
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> 261
            else -> 0
        }
    }

    /**
     * Convert a character string to Parsec keycode with shift modifier info.
     * Ported from iOS getParsecKeycode(for:).
     */
    fun getParsecKeycode(key: String): Pair<Int, Boolean> {
        var keyMod = false
        val parsecKeycode: Int = when (key) {
            "-" -> 45
            "=" -> 46
            "[" -> 47
            "]" -> 48
            "\\" -> 49
            ";" -> 51
            "'" -> 52
            "`" -> 53
            "," -> 54
            "." -> 55
            "/" -> 56
            "_" -> { keyMod = true; 45 }
            "+" -> { keyMod = true; 46 }
            "{" -> { keyMod = true; 47 }
            "}" -> { keyMod = true; 48 }
            "|" -> { keyMod = true; 49 }
            ":" -> { keyMod = true; 51 }
            "\"" -> { keyMod = true; 52 }
            "~" -> { keyMod = true; 53 }
            "<" -> { keyMod = true; 54 }
            ">" -> { keyMod = true; 55 }
            "?" -> { keyMod = true; 56 }
            "!" -> { keyMod = true; 30 }
            "@" -> { keyMod = true; 31 }
            "#" -> { keyMod = true; 32 }
            "$" -> { keyMod = true; 33 }
            "%" -> { keyMod = true; 34 }
            "^" -> { keyMod = true; 35 }
            "&" -> { keyMod = true; 36 }
            "*" -> { keyMod = true; 37 }
            "(" -> { keyMod = true; 38 }
            ")" -> { keyMod = true; 39 }
            else -> -1
        }
        return Pair(parsecKeycode, keyMod)
    }

    /**
     * Convert a text character to Parsec keycode.
     * Ported from iOS parsecKeyCodeTranslator.
     */
    fun parsecKeyCodeTranslator(str: String): Int? {
        return when (str) {
            "A" -> 4; "B" -> 5; "C" -> 6; "D" -> 7; "E" -> 8
            "F" -> 9; "G" -> 10; "H" -> 11; "I" -> 12; "J" -> 13
            "K" -> 14; "L" -> 15; "M" -> 16; "N" -> 17; "O" -> 18
            "P" -> 19; "Q" -> 20; "R" -> 21; "S" -> 22; "T" -> 23
            "U" -> 24; "V" -> 25; "W" -> 26; "X" -> 27; "Y" -> 28
            "Z" -> 29
            "1" -> 30; "2" -> 31; "3" -> 32; "4" -> 33; "5" -> 34
            "6" -> 35; "7" -> 36; "8" -> 37; "9" -> 38; "0" -> 39
            "ENTER" -> 40; "ESCAPE" -> 41; "BACKSPACE" -> 42
            "TAB" -> 43; "SPACE" -> 44
            "MINUS" -> 45; "EQUALS" -> 46
            "LBRACKET" -> 47; "RBRACKET" -> 48
            "BACKSLASH" -> 49; "SEMICOLON" -> 51
            "APOSTROPHE" -> 52; "BACKTICK" -> 53
            "COMMA" -> 54; "PERIOD" -> 55; "SLASH" -> 56
            "CAPSLOCK" -> 57
            "F1" -> 58; "F2" -> 59; "F3" -> 60; "F4" -> 61
            "F5" -> 62; "F6" -> 63; "F7" -> 64; "F8" -> 65
            "F9" -> 66; "F10" -> 67; "F11" -> 68; "F12" -> 69
            "PRINTSCREEN" -> 70; "SCROLLLOCK" -> 71; "PAUSE" -> 72
            "INSERT" -> 73; "HOME" -> 74; "PAGEUP" -> 75
            "DELETE" -> 76; "END" -> 77; "PAGEDOWN" -> 78
            "RIGHT" -> 79; "LEFT" -> 80; "DOWN" -> 81; "UP" -> 82
            "NUMLOCK" -> 83
            "CONTROL" -> 224; "SHIFT" -> 225
            "LALT" -> 226; "LGUI" -> 227
            "RCTRL" -> 228; "RSHIFT" -> 229
            "RALT" -> 230; "RGUI" -> 231
            else -> null
        }
    }
}
