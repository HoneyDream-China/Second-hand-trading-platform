package edu.campus.loop.common;

public record ApiResponse<T>(String code, String message, T data, String traceId) { }
