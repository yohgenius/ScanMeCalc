package com.example.textrecognitiontest

sealed class TextResult {
    object InitialState: TextResult()
    class Success(val input: String, val result: String): TextResult()
    object NoResultFound: TextResult()
}
