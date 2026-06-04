package com.pms.hotel.room;

import jakarta.validation.constraints.NotNull;

public record RoomStatusRequest(@NotNull RoomStatus status) {
}
