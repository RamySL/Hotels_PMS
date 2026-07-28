export type RoomStatus = "READY" | "OCCUPIED" | "CLEANING" | "OUT_OF_SERVICE";

export interface RoomType {
  id: string;
  name: string;
  description: string | null;
  basePrice: number;
  capacity: number;
}

export interface Room {
  id: string;
  roomNumber: string;
  floor: number;
  status: RoomStatus;
  roomType: RoomType;
}
