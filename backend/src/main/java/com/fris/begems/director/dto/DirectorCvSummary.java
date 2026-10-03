package com.fris.begems.director.dto;

import java.time.Instant;

public record DirectorCvSummary(String fileName, String contentType, long fileSize, Instant uploadedAt) {
}
