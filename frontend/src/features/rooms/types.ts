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

/** Corps envoyé pour créer ou modifier un type ; null désigne un champ laissé vide. */
export interface RoomTypeRequest {
  name: string;
  description: string | null;
  basePrice: number | null;
  capacity: number | null;
}

/** Corps envoyé pour créer ou modifier une chambre ; null désigne un champ laissé vide. */
export interface RoomRequest {
  roomNumber: string;
  floor: number | null;
  roomTypeId: string | null;
}
