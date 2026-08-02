import type { Room, RoomStatus, RoomType } from "@/features/rooms/types";

export const DOUBLE: RoomType = {
  id: "type-double",
  name: "Double",
  description: null,
  basePrice: 89.9,
  capacity: 2,
};

export const SUITE: RoomType = {
  id: "type-suite",
  name: "Suite",
  description: "Vue sur la mer",
  basePrice: 189,
  capacity: 4,
};

export function room(
  roomNumber: string,
  status: RoomStatus = "READY",
  roomType: RoomType = DOUBLE,
): Room {
  return { id: `room-${roomNumber}`, roomNumber, floor: 1, status, roomType };
}
