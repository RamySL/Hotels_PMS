import { useState } from "react";
import { useSearchParams } from "react-router";
import { useIsAdmin } from "@/features/auth/useIsAdmin";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { SelectField } from "@/shared/components/SelectField";
import { useApiData } from "@/shared/hooks/useApiData";
import { ApiError } from "@/shared/lib/apiClient";
import { changeRoomStatus } from "./api";
import {
  isRoomStatus,
  ROOM_STATUS_LABEL,
  ROOM_STATUSES,
  STATUS_ACTIONS,
} from "./roomStatus";
import { RoomStatusBadge } from "./RoomStatusBadge";
import type { Room, RoomStatus } from "./types";

const FILTER_OPTIONS = [
  { value: "", label: "Tous les statuts" },
  ...ROOM_STATUSES.map((status) => ({
    value: status,
    label: ROOM_STATUS_LABEL[status],
  })),
];

/** Liste des chambres et changement de leur statut ; le filtre vit dans l'adresse (?statut=). */
export function RoomsPage() {
  const isAdmin = useIsAdmin();
  const [searchParams, setSearchParams] = useSearchParams();
  const statusParam = searchParams.get("statut");
  const filter = isRoomStatus(statusParam) ? statusParam : null;
  const rooms = useApiData<Room[]>(
    filter ? `/rooms?status=${filter}` : "/rooms",
  );
  const [pendingRoomId, setPendingRoomId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function changeStatus(room: Room, status: RoomStatus) {
    setPendingRoomId(room.id);
    setActionError(null);
    try {
      await changeRoomStatus(room.id, status);
      rooms.reload();
    } catch (e) {
      setActionError(
        e instanceof ApiError ? e.message : "Le statut n'a pas pu être changé.",
      );
    } finally {
      setPendingRoomId(null);
    }
  }

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Chambres</h1>
        <div className="flex gap-3">
          <ButtonLink to="/chambres/types" variant="secondary">
            Types et tarifs
          </ButtonLink>
          {isAdmin && (
            <ButtonLink to="/chambres/nouvelle">Nouvelle chambre</ButtonLink>
          )}
        </div>
      </div>

      <SelectField
        label="Statut"
        options={FILTER_OPTIONS}
        value={filter ?? ""}
        onChange={(event) =>
          setSearchParams(
            event.target.value ? { statut: event.target.value } : {},
          )
        }
        className="mt-6 max-w-xs"
      />

      {actionError && (
        <Alert tone="error" className="mt-4">
          {actionError}
        </Alert>
      )}
      {rooms.error && (
        <Alert tone="error" className="mt-4">
          {rooms.error.message}
        </Alert>
      )}

      {rooms.data === undefined ? (
        rooms.loading && <p className="mt-6 text-slate-500">Chargement…</p>
      ) : rooms.data.length === 0 ? (
        <p className="mt-6 text-slate-500">
          {filter ? "Aucune chambre dans ce statut." : "Aucune chambre."}
        </p>
      ) : (
        <table className="mt-6 w-full border-collapse overflow-hidden rounded-lg bg-white text-left shadow-sm">
          <thead className="border-b border-slate-200 text-sm text-slate-600">
            <tr>
              <th className="px-4 py-3 font-medium">Chambre</th>
              <th className="px-4 py-3 font-medium">Étage</th>
              <th className="px-4 py-3 font-medium">Type</th>
              <th className="px-4 py-3 font-medium">Statut</th>
              <th className="px-4 py-3 font-medium">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {rooms.data.map((room) => (
              <tr
                key={room.id}
                className="border-b border-slate-100 last:border-0"
              >
                <td className="px-4 py-3 font-medium text-slate-900">
                  {room.roomNumber}
                </td>
                <td className="px-4 py-3 text-slate-700">{room.floor}</td>
                <td className="px-4 py-3 text-slate-700">
                  {room.roomType.name}
                </td>
                <td className="px-4 py-3">
                  <RoomStatusBadge status={room.status} />
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    {STATUS_ACTIONS[room.status].length === 0 ? (
                      <span className="text-sm text-slate-500">
                        Libérée au check-out
                      </span>
                    ) : (
                      STATUS_ACTIONS[room.status].map((action) => (
                        <Button
                          key={action.status}
                          variant="secondary"
                          size="sm"
                          disabled={pendingRoomId === room.id}
                          onClick={() => changeStatus(room, action.status)}
                          aria-label={`${action.label}, chambre ${room.roomNumber}`}
                        >
                          {action.label}
                        </Button>
                      ))
                    )}
                    {isAdmin && (
                      <ButtonLink
                        to={`/chambres/${room.id}/modifier`}
                        variant="secondary"
                        size="sm"
                        aria-label={`Modifier la chambre ${room.roomNumber}`}
                      >
                        Modifier
                      </ButtonLink>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
