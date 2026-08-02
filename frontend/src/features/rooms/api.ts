import { api } from "@/shared/lib/apiClient";
import type {
  Room,
  RoomRequest,
  RoomStatus,
  RoomType,
  RoomTypeRequest,
} from "./types";

export function changeRoomStatus(
  roomId: string,
  status: RoomStatus,
): Promise<Room> {
  return api.patch<Room>(`/rooms/${roomId}/status`, { status });
}

export function createRoom(request: RoomRequest): Promise<Room> {
  return api.post<Room>("/rooms", request);
}

export function updateRoom(id: string, request: RoomRequest): Promise<Room> {
  return api.put<Room>(`/rooms/${id}`, request);
}

export function createRoomType(request: RoomTypeRequest): Promise<RoomType> {
  return api.post<RoomType>("/room-types", request);
}

export function updateRoomType(
  id: string,
  request: RoomTypeRequest,
): Promise<RoomType> {
  return api.put<RoomType>(`/room-types/${id}`, request);
}
