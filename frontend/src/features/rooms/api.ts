import { api } from "@/shared/lib/apiClient";
import type { Room, RoomStatus } from "./types";

export function changeRoomStatus(
  roomId: string,
  status: RoomStatus,
): Promise<Room> {
  return api.patch<Room>(`/rooms/${roomId}/status`, { status });
}
