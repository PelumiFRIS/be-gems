package com.fris.begems.approval.dto;

import jakarta.validation.constraints.Size;

public record ApprovalRequest(@Size(max = 2000) String comment) {
}
